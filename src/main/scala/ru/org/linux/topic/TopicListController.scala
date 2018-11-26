/*
 * Copyright 1998-2018 Linux.org.ru
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

import java.net.URLEncoder
import java.util.concurrent.CompletionStage
import javax.servlet.http.{HttpServletRequest, HttpServletResponse}

import akka.actor.ActorSystem
import com.typesafe.scalalogging.StrictLogging
import org.joda.time.DateTime
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation._
import org.springframework.web.servlet.view.RedirectView
import org.springframework.web.servlet.{ModelAndView, View}
import ru.org.linux.group.{Group, GroupDao, GroupNotFoundException}
import ru.org.linux.section.{Section, SectionNotFoundException, SectionService}
import ru.org.linux.site.{ScriptErrorException, Template}
import ru.org.linux.tag.{TagPageController, TagService}
import ru.org.linux.user.UserErrorException
import ru.org.linux.util.RichFuture._
import ru.org.linux.util.{DateUtil, ServletParameterException, ServletParameterMissingException}

import scala.collection.JavaConverters._
import scala.compat.java8.FutureConverters._
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent._

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
      "Архив: " + section.getName + ", " + topicListForm.getYear + ", " + DateUtil.getMonth(topicListForm.getMonth)
    }
  }

  private def calculateNavTitle(section: Section, group: Option[Group], topicListForm: TopicListRequest): String = {
    val navTitle = new StringBuilder(section.getName)

    group foreach { group ⇒
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

  private implicit val akka = actorSystem

  private def mainTopicsFeedHandler(section: Section, request: HttpServletRequest,
                                    topicListForm: TopicListRequest, response: HttpServletResponse,
                                    group: Option[Group]): ModelAndView = {
    checkRequestConditions(section, group, topicListForm)

    val modelAndView = new ModelAndView("view-news")

    group foreach { group ⇒
      modelAndView.addObject("group", group)
    }

    modelAndView.addObject("url", "view-news.jsp")
    modelAndView.addObject("section", section)
    modelAndView.addObject("archiveLink", section.getArchiveLink)

    TopicListController.setExpireHeaders(response, topicListForm.getYear, topicListForm.getMonth)

    modelAndView.addObject("navtitle", TopicListController.calculateNavTitle(section, group, topicListForm))

    topicListForm.setOffset(topicListService.fixOffset(topicListForm.getOffset))

    val messages = topicListService.getTopicsFeed(
      section, group.orNull, null, topicListForm.getOffset, topicListForm.getYear, topicListForm.getMonth, 20)

    val tmpl = Template.getTemplate(request)

    modelAndView.addObject(
      "messages",
      prepareService.prepareMessagesForUser(messages, tmpl.getCurrentUser, tmpl.getProf, false))

    modelAndView.addObject("offsetNavigation", topicListForm.getMonth == null)

    modelAndView
  }

  @RequestMapping(Array("/{section:(?:news)|(?:polls)|(?:gallery)}/"))
  def topics(request: HttpServletRequest, @PathVariable("section") sectionName: String,
             topicListForm: TopicListRequest, response: HttpServletResponse): CompletionStage[ModelAndView] = {
    val deadline = TagPageController.Timeout.fromNow

    val section = sectionService.getSectionByName(sectionName)

    val activeTagsF = {
      tagService.getActiveTopTags(section) map { activeTags ⇒
        if (activeTags.nonEmpty) {
          Some(activeTags)
        } else {
          None
        }
      }
    }

    val modelAndView = mainTopicsFeedHandler(section, request, topicListForm, response, None)

    modelAndView.addObject("ptitle", TopicListController.calculatePTitle(section, topicListForm))
    modelAndView.addObject("url", section.getNewsViewerLink)
    modelAndView.addObject("rssLink", s"section-rss.jsp?section=${section.getId}")

    activeTagsF withTimeout deadline.timeLeft recover {
      case ex: TimeoutException ⇒
        logger.warn(s"Active top tags search timed out (${ex.getMessage})")
        None
      case ex ⇒
        logger.warn("Unable to find active top tags", ex)
        None
    } map { activeTags ⇒
      activeTags foreach { tags ⇒
        modelAndView.addObject("activeTags", tags.asJava)
      }

      modelAndView
    } toJava
  }

  @RequestMapping(Array("/forum/lenta"))
  def forum(request: HttpServletRequest, topicListForm: TopicListRequest,
            response: HttpServletResponse): CompletionStage[ModelAndView] = {
    topics(request, "forum", topicListForm, response)
  }

  @RequestMapping(Array("/{section:(?:news)|(?:polls)|(?:gallery)}/{group:[^.]+}"))
  def topicsByGroup(@PathVariable("section") sectionName: String, request: HttpServletRequest,
                    topicListForm: TopicListRequest, @PathVariable("group") groupName: String,
                    response: HttpServletResponse): ModelAndView = {
    val section = sectionService.getSectionByName(sectionName)

    group(section, request, topicListForm, groupName, response)
  }

  @RequestMapping(Array("/{section}/archive/{year}/{month}"))
  def galleryArchive(request: HttpServletRequest, @PathVariable section: String, @PathVariable year: Int,
                     @PathVariable month: Int, response: HttpServletResponse): ModelAndView = {
    val sectionObject = sectionService.getSectionByName(section)

    if (sectionObject.isPremoderated) {
      val topicListForm = new TopicListRequest

      topicListForm.setYear(year)
      topicListForm.setMonth(month)

      val modelAndView = mainTopicsFeedHandler(sectionObject, request, topicListForm, response, None)

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
      new RedirectView("/people/" + nick + "/?output=rss")
    } else {
      new RedirectView("/people/" + nick + '/')
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
  def showRSS(request: HttpServletRequest, topicListForm: TopicListRequest,
              @RequestParam(value = "section", defaultValue = "1") sectionId: Int,
              @RequestParam(value = "group", defaultValue = "0") groupId: Int,
              @RequestParam(value = "filter", required = false) filter: String): ModelAndView = {
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

    group foreach { group ⇒
      modelAndView.addObject("group", group)
    }

    modelAndView.addObject("section", section)
    modelAndView.addObject("ptitle", ptitle)

    val notalks = "notalks" == filter
    val tech = "tech" == filter

    val fromDate = DateTime.now.minusMonths(3)
    val messages = topicListService.getRssTopicsFeed(section, group.orNull, fromDate.toDate, notalks, tech)

    modelAndView.addObject("messages", prepareService.prepareMessages(messages))

    modelAndView
  }

  private def group(section: Section, request: HttpServletRequest, topicListForm: TopicListRequest,
                    groupName: String, response: HttpServletResponse): ModelAndView = {
    val group = groupDao.getGroup(section, groupName)

    val modelAndView = mainTopicsFeedHandler(section, request, topicListForm, response, Some(group))

    modelAndView.addObject("ptitle", section.getName + " - " + group.getTitle)
    modelAndView.addObject("url", group.getUrl)

    modelAndView
  }

  private def checkRequestConditions(section: Section, group: Option[Group], topicListForm: TopicListRequest): Unit = {
    if (topicListForm.getMonth != null && topicListForm.getYear == null) {
      throw new ServletParameterMissingException("year")
    }

    if (section == null) {
      throw new ServletParameterException("section or tag required")
    }

    group foreach { group ⇒
      if (group.getSectionId != section.getId) {
        throw new ScriptErrorException("группа #" + group.getId + " не принадлежит разделу #" + section.getId)
      }
    }
  }

  @ExceptionHandler(Array(classOf[GroupNotFoundException], classOf[SectionNotFoundException]))
  @ResponseStatus(HttpStatus.NOT_FOUND)
  def handleNotFoundException: ModelAndView = new ModelAndView("errors/code404")
}