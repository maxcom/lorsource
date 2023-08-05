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
import ru.org.linux.auth.AuthUtil
import ru.org.linux.group.{Group, GroupDao, GroupNotFoundException}
import ru.org.linux.section.{Section, SectionController, SectionNotFoundException, SectionService}
import ru.org.linux.site.{ScriptErrorException, Template}
import ru.org.linux.tag.{TagPageController, TagService}
import ru.org.linux.user.UserErrorException
import ru.org.linux.util.{DateUtil, ServletParameterException, ServletParameterMissingException}

import java.net.URLEncoder
import java.util.concurrent.CompletionStage
import javax.servlet.http.HttpServletResponse
import scala.compat.java8.FutureConverters.*
import scala.concurrent.ExecutionContext.Implicits.global
import scala.jdk.CollectionConverters.*
import scala.jdk.OptionConverters.RichOption

object TopicListController {
  private def calculatePTitle(section: Section, topicListForm: TopicListRequest): String = {
    if (topicListForm.yearMonth.isEmpty) {
      section.getName
    } else {
      s"Архив: ${section.getName}, ${topicListForm.getYear.get}, ${DateUtil.getMonth(topicListForm.getMonth.get)}"
    }
  }

  private def calculateNavTitle(section: Section, group: Option[Group], topicListForm: TopicListRequest): String = {
    val navTitle = new StringBuilder(section.getName)

    group foreach { group =>
      navTitle.append(s" «${group.getTitle}»")
    }

    if (topicListForm.getMonth.isDefined) {
      navTitle.append(" - Архив ").append(topicListForm.getYear.get).append(", ").append(DateUtil.getMonth(topicListForm.getMonth.get))
    }

    navTitle.toString
  }

  val RssFilters: Set[String] = Set("all", "notalks", "tech")
}

@Controller
class TopicListController(sectionService: SectionService, topicListService: TopicListService,
                          prepareService: TopicPrepareService, tagService: TagService,
                          groupDao: GroupDao) extends StrictLogging {
  private def mainTopicsFeedHandler(section: Section, topicListForm: TopicListRequest,
                                    group: Option[Group]): ModelAndView = {
    checkRequestConditions(section, group)

    val modelAndView = new ModelAndView("view-news")

    group foreach { group =>
      modelAndView.addObject("group", group)
    }

    modelAndView.addObject("section", section)
    modelAndView.addObject("archiveLink", section.getArchiveLink)

    modelAndView.addObject("groupList",
      SectionController.groupsSorted(groupDao.getGroups(section).asScala).asJava)

    modelAndView.addObject("navtitle", TopicListController.calculateNavTitle(section, group, topicListForm))

    val tmpl = Template.getTemplate

    val messages = topicListService.getTopicsFeed(
      section, group.orNull, null, topicListForm.offset, topicListForm.getYear.map(Integer.valueOf).toJava,
      topicListForm.getMonth.map(Integer.valueOf).toJava, 20, AuthUtil.getCurrentUser)

    modelAndView.addObject(
      "messages",
      prepareService.prepareTopicsForUser(messages, AuthUtil.getCurrentUser, tmpl.getProf, loadUserpics = false))

    modelAndView.addObject("offsetNavigation", topicListForm.yearMonth.isEmpty)

    modelAndView
  }

  @RequestMapping(Array("/{section:(?:news)|(?:polls)|(?:articles)|(?:gallery)}/"))
  def topics(@PathVariable("section") sectionName: String,
             @RequestParam(value="offset", defaultValue = "0") offset: Int): CompletionStage[ModelAndView] = {
    val deadline = TagPageController.Timeout.fromNow

    val section = sectionService.getSectionByName(sectionName)

    val activeTagsF = tagService.getActiveTopTags(section, None, deadline)

    val topicListForm = TopicListRequest.ofOffset(offset)

    val modelAndView = mainTopicsFeedHandler(section, topicListForm, None)

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
  def forum(@RequestParam(value="offset", defaultValue = "0") offset: Int): CompletionStage[ModelAndView] = {
    topics("forum", offset)
  }

  @RequestMapping(Array("/{section:(?:news)|(?:polls)|(?:articles)|(?:gallery)}/{group:[^.]+}"))
  def topicsByGroup(@PathVariable("section") sectionName: String,
                    @RequestParam(value="offset", defaultValue = "0") offset: Int,
                    @PathVariable("group") groupName: String): CompletionStage[ModelAndView] = {
    val deadline = TagPageController.Timeout.fromNow

    val section = sectionService.getSectionByName(sectionName)

    val group = groupDao.getGroup(section, groupName)

    val activeTagsF = tagService.getActiveTopTags(section, Some(group), deadline)

    val topicListForm = TopicListRequest.ofOffset(offset)

    val modelAndView = mainTopicsFeedHandler(section, topicListForm, Some(group))

    modelAndView.addObject("ptitle", s"${section.getName} - ${group.getTitle}")
    modelAndView.addObject("url", group.getUrl)

    activeTagsF.map { activeTags =>
      if (activeTags.nonEmpty) {
        modelAndView.addObject("activeTags", activeTags.asJava)
      }

      modelAndView
    }.toJava
  }

  @RequestMapping(Array("/{section}/archive/{year:\\d{4}}/{month}"))
  def sectionArchive(@PathVariable section: String, @PathVariable year: Int, @PathVariable month: Int): ModelAndView = {
    val sectionObject = sectionService.getSectionByName(section)

    if (sectionObject.isPremoderated) {
      val topicListForm = TopicListRequest.orYearMonth(year, month)

      val modelAndView = mainTopicsFeedHandler(sectionObject, topicListForm, None)

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

  @RequestMapping(Array("/section-rss.jsp"))
  def showRSS(@RequestParam(value = "section", defaultValue = "1") sectionId: Int,
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

    checkRequestConditions(section, group)
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

  private def checkRequestConditions(section: Section, group: Option[Group]): Unit = {
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