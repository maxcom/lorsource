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

import akka.actor.ActorSystem
import com.typesafe.scalalogging.StrictLogging
import org.joda.time.DateTime
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.*
import org.springframework.web.context.request.WebRequest
import org.springframework.web.servlet.view.RedirectView
import org.springframework.web.servlet.{ModelAndView, View}
import ru.org.linux.auth.AuthUtil
import ru.org.linux.group.{Group, GroupDao, GroupNotFoundException}
import ru.org.linux.section.{Section, SectionNotFoundException, SectionService}
import ru.org.linux.site.{ScriptErrorException, Template}
import ru.org.linux.tag.{TagPageController, TagRef, TagService}
import ru.org.linux.user.UserErrorException
import ru.org.linux.util.RichFuture.*
import ru.org.linux.util.{DateUtil, ServletParameterException, ServletParameterMissingException}

import java.net.URLEncoder
import java.util.concurrent.CompletionStage
import javax.servlet.http.HttpServletResponse
import scala.compat.java8.FutureConverters.*
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.*
import scala.concurrent.duration.Deadline
import scala.jdk.CollectionConverters.*

object TopicListController {
  def setExpireHeaders(response: HttpServletResponse, year: Integer, month: Integer): Unit = {
    if (month == null) {
      response.setDateHeader("Expires", System.currentTimeMillis + 60 * 1000)
      response.setDateHeader("Last-Modified", System.currentTimeMillis)
    } else {
      val endOfMonth = new DateTime(year, month, 1, 0, 0).plusMonths(1)

      if (endOfMonth.isBeforeNow) {
        response.setDateHeader("Last-Modified", endOfMonth.getMillis)
      } else {
        response.setDateHeader("Expires", System.currentTimeMillis + 60 * 1000)
        response.setDateHeader("Last-Modified", System.currentTimeMillis)
      }
    }
  }

  private def calculatePTitle(section: Section, topicListForm: TopicListRequest): String = {
    if (topicListForm.getMonth == null) {
      section.getName
    } else {
      s"Архив: ${section.getName}, ${topicListForm.getYear}, ${DateUtil.getMonth(topicListForm.getMonth)}"
    }
  }

  private def calculateNavTitle(section: Section, group: Option[Group], topicListForm: TopicListRequest): String = {
    val navTitle = new StringBuilder(section.getName)

    group foreach { group =>
      navTitle.append(s" «${group.getTitle}»")
    }

    if (topicListForm.getMonth != null) {
      navTitle.append(" - Архив ").append(topicListForm.getYear).append(", ").append(DateUtil.getMonth(topicListForm.getMonth))
    }

    navTitle.toString
  }

  val RssFilters: Set[String] = Set("all", "notalks", "tech")
}

