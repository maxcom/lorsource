/*
 * Copyright 1998-2023 Linux.org.ru
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 */
package ru.org.linux.topic

import com.typesafe.scalalogging.StrictLogging
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.*
import org.springframework.web.context.request.WebRequest
import org.springframework.web.servlet.ModelAndView
import org.springframework.web.servlet.view.RedirectView
import ru.org.linux.auth.AuthUtil.AuthorizedOpt
import ru.org.linux.auth.IPBlockDao
import ru.org.linux.comment.*
import ru.org.linux.edithistory.EditHistoryObjectTypeEnum.TOPIC
import ru.org.linux.edithistory.EditHistoryService
import ru.org.linux.group.{Group, GroupDao}
import ru.org.linux.markup.MessageTextService
import ru.org.linux.paginator.PagesInfo
import ru.org.linux.search.{MoreLikeThisService, MoreLikeThisTopic}
import ru.org.linux.section.{Section, SectionScrollModeEnum, SectionService}
import ru.org.linux.site.{MessageNotFoundException, Template}
import ru.org.linux.spring.dao.MsgbaseDao
import ru.org.linux.user.{IgnoreListDao, MemoriesDao, User}

import java.time.Instant
import java.util
import java.util.concurrent.{Callable, TimeUnit}
import javax.servlet.http.{HttpServletRequest, HttpServletResponse}
import scala.collection.mutable
import scala.concurrent.duration.Duration
import scala.jdk.CollectionConverters.{MapHasAsJava, SeqHasAsJava}

object TopicController {
  private val MoreLikeThisTimeout = Duration.apply(500, TimeUnit.MILLISECONDS)
  private val logger = LoggerFactory.getLogger(classOf[TopicController])

  private val JUMP_MIN_DURATION: org.joda.time.Duration = org.joda.time.Duration.standardDays(30)

  private def getDefaultFilter(emptyIgnoreList: Boolean): Int = {
    var filterMode = CommentFilter.FILTER_IGNORED

    if (emptyIgnoreList) {
      filterMode &= ~CommentFilter.FILTER_IGNORED
    }

    filterMode
  }

  private def buildPages(topic: Topic, messagesPerPage: Int, filterModeShow: Boolean, currentPage: Int): PagesInfo = {
    var base = TopicLinkBuilder.baseLink(topic).lastmod(messagesPerPage)

    if (filterModeShow) {
      base = base.filterShow()
    }

    val out = for (i <- 0 until topic.getPageCount(messagesPerPage)) yield {
      base.page(i).build
    }

    new PagesInfo(out.asJava, currentPage)
  }

  private def checkLastModified(webRequest: WebRequest, message: Topic): Boolean = {
    try {
      webRequest.checkNotModified(message.lastModified.getTime)
    } catch {
      case _: IllegalArgumentException =>
        false
    }
  }

  private def getEtag(message: Topic): String = "msg-" + message.id + '-' + message.lastModified.getTime
}

