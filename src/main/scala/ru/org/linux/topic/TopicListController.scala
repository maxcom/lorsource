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
import org.joda.time.DateTime
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.*
import org.springframework.web.context.request.WebRequest
import org.springframework.web.servlet.view.RedirectView
import org.springframework.web.servlet.{ModelAndView, View}
import ru.org.linux.auth.AuthUtil.AuthorizedOpt
import ru.org.linux.group.{Group, GroupDao, GroupNotFoundException, GroupPermissionService}
import ru.org.linux.section.{Section, SectionController, SectionNotFoundException, SectionService}
import ru.org.linux.site.{ScriptErrorException, Template}
import ru.org.linux.tag.{TagPageController, TagService}
import ru.org.linux.topic.TopicListController.ForumFilter.{NoTalks, Tech}
import ru.org.linux.topic.TopicListController.{ForumFilter, ForumFilters, calculatePTitle}
import ru.org.linux.user.UserErrorException
import ru.org.linux.util.{DateUtil, ServletParameterException}

import java.util.concurrent.CompletionStage
import javax.annotation.Nullable
import scala.compat.java8.FutureConverters.*
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.jdk.CollectionConverters.*

object TopicListController {
  private def calculatePTitle(section: Section, groupOpt: Option[Group], topicListForm: TopicListRequest): String = {
    groupOpt match {
      case Some(group) =>
        s"${section.getName} - ${group.title}"
      case None =>
        if (topicListForm.yearMonth.isEmpty) {
          section.getName + topicListForm.filter.map(f => s" (${f.title})").getOrElse("")
        } else {
          s"Архив: ${section.getName}, ${topicListForm.getYear.get}, ${DateUtil.getMonth(topicListForm.getMonth.get)}"
        }
    }
  }

  private def calculateNavTitle(section: Section, group: Option[Group], topicListForm: TopicListRequest): String = {
    val navTitle = new StringBuilder(section.getName)

    group.foreach { group =>
      navTitle.append(s" «${group.title}»")
    }

    topicListForm.filter.foreach { f =>
      navTitle.append(s" (${f.title})")
    }

    if (topicListForm.getMonth.isDefined) {
      navTitle.append(" - Архив ").append(topicListForm.getYear.get).append(", ").append(DateUtil.getMonth(topicListForm.getMonth.get))
    }

    navTitle.toString
  }

  sealed trait ForumFilter {
    def id: String
    def title: String

    final def getId: String = id
    final def getTitle: String = title
  }

  object ForumFilter {
    case object NoTalks extends ForumFilter {
      override val id = "notalks"
      override val title = "без talks"
    }

    case object Tech extends ForumFilter {
      override val id = "tech"
      override val title = "тех. форум"
    }
  }

  private val ForumFilters = Seq(NoTalks, Tech)
}

