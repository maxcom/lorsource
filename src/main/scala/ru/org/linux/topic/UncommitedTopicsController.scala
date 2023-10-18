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

import org.springframework.http.HttpStatus
import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.*
import org.springframework.web.servlet.ModelAndView
import ru.org.linux.auth.AuthUtil.AuthorizedOpt
import ru.org.linux.group.GroupPermissionService
import ru.org.linux.section.{Section, SectionNotFoundException, SectionService}
import ru.org.linux.site.Template

import java.util.{Calendar, Date}
import scala.jdk.CollectionConverters.SeqHasAsJava

@Controller
@RequestMapping(value = Array("/view-all.jsp"), method = Array(RequestMethod.GET, RequestMethod.HEAD))
class UncommitedTopicsController(sectionService: SectionService, topicListService: TopicListService,
                                 prepareService: TopicPrepareService, groupPermissionService: GroupPermissionService) {
  @RequestMapping
  def viewAll(@RequestParam(value = "section", required = false, defaultValue = "0") sectionId: Int): ModelAndView = AuthorizedOpt { currentUserOpt =>
    val modelAndView = new ModelAndView("view-all")

    val section: Option[Section] = if (sectionId != 0) {
      Some(sectionService.getSection(sectionId))
    } else {
      None
    }

    section.foreach { section =>
      modelAndView.addObject("section", section)

      if (groupPermissionService.isTopicPostingAllowed(section, currentUserOpt.map(_.user))) {
        modelAndView.addObject("addlink", AddTopicController.getAddUrl(section))
      }
    }

    val title = section.map { section =>
      section.getId match {
        case Section.SECTION_NEWS => "Неподтвержденные новости"
        case Section.SECTION_POLLS => "Неподтвержденные опросы"
        case Section.SECTION_GALLERY => "Неподтвержденные изображения"
        case _ => "Неподтвержденные: " + section.getName
      }
    }.getOrElse("Просмотр неподтвержденных сообщений")

    modelAndView.addObject("title", title)

    val calendar = Calendar.getInstance
    calendar.setTime(new Date)
    calendar.add(Calendar.MONTH, -3)

    val includeAnonymous = currentUserOpt.exists(u => u.moderator || u.corrector)

    val messages = topicListService.getUncommitedTopic(section, calendar.getTime, includeAnonymous)

    val tmpl = Template.getTemplate

    val topics = prepareService.prepareTopicsForUser(messages, currentUserOpt.map(_.user), tmpl.getProf, loadUserpics = false)

    modelAndView.addObject("messages", topics)

    val deleted = topicListService.getDeletedTopics(sectionId, !tmpl.isModeratorSession, includeAnonymous)

    modelAndView.addObject("deletedTopics", deleted.asJava)
    modelAndView.addObject("sections", sectionService.sections.asJava)

    modelAndView
  }

  @ExceptionHandler(Array(classOf[SectionNotFoundException]))
  @ResponseStatus(HttpStatus.NOT_FOUND)
  def handleNotFoundException = new ModelAndView("errors/code404")
}