@Controller
class TopicListController(sectionService: SectionService, topicListService: TopicListService,
                          prepareService: TopicPrepareService, tagService: TagService,
                          groupDao: GroupDao, actorSystem: ActorSystem) extends StrictLogging {

  private implicit val akka: ActorSystem = actorSystem

  private def mainTopicsFeedHandler(section: Section, topicListForm: TopicListRequest, response: HttpServletResponse,
                                    group: Option[Group]): ModelAndView = {
    checkRequestConditions(section, group, topicListForm)

    val modelAndView = new ModelAndView("view-news")

    group foreach { group =>
      modelAndView.addObject("group", group)
    }

    modelAndView.addObject("url", "view-news.jsp")
    modelAndView.addObject("section", section)
    modelAndView.addObject("archiveLink", section.getArchiveLink)
    modelAndView.addObject("groupList", groupDao.getGroups(section))

    TopicListController.setExpireHeaders(response, topicListForm.getYear, topicListForm.getMonth)

    modelAndView.addObject("navtitle", TopicListController.calculateNavTitle(section, group, topicListForm))

    topicListForm.setOffset(topicListService.fixOffset(topicListForm.getOffset))

    val tmpl = Template.getTemplate

    val messages = topicListService.getTopicsFeed(
      section, group.orNull, null, topicListForm.getOffset, topicListForm.getYear, topicListForm.getMonth, 20, AuthUtil.getCurrentUser)

    modelAndView.addObject(
      "messages",
      prepareService.prepareTopicsForUser(messages, AuthUtil.getCurrentUser, tmpl.getProf, loadUserpics = false))

    modelAndView.addObject("offsetNavigation", topicListForm.getMonth == null)

    modelAndView
  }

  private def activeTopTags(section: Section, group: Option[Group], deadline: Deadline): Future[Seq[TagRef]] = {
    val activeTagsF = tagService.getActiveTopTags(section, group)

    activeTagsF.withTimeout(deadline.timeLeft).recover {
      case ex: TimeoutException =>
        logger.warn(s"Active top tags search timed out (${ex.getMessage})")
        Seq.empty
      case ex =>
        logger.warn("Unable to find active top tags", ex)
        Seq.empty
    }
  }

  @RequestMapping(Array("/{section:(?:news)|(?:polls)|(?:articles)|(?:gallery)}/"))
  def topics(@PathVariable("section") sectionName: String, topicListForm: TopicListRequest,
             response: HttpServletResponse): CompletionStage[ModelAndView] = {
    val deadline = TagPageController.Timeout.fromNow

    val section = sectionService.getSectionByName(sectionName)

    val activeTagsF = activeTopTags(section, None, deadline)

    val modelAndView = mainTopicsFeedHandler(section, topicListForm, response, None)

    modelAndView.addObject("ptitle", TopicListController.calculatePTitle(section, topicListForm))
    modelAndView.addObject("url", section.getNewsViewerLink)
    modelAndView.addObject("rssLink", s"section-rss.jsp?section=${section.getId}")

    activeTagsF.map { activeTags =>
      if (activeTags.nonEmpty) {
        modelAndView.addObject("activeTags", activeTags.asJava)
      }

      modelAndView
    }.toJava
  }

  @RequestMapping(Array("/forum/lenta"))
  def forum(topicListForm: TopicListRequest, response: HttpServletResponse): CompletionStage[ModelAndView] = {
    topics("forum", topicListForm, response)
  }

  @RequestMapping(Array("/{section:(?:news)|(?:polls)|(?:articles)|(?:gallery)}/{group:[^.]+}"))
  def topicsByGroup(@PathVariable("section") sectionName: String, topicListForm: TopicListRequest,
                    @PathVariable("group") groupName: String, response: HttpServletResponse): CompletionStage[ModelAndView] = {
    val deadline = TagPageController.Timeout.fromNow

    val section = sectionService.getSectionByName(sectionName)

    val group = groupDao.getGroup(section, groupName)

    val activeTagsF = activeTopTags(section, None, deadline)

    val modelAndView = mainTopicsFeedHandler(section, topicListForm, response, Some(group))

    modelAndView.addObject("ptitle", s"${section.getName} - ${group.getTitle}")
    modelAndView.addObject("url", group.getUrl)

    activeTagsF.map { activeTags =>
      if (activeTags.nonEmpty) {
        modelAndView.addObject("activeTags", activeTags.asJava)
      }

      modelAndView
    }.toJava
  }

  @RequestMapping(Array("/{section}/archive/{year}/{month}"))
  def galleryArchive(@PathVariable section: String, @PathVariable year: Int, @PathVariable month: Int,
                     response: HttpServletResponse): ModelAndView = {
    val sectionObject = sectionService.getSectionByName(section)

    if (sectionObject.isPremoderated) {
      val topicListForm = new TopicListRequest

      topicListForm.setYear(year)
      topicListForm.setMonth(month)

      val modelAndView = mainTopicsFeedHandler(sectionObject, topicListForm, response, None)

      modelAndView.addObject("ptitle", TopicListController.calculatePTitle(sectionObject, topicListForm))

      modelAndView
    } else {
      new ModelAndView(new RedirectView(sectionObject.getSectionLink))
    }
  }

  @RequestMapping(value = Array("/show-topics.jsp"), method = Array(RequestMethod.GET))
  def showUserTopics(@RequestParam("nick") nick: String,
                     @RequestParam(value = "output", required = false) output: String): View = {
    if (output != null) {
      new RedirectView(s"/people/$nick/?output=rss")
    } else {
      new RedirectView(s"/people/$nick/")
    }
  }

  @RequestMapping(value = Array("/view-news.jsp"), params = Array("section", "!tag"))
  def oldLink(topicListForm: TopicListRequest, @RequestParam("section") sectionId: Int,
              @RequestParam(value = "group", defaultValue = "0") groupId: Int): View = {
    val section = sectionService.getSection(sectionId)

    val redirectLink = new StringBuilder(section.getNewsViewerLink)

    if (topicListForm.getYear != null && topicListForm.getMonth != null) {
      redirectLink.append(topicListForm.getYear).append('/').append(topicListForm.getMonth)
    } else if (groupId > 0) {
      val group = groupDao.getGroup(groupId)
      redirectLink.append(group.getUrlName).append('/')
    }

    val queryStr = if (topicListForm.getOffset == null) {
      ""
    } else {
      "?" + URLEncoder.encode("offset=" + topicListForm.getOffset, "UTF-8")
    }

    redirectLink.append(queryStr)

    new RedirectView(redirectLink.toString)
  }

  @RequestMapping(Array("/section-rss.jsp"))
  def showRSS(topicListForm: TopicListRequest,
              @RequestParam(value = "section", defaultValue = "1") sectionId: Int,
              @RequestParam(value = "group", defaultValue = "0") groupId: Int,
              @RequestParam(value = "filter", required = false) filter: String,
              webRequest: WebRequest): ModelAndView = {
    if (filter != null && !TopicListController.RssFilters.contains(filter)) {
      throw new UserErrorException("Некорректное значение filter")
    }

    val section = sectionService.getSection(sectionId)
    var ptitle = section.getName

    val group = if (groupId != 0) {
      val g = groupDao.getGroup(groupId)
      ptitle += " - " + g.getTitle
      Some(g)
    } else {
      None
    }

    checkRequestConditions(section, group, topicListForm)
    val modelAndView = new ModelAndView("section-rss")

    group.foreach { group =>
      modelAndView.addObject("group", group)
    }

    modelAndView.addObject("section", section)
    modelAndView.addObject("ptitle", ptitle)

    val notalks = "notalks" == filter
    val tech = "tech" == filter

    val fromDate = DateTime.now.minusMonths(3)
    val messages = topicListService.getRssTopicsFeed(section, group.orNull, fromDate.toDate, notalks, tech)

    // не лучший вариант, так как включает комментарии
    // по хорошему тут надо учитывать только правки текста топика
    val lastModified = messages.asScala.view.map(_.lastModified.getTime).maxOption

    if (lastModified.exists(webRequest.checkNotModified)) {
      null
    } else {
      modelAndView.addObject("messages", prepareService.prepareTopics(messages.asScala.toSeq).asJava)

      modelAndView
    }
  }

  private def checkRequestConditions(section: Section, group: Option[Group], topicListForm: TopicListRequest): Unit = {
    if (topicListForm.getMonth != null && topicListForm.getYear == null) {
      throw new ServletParameterMissingException("year")
    }

    if (section == null) {
      throw new ServletParameterException("section or tag required")
    }

    group foreach { group =>
      if (group.getSectionId != section.getId) {
        throw new ScriptErrorException(s"группа #${group.getId} не принадлежит разделу #${section.getId}")
      }
    }
  }

  @ExceptionHandler(Array(classOf[GroupNotFoundException], classOf[SectionNotFoundException]))
  @ResponseStatus(HttpStatus.NOT_FOUND)
  def handleNotFoundException: ModelAndView = new ModelAndView("errors/code404")
}