@Controller
class TopicListController(sectionService: SectionService, topicListService: TopicListService,
                          prepareService: TopicPrepareService, tagService: TagService,
                          groupDao: GroupDao, groupPermissionService: GroupPermissionService) extends StrictLogging {
  private def mainTopicsFeedHandler(section: Section, topicListForm: TopicListRequest,
                                    group: Option[Group]): Future[ModelAndView] = AuthorizedOpt { currentUserOpt =>
    val deadline = TagPageController.Timeout.fromNow

    checkRequestConditions(section, group)

    val activeTagsF = if (topicListForm.yearMonth.isEmpty) {
      tagService.getActiveTopTags(section, group, topicListForm.filter, deadline)
    } else {
      Future.successful(Seq.empty)
    }

    val modelAndView = new ModelAndView("view-news")

    modelAndView.addObject("ptitle", calculatePTitle(section, group, topicListForm))

    modelAndView.addObject("topicListRequest", topicListForm)

    group foreach { group =>
      modelAndView.addObject("group", group)
    }

    modelAndView.addObject("section", section)
    modelAndView.addObject("archiveLink", section.getArchiveLink)

    if (section.getId != Section.SECTION_FORUM) {
      modelAndView.addObject("groupList",
        SectionController.groupsSorted(groupDao.getGroups(section).asScala).asJava)
    } else {
      modelAndView.addObject("filters", ForumFilters.asJava)
      modelAndView.addObject("filter", topicListForm.filter.getOrElse(""))
    }

    modelAndView.addObject("navtitle", TopicListController.calculateNavTitle(section, group, topicListForm))

    val tmpl = Template.getTemplate

    val messages = topicListService.getTopicsFeed(section, group, None, topicListForm.offset,
      topicListForm.yearMonth, 20, currentUserOpt.map(_.user), topicListForm.filter.contains(NoTalks),
      topicListForm.filter.contains(Tech))

    modelAndView.addObject(
      "messages",
      prepareService.prepareTopicsForUser(messages, currentUserOpt.map(_.user), tmpl.getProf, loadUserpics = false))

    modelAndView.addObject("offsetNavigation", topicListForm.yearMonth.isEmpty)

    val addUrl = group match {
      case Some(group) if groupPermissionService.isTopicPostingAllowed(group, currentUserOpt.map(_.user).orNull) =>
        AddTopicController.getAddUrl(group)
      case None if groupPermissionService.isTopicPostingAllowed(section, currentUserOpt.map(_.user)) =>
        AddTopicController.getAddUrl(section)
      case _ =>
        ""
    }

    modelAndView.addObject("addUrl", addUrl)

    activeTagsF.map { activeTags =>
      if (activeTags.nonEmpty) {
        modelAndView.addObject("activeTags", activeTags.asJava)
      }

      modelAndView
    }
  }

  @RequestMapping(path = Array("/{section:(?:news)|(?:polls)|(?:articles)|(?:gallery)}/"))
  def topics(@PathVariable("section") sectionName: String,
             @RequestParam(value="offset", defaultValue = "0") offset: Int): CompletionStage[ModelAndView] = {
    val section = sectionService.getSectionByName(sectionName)

    val topicListForm = TopicListRequest.ofOffset(offset)

    mainTopicsFeedHandler(section, topicListForm, None).map { modelAndView =>
      modelAndView.addObject("url", section.getNewsViewerLink)
      modelAndView.addObject("rssLink", s"section-rss.jsp?section=${section.getId}")
    }.toJava
  }

  private def parseFilter(@Nullable value: String): Option[ForumFilter] = {
    Option(value)
      .map(v => ForumFilters.find(_.id == v).getOrElse(throw new UserErrorException("Некорректное значение filter")))
  }

  @RequestMapping(path = Array("/forum/lenta"))
  def forum(@RequestParam(value="offset", defaultValue = "0") offset: Int,
            @RequestParam(value = "filter", required = false) filter: String): CompletionStage[ModelAndView] = {
    val section = sectionService.getSection(Section.SECTION_FORUM)

    val topicListForm = TopicListRequest.ofOffset(offset).copy(filter = parseFilter(filter))

    mainTopicsFeedHandler(section, topicListForm, None).map { modelAndView =>
      if (filter==null) {
        modelAndView.addObject("url", section.getNewsViewerLink)
        modelAndView.addObject("rssLink", s"section-rss.jsp?section=${section.getId}")
      } else {
        modelAndView.addObject("url", section.getNewsViewerLink + s"?filter=$filter")
        modelAndView.addObject("rssLink", s"section-rss.jsp?section=${section.getId}&filter=$filter")
      }
    }.toJava
  }

  @RequestMapping(path = Array("/{section:(?:news)|(?:polls)|(?:articles)|(?:gallery)}/{group:[^.]+}"))
  def topicsByGroup(@PathVariable("section") sectionName: String,
                    @RequestParam(value="offset", defaultValue = "0") offset: Int,
                    @PathVariable("group") groupName: String): CompletionStage[ModelAndView] = {
    val section = sectionService.getSectionByName(sectionName)

    val group = groupDao.getGroup(section, groupName)

    val topicListForm = TopicListRequest.ofOffset(offset)

    mainTopicsFeedHandler(section, topicListForm, Some(group)).map { modelAndView =>
      modelAndView.addObject("url", group.getUrl)
    }.toJava
  }

  @RequestMapping(path = Array("/{section}/archive/{year:\\d{4}}/{month}"))
  def sectionArchive(@PathVariable section: String, @PathVariable year: Int, @PathVariable month: Int): CompletionStage[ModelAndView] = {
    val sectionObject = sectionService.getSectionByName(section)

    (if (sectionObject.isPremoderated) {
      val topicListForm = TopicListRequest.orYearMonth(year, month)

      mainTopicsFeedHandler(sectionObject, topicListForm, None)
    } else {
      Future.successful(new ModelAndView(new RedirectView(sectionObject.getSectionLink)))
    }).toJava
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

  @RequestMapping(path = Array("/section-rss.jsp"))
  def showRSS(@RequestParam(value = "section", defaultValue = "1") sectionId: Int,
              @RequestParam(value = "group", defaultValue = "0") groupId: Int,
              @RequestParam(value = "filter", required = false) filter: String,
              webRequest: WebRequest): ModelAndView = {
    val forumFilter = parseFilter(filter)

    val section = sectionService.getSection(sectionId)
    var ptitle = section.getName

    val group = if (groupId != 0) {
      val g = groupDao.getGroup(groupId)
      ptitle += " - " + g.title
      Some(g)
    } else {
      None
    }

    forumFilter.foreach { f =>
      ptitle += s" (${f.title})"
    }

    checkRequestConditions(section, group)
    val modelAndView = new ModelAndView("section-rss")

    group.foreach { group =>
      modelAndView.addObject("group", group)
    }

    modelAndView.addObject("section", section)
    modelAndView.addObject("ptitle", ptitle)

    val notalks = forumFilter.contains(NoTalks)
    val tech = forumFilter.contains(Tech)

    val fromDate = DateTime.now.minusMonths(3)
    val messages = topicListService.getRssTopicsFeed(section, group, fromDate.toDate, notalks, tech)

    // не лучший вариант, так как включает комментарии
    // по хорошему тут надо учитывать только правки текста топика
    val lastModified = messages.view.map(_.lastModified.getTime).maxOption

    if (lastModified.exists(webRequest.checkNotModified)) {
      null
    } else {
      modelAndView.addObject("messages", prepareService.prepareTopics(messages.toSeq).asJava)

      modelAndView
    }
  }

  private def checkRequestConditions(section: Section, group: Option[Group]): Unit = {
    if (section == null) {
      throw new ServletParameterException("section or tag required")
    }

    group foreach { group =>
      if (group.sectionId != section.getId) {
        throw new ScriptErrorException(s"группа #${group.id} не принадлежит разделу #${section.getId}")
      }
    }
  }

  @ExceptionHandler(Array(classOf[GroupNotFoundException], classOf[SectionNotFoundException]))
  @ResponseStatus(HttpStatus.NOT_FOUND)
  def handleNotFoundException: ModelAndView = new ModelAndView("errors/code404")
}