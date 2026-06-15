/*
 * Copyright 1998-2026 Linux.org.ru
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

import jakarta.servlet.http.HttpServletRequest
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.*
import org.springframework.web.servlet.ModelAndView
import ru.org.linux.auth.AuthUtil.MaybeAuthorized
import ru.org.linux.group.GroupPermissionService
import ru.org.linux.rights.TopicPostingChecker
import ru.org.linux.section.{Section, SectionNotFoundException, SectionService}

import java.util.{Calendar, Date}
import scala.jdk.CollectionConverters.SeqHasAsJava

@Controller @RequestMapping(value = Array("/view-all.jsp"), method = Array(RequestMethod.GET, RequestMethod.HEAD))
class UncommitedTopicsController(
    sectionService: SectionService,
    topicListService: TopicListService,
    prepareService: TopicPrepareService,
    topicPostingChecker: TopicPostingChecker,
    topicService: TopicService):
  @RequestMapping
  def viewAll(
      @RequestParam(value = "section", required = false, defaultValue = "0")
      sectionId: Int,
      request: HttpServletRequest): ModelAndView =
    MaybeAuthorized { implicit session =>
      val modelAndView = new ModelAndView("view-all")

      val section: Option[Section] =
        if sectionId != 0 then
          Some(sectionService.getSection(sectionId))
        else
          None

      section.foreach { section =>
        modelAndView.addObject("section", section)

        val postingCheck = topicPostingChecker.checkTopicPosting(section, request.getRemoteAddr)
        
        if postingCheck.permitted then
          modelAndView.addObject("addlink", AddTopicController.getAddUrl(section))
        else
          modelAndView.addObject("addlinkReason", postingCheck.reason)
      }

      if section.isEmpty then
        modelAndView.addObject("addlink", "/add-section.jsp")

      val title = section.map(_.uncommitedName).getOrElse("Просмотр неподтверждённых сообщений")

      modelAndView.addObject("title", title)

      val calendar = Calendar.getInstance
      calendar.setTime(new Date)
      calendar.add(Calendar.MONTH, -3)

      val messages = topicListService.getUncommitedTopic(section, calendar.getTime)

      val topics = prepareService.prepareTopics(messages, loadUserpics = false)

      modelAndView.addObject("messages", topics.asJava)

      val deleted = topicListService.getDeletedTopics(sectionId, skipBadReason = !session.moderator)

      modelAndView.addObject("deletedTopics", deleted.asJava)

      if section.isEmpty then
        val uncommitedCounts = topicService.getUncommitedCounts
        val uncommited = uncommitedCounts.map(_._2).sum

        modelAndView.getModel.put("uncommited", Int.box(uncommited))
        modelAndView.getModel.put("uncommitedCounts", uncommitedCounts.asJava)
      else
        modelAndView.getModel.put("uncommited", Int.box(topicService.getUncommitedCount(section.get)))

      modelAndView
    }

  @ExceptionHandler(Array(classOf[SectionNotFoundException])) @ResponseStatus(HttpStatus.NOT_FOUND)
  def handleNotFoundException = new ModelAndView("errors/code404")