@Controller
class TopicController(sectionService: SectionService, topicDao: TopicDao, prepareService: CommentPrepareService,
                      topicPrepareService: TopicPrepareService, commentService: CommentReadService,
                      ignoreListDao: IgnoreListDao, ipBlockDao: IPBlockDao, editHistoryService: EditHistoryService,
                      memoriesDao: MemoriesDao, permissionService: TopicPermissionService,
                      moreLikeThisService: MoreLikeThisService, topicTagService: TopicTagService,
                      msgbaseDao: MsgbaseDao, textService: MessageTextService, groupDao: GroupDao) extends StrictLogging {
  @RequestMapping(value = Array("/{section:(?:forum)|(?:news)|(?:polls)|(?:articles)|(?:gallery)}/{group}/{id}"))
  def getMessageNewMain(webRequest: WebRequest, request: HttpServletRequest, response: HttpServletResponse,
                        @RequestParam(value = "filter", required = false) filter: String,
                        @RequestParam(value = "cid", required = false) cid: Integer,
                        @RequestParam(value = "deleted", required = false) deleted: String,
                        @RequestParam(value = "skipdeleted", required = false, defaultValue = "false") skipDeleted: Boolean,
                        @PathVariable("section") sectionName: String, @PathVariable("group") groupName: String,
                        @PathVariable("id") msgid: Int): ModelAndView = {
    if (cid != null) {
      jumpMessage(msgid, cid, skipDeleted)
    } else {
      val section = sectionService.getSectionByName(sectionName)

      val showDeleted = deleted!=null

      if (showDeleted) {
        getMessage(section, webRequest, request, response, -1, null, groupName, msgid, 0, showDeleted = true)
      } else {
        getMessage(section, webRequest, request, response, 0, filter, groupName, msgid, 0, showDeleted = false)
      }
    }
  }

  @RequestMapping(value = Array("/{section:(?:forum)|(?:news)|(?:polls)|(?:articles)|(?:gallery)}/{group}/{id}/page{page}"),
    method = Array(RequestMethod.GET))
  def getMessageNewPage(webRequest: WebRequest, request: HttpServletRequest, response: HttpServletResponse,
                        @RequestParam(value = "filter", required = false) filter: String,
                        @PathVariable("section") sectionName: String, @PathVariable("group") groupName: String,
                        @PathVariable("id") msgid: Int, @PathVariable("page") page: Int): ModelAndView = {
    val section = sectionService.getSectionByName(sectionName)

    if (page == -1) {
      val topic = topicDao.getById(msgid)
      new ModelAndView(new RedirectView(topic.getLink))
    } else {
      getMessage(section, webRequest, request, response, page, filter, groupName, msgid, 0, showDeleted = false)
    }
  }

  @RequestMapping(value = Array("/{section:(?:forum)|(?:news)|(?:polls)|(?:articles)|(?:gallery)}/{group}/{id}/thread/{threadRoot}"),
    method = Array(RequestMethod.GET))
  def getMessageThread(webRequest: WebRequest, request: HttpServletRequest, response: HttpServletResponse,
                       @RequestParam(value = "filter", required = false) filter: String,
                       @PathVariable("section") sectionName: String, @PathVariable("group") groupName: String,
                       @PathVariable("id") msgid: Int, @PathVariable("threadRoot") threadRoot: Int): ModelAndView = {
    val section = sectionService.getSectionByName(sectionName)

    getMessage(section, webRequest, request, response, 0, filter, groupName, msgid, threadRoot, showDeleted = false)
  }

  private def getMessage(section: Section, webRequest: WebRequest, request: HttpServletRequest,
                         response: HttpServletResponse, page: Int, filter: String, groupName: String, msgid: Int,
                         threadRoot: Int, showDeleted: Boolean): ModelAndView = AuthorizedOpt { currentUserOpt =>
    val deadline = TopicController.MoreLikeThisTimeout.fromNow

    val topic = topicDao.getById(msgid)

    if (!currentUserOpt.exists(_.moderator) && showDeleted && !("POST" == request.getMethod)) {
      return new ModelAndView(new RedirectView(topic.getLink))
    }

    val tags = topicTagService.getTagRefs(topic)
    val moreLikeThis = moreLikeThisService.searchSimilar(topic, tags)
    val messageText = msgbaseDao.getMessageText(topic.id)
    val plainText = textService.extractPlainText(messageText)

    val preparedMessage = topicPrepareService.prepareTopic(topic, tags, currentUserOpt.map(_.user), messageText)

    val group = preparedMessage.group

    if (!(group.urlName == groupName) || group.sectionId != section.getId) {
      return new ModelAndView(new RedirectView(topic.getLink))
    }

    val params = new mutable.HashMap[String, AnyRef]()

    if (currentUserOpt.isDefined) {
      val editInfoSummary = editHistoryService.editInfoSummary(topic.id, TOPIC)

      if (editInfoSummary.nonEmpty) {
        params.put("editInfo", topicPrepareService.prepareEditInfo(editInfoSummary.get))
      }
    }

    permissionService.checkView(group, topic, currentUserOpt.map(_.user).orNull, preparedMessage.author, showDeleted)

    val tmpl = Template.getTemplate

    val pages = topic.getPageCount(tmpl.getProf.getMessages)

    if (page >= pages && page > 0) {
      if (pages == 0) {
        return new ModelAndView(new RedirectView(topic.getLink))
      } else {
        return new ModelAndView(new RedirectView(topic.getLinkPage(pages - 1)))
      }
    }

    if (showDeleted || topic.deleted) {
      logger.info(s"View deleted ${topic.getLink} by " +
        s"${currentUserOpt.map(_.user.getNick).getOrElse("<none>")} (deleted = ${topic.deleted})")
    }

    params.put("showDeleted", Boolean.box(showDeleted))
    params.put("message", topic)
    params.put("preparedMessage", preparedMessage)

    if (topic.expired) {
      response.setDateHeader("Expires", System.currentTimeMillis + 30 * 24 * 60 * 60 * 1000L)
    }

    params.put("ogDescription", MessageTextService.trimPlainText(plainText, 250, encodeHtml = true))
    params.put("page", Integer.valueOf(page))
    params.put("group", group)
    params.put("showAdsense", Boolean.box(currentUserOpt.isEmpty || !tmpl.getProf.isHideAdsense))

    if (currentUserOpt.isEmpty && topic.expired) {
      val etag = TopicController.getEtag(topic)

      response.setHeader("Etag", etag)

      if (request.getHeader("If-None-Match") != null) if (etag == request.getHeader("If-None-Match")) {
        response.setStatus(HttpServletResponse.SC_NOT_MODIFIED)
        return null
      } else if (TopicController.checkLastModified(webRequest, topic)) {
        return null
      }
    }

    params.put("messageMenu", topicPrepareService.getTopicMenu(preparedMessage, currentUserOpt.map(_.user).orNull, tmpl.getProf, loadUserpics = true))
    params.put("memoriesInfo", memoriesDao.getTopicInfo(topic.id, currentUserOpt.map(_.user)))

    val ignoreList: Set[Int] = currentUserOpt.map { currentUser =>
      ignoreListDao.get(currentUser.user.getId)
    }.getOrElse(Set.empty)

    val (filterMode, filterModeShow) = if (filter=="show") {
      (CommentFilter.FILTER_NONE, true)
    } else{
      (TopicController.getDefaultFilter(ignoreList.isEmpty), false)
    }

    params.put("filterModeShow", Boolean.box(filterModeShow))

    loadTopicScroller(params, topic, currentUserOpt.map(_.user), ignoreList.nonEmpty)

    val comments = getCommentList(topic, group, showDeleted)
    val hideSet = commentService.makeHideSet(comments, filterMode, ignoreList)

    val (commentsFiltered, unfilteredCount) = if (threadRoot != 0) {
      params.put("threadMode", Boolean.box(true))
      params.put("threadRoot", Integer.valueOf(threadRoot))

      (commentService.getCommentsSubtree(comments, threadRoot, hideSet),
        commentService.getCommentsSubtree(comments, threadRoot, Set.empty[Int]).size)
    } else {
      (getCommentsForPage(comments, page, tmpl.getProf.getMessages, hideSet),
        getCommentsForPage(comments, page, tmpl.getProf.getMessages, Set.empty[Int]).size)
    }

    params.put("unfilteredCount", Integer.valueOf(unfilteredCount))

    val commentsPrepared = prepareService.prepareCommentList(
      comments = comments,
      list = commentsFiltered,
      topic = topic,
      hideSet = hideSet,
      currentUser = currentUserOpt.map(_.user),
      profile = tmpl.getProf,
      ignoreList = ignoreList,
      filterShow = filterModeShow)

    params.put("commentsPrepared", commentsPrepared.asJava)

    if (comments.comments.isEmpty) {
      params.put("lastCommentId", Integer.valueOf(0))
    } else {
      params.put("lastCommentId", Integer.valueOf(comments.comments.last.id))
    }

    val ipBlockInfo = ipBlockDao.getBlockInfo(request.getRemoteAddr)

    params.put("ipBlockInfo", ipBlockInfo)
    params.put("modes", MessageTextService.postingModeSelector(currentUserOpt, tmpl.getFormatMode).asJava)

    val add = new CommentRequest
    add.setMode(tmpl.getFormatMode)
    params.put("add", add)

    if (pages > 1 && !showDeleted && threadRoot == 0 && comments.comments.nonEmpty) {
      params.put("pages", TopicController.buildPages(topic, tmpl.getProf.getMessages, filterModeShow, page))
    }

    params.put("moreLikeThisGetter", new Callable[java.util.List[java.util.List[MoreLikeThisTopic]]] {
      override def call(): util.List[util.List[MoreLikeThisTopic]] =
        moreLikeThisService.resultsOrNothing(topic, moreLikeThis, deadline)
    })

    params.put("showDeletedButton", Boolean.box(permissionService.allowViewDeletedComments(topic, currentUserOpt.map(_.user).orNull) && !showDeleted))
    params.put("dateJumps", prepareService.buildDateJumpSet(commentsFiltered, TopicController.JUMP_MIN_DURATION))

    new ModelAndView("view-topic", params.asJava)
  }

  private def getCommentList(topic: Topic, group: Group, showDeleted: Boolean): CommentList = {
    if (permissionService.getPostscore(group, topic) == TopicPermissionService.POSTSCORE_HIDE_COMMENTS && !showDeleted) {
      new CommentList(Vector.empty, Instant.EPOCH)
    } else {
      commentService.getCommentList(topic, showDeleted)
    }
  }

  private def getCommentsForPage(commentList: CommentList, page: Int, messagesPerPage: Int, hideSet: Set[Int]): Seq[Comment] = {
    val comments = commentList.comments

    if (page != -1) {
      val limit = messagesPerPage
      val offset = messagesPerPage * page

      comments.view.slice(offset, Math.min(offset + limit, comments.size)).filter(comment => !hideSet.contains(comment.id)).toSeq
    } else {
      comments.view.filter(comment => !hideSet.contains(comment.id)).toSeq
    }
  }

  private def loadTopicScroller(params: mutable.Map[String, AnyRef], topic: Topic, currentUser: Option[User],
                                useIgnoreList: Boolean): Unit = {
    val (prevMessage, nextMessage) = if (useIgnoreList) {
      (topicDao.getPreviousMessage(topic, currentUser.orNull), topicDao.getNextMessage(topic, currentUser.orNull))
    } else {
      (topicDao.getPreviousMessage(topic, null), topicDao.getNextMessage(topic, null))
    }

    params.put("prevMessage", prevMessage)
    params.put("nextMessage", nextMessage)

    val sectionScroller = sectionService.getScrollMode(topic.sectionId)

    val topScroller = if (prevMessage == null && nextMessage == null) {
      false
    } else {
      sectionScroller != SectionScrollModeEnum.NO_SCROLL
    }

    params.put("topScroller", Boolean.box(topScroller))

    val bottomScroller = sectionScroller != SectionScrollModeEnum.NO_SCROLL

    params.put("bottomScroller", Boolean.box(bottomScroller))
  }

  /**
   * Оставлено для старых ссылок /view-message.jsp
   *
   * @param msgid   id топика
   * @param page    страница топика
   * @param lastmod параметр для кэширования
   * @param filter  фильтр
   * @param output  ?
   * @return вовзращает редирект на новый код
   */
  @RequestMapping(path = Array("/view-message.jsp"))
  def getMessageOld(@RequestParam("msgid") msgid: Int, @RequestParam(value = "page", required = false) page: Integer,
                    @RequestParam(value = "lastmod", required = false) lastmod: java.lang.Long,
                    @RequestParam(value = "filter", required = false) filter: String,
                    @RequestParam(required = false) output: String): ModelAndView = {
    val topic = topicDao.getById(msgid)

    val link = new StringBuilder(topic.getLink)
    val params = new StringBuilder

    if (page != null) {
      link.append("/page").append(page)
    }

    if (lastmod != null && !topic.expired) {
      params.append("?lastmod=").append(topic.lastModified.getTime)
    }

    if (filter != null) {
      if (params.isEmpty) {
        params.append('?')
      } else {
        params.append('&')
      }

      params.append("filter=").append(filter)
    }

    if (output != null) {
      if (params.isEmpty) {
        params.append('?')
      } else {
        params.append('&')
      }

      params.append("output=").append(output)
    }

    link.append(params)

    new ModelAndView(new RedirectView(link.toString))
  }

  private def jumpMessage(msgid: Int, cid: Int, skipDeleted: Boolean): ModelAndView = AuthorizedOpt { currentUserOpt =>
    val topic = topicDao.getById(msgid)
    val group = groupDao.getGroup(topic.groupId)

    var comments = getCommentList(topic, group, showDeleted = false)

    var node = comments.getNodeOpt(cid).orNull

    if (node == null && skipDeleted) {
      val list = comments.comments

      if (list.isEmpty) {
        return new ModelAndView(new RedirectView(topic.getLink))
      }

      val c = list.find(_.id > cid).getOrElse(list.last)
      node = comments.getNode(c.id)
    }

    var deleted = false

    if (node == null && currentUserOpt.exists(_.moderator)) {
      comments = getCommentList(topic, group, showDeleted = true)
      node = comments.getNode(cid)
      deleted = true
    }

    if (node == null) {
      throw new MessageNotFoundException(topic, cid, s"Сообщение #$cid было удалено или не существует")
    }

    val tmpl = Template.getTemplate

    val pagenum = if (deleted) 0 else comments.getCommentPage(node.getComment, tmpl.getProf.getMessages)

    var redirectUrl = TopicLinkBuilder.pageLink(topic, pagenum).lastmod(tmpl.getProf.getMessages).comment(node.getComment.id)

    if (deleted) redirectUrl = redirectUrl.showDeleted

    if (currentUserOpt.isDefined && !deleted) {
      val ignoreList = ignoreListDao.get(currentUserOpt.get.user.getId)
      val hideSet = commentService.makeHideSet(comments, TopicController.getDefaultFilter(ignoreList.isEmpty), ignoreList)

      if (hideSet.contains(node.getComment.id)) {
        redirectUrl = redirectUrl.filterShow()
      }
    }

    new ModelAndView(new RedirectView(redirectUrl.build))
  }

  @RequestMapping(value = Array("/jump-message.jsp"), method = Array(RequestMethod.GET, RequestMethod.HEAD))
  def jumpMessage(@RequestParam msgid: Int, @RequestParam(required = false) page: Integer,
                  @RequestParam(required = false) cid: Integer): ModelAndView = {
    if (cid != null) {
      return jumpMessage(msgid, cid, skipDeleted = false)
    }

    val topic = topicDao.getById(msgid)

    val builder: TopicLinkBuilder = if (page != null) {
      TopicLinkBuilder.pageLink(topic, page)
    } else {
      TopicLinkBuilder.baseLink(topic)
    }

    new ModelAndView(new RedirectView(builder.build))
  }

  @ExceptionHandler(Array(classOf[MessageNotFoundException]))
  @ResponseStatus(HttpStatus.NOT_FOUND)
  def handleMessageNotFoundException(ex: MessageNotFoundException): ModelAndView = {
    TopicController.logger.debug("Not found", ex)

    if (ex.getTopic != null) {
      val mav = new ModelAndView("errors/good-penguin")
      val topic = ex.getTopic
      mav.addObject("msgTitle", "Ошибка: сообщения не существует")
      mav.addObject("msgHeader", "Сообщение удалено или не существует")
      mav.addObject("msgMessage", String.format("Сообщение %d в топике <a href=\"%s\">%s</a> удалено или не существует", ex.getId, topic.getLink, topic.title))
      mav
    } else {
      new ModelAndView("errors/code404")
    }
  }
}