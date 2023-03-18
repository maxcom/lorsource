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
package ru.org.linux.group

import org.springframework.http.HttpStatus
import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.*
import org.springframework.web.servlet.ModelAndView
import org.springframework.web.servlet.view.RedirectView
import ru.org.linux.auth.{AccessViolationException, AuthUtil}
import ru.org.linux.section.{Section, SectionController, SectionService}
import ru.org.linux.site.Template
import ru.org.linux.tag.{TagPageController, TagService}
import ru.org.linux.topic.ArchiveDao
import ru.org.linux.util.ServletParameterBadValueException

import scala.concurrent.ExecutionContext.Implicits.global
import java.util
import java.util.concurrent.CompletionStage
import javax.servlet.http.{HttpServletRequest, HttpServletResponse}
import scala.compat.java8.FutureConverters.FutureOps
import scala.concurrent.Future
import scala.jdk.CollectionConverters.{ListHasAsScala, SeqHasAsJava}

object GroupController {
  private val MaxOffset = 300
}

@Controller
class GroupController(groupDao: GroupDao, archiveDao: ArchiveDao, sectionService: SectionService,
                      prepareService: GroupInfoPrepareService, groupPermissionService: GroupPermissionService,
                      groupListDao: GroupListDao, tagService: TagService) {
  @RequestMapping(Array("/group.jsp"))
  def topics(@RequestParam("group") groupId: Int,
             @RequestParam(value = "offset", required = false) offsetObject: Integer): ModelAndView = {
    val group = groupDao.getGroup(groupId)

    if (offsetObject != null) {
      new ModelAndView(new RedirectView(group.getUrl + "?offset=" + offsetObject))
    } else {
      new ModelAndView(new RedirectView(group.getUrl))
    }
  }

  @RequestMapping(Array("/group-lastmod.jsp"))
  def topicsLastmod(@RequestParam("group") groupId: Int,
                    @RequestParam(value = "offset", required = false) offsetObject: Integer): ModelAndView = {
    val group = groupDao.getGroup(groupId)

    if (offsetObject != null) {
      new ModelAndView(new RedirectView(group.getUrl + "?offset=" + offsetObject + "&lastmod=true"))
    } else {
      new ModelAndView(new RedirectView(group.getUrl + "?lastmod=true"))
    }
  }

  @RequestMapping(Array("/forum/{group}/{year:\\d+}/{month:\\d+}"))
  def forumArchive(@PathVariable("group") groupName: String,
                   @RequestParam(defaultValue = "0", value = "offset") offset: Int,
                   @PathVariable year: Int, @PathVariable month: Int,
                   request: HttpServletRequest, response: HttpServletResponse): CompletionStage[ModelAndView] =
    forum(groupName, offset, lastmod = false, request, response, year, month)

  @RequestMapping(Array("/forum/{group}"))
  def forum(@PathVariable("group") groupName: String, @RequestParam(defaultValue = "0", value = "offset") offset: Int,
            @RequestParam(defaultValue = "false") lastmod: Boolean, request: HttpServletRequest,
            response: HttpServletResponse): CompletionStage[ModelAndView] =
    forum(groupName, offset, lastmod, request, response, null, null)

  private def forum(groupName: String, offset: Int, lastmod: Boolean, request: HttpServletRequest,
                    response: HttpServletResponse, year: Integer, month: Integer): CompletionStage[ModelAndView] = {
    val deadline = TagPageController.Timeout.fromNow

    val section = sectionService.getSection(Section.SECTION_FORUM)
    val group = groupDao.getGroup(section, groupName)

    val activeTagsF = tagService.getActiveTopTags(section, Some(group), deadline)

    val params = new util.HashMap[String, AnyRef]

    val showDeleted = request.getParameter("deleted") != null

    params.put("showDeleted", Boolean.box(showDeleted))

    params.put("groupList", SectionController.groupsSorted(groupDao.getGroups(section).asScala).asJava)

    if (showDeleted && !("POST" == request.getMethod)) {
      return Future.successful(new ModelAndView(new RedirectView(group.getUrl))).toJava
    }

    val tmpl = Template.getTemplate

    if (showDeleted && !tmpl.isSessionAuthorized) {
      throw new AccessViolationException("Вы не авторизованы")
    }

    var firstPage = false

    if (offset != 0) {
      firstPage = false

      if (offset < 0) {
        throw new ServletParameterBadValueException("offset", "offset не может быть отрицательным")
      }

      if (year == null && offset > GroupController.MaxOffset) {
        return Future.successful(new ModelAndView(new RedirectView(group.getUrl + "archive"))).toJava
      }
    } else {
      firstPage = true
    }

    params.put("firstPage", Boolean.box(firstPage))
    params.put("offset", Integer.valueOf(offset))
    params.put("prevPage", Integer.valueOf(offset - tmpl.getProf.getTopics))
    params.put("nextPage", Integer.valueOf(offset + tmpl.getProf.getTopics))
    params.put("lastmod", Boolean.box(lastmod))

    val showIgnored = if (request.getParameter("showignored") != null) {
      "t" == request.getParameter("showignored")
    } else {
      false
    }

    params.put("showIgnored", Boolean.box(showIgnored))
    params.put("group", group)

    params.put("section", section)
    params.put("groupInfo", prepareService.prepareGroupInfo(group))

    if (year != null) {
      if (year < 1990 || year > 3000) {
        throw new ServletParameterBadValueException("year", "указан некорректный год")
      }

      if (month < 1 || month > 12) {
        throw new ServletParameterBadValueException("month", "указан некорректный месяц")
      }

      params.put("year", year)
      params.put("month", month)
      params.put("url", group.getUrl + year + '/' + month + '/')
    } else {
      params.put("url", group.getUrl)
    }

    val mainTopics = if (!lastmod) {
      groupListDao.getGroupListTopics(group.getId, AuthUtil.getCurrentUser, tmpl.getProf.getTopics, offset,
        tmpl.getProf.getMessages, showIgnored, showDeleted, year, month)
    } else {
      groupListDao.getGroupTrackerTopics(group.getId, AuthUtil.getCurrentUser, tmpl.getProf.getTopics, offset,
        tmpl.getProf.getMessages)
    }

    if (year == null && offset == 0 && !lastmod) {
      val stickyTopics = groupListDao.getGroupStickyTopics(group, tmpl.getProf.getMessages)

      params.put("topicsList", (stickyTopics.asScala.view ++ mainTopics.asScala).toSeq.asJava)
    } else {
      params.put("topicsList", mainTopics)
    }

    if (year != null) {
      params.put("hasNext",
        Boolean.box(offset + tmpl.getProf.getTopics < archiveDao.getArchiveCount(group.getId, year, month)))
    } else {
      params.put("hasNext",
        Boolean.box(offset < GroupController.MaxOffset && mainTopics.size == tmpl.getProf.getTopics))
    }

    params.put("addable", Boolean.box(groupPermissionService.isTopicPostingAllowed(group, AuthUtil.getCurrentUser)))

    response.setDateHeader("Expires", System.currentTimeMillis + 90 * 1000)

    activeTagsF.map { activeTags =>
      if (activeTags.nonEmpty) {
        params.put("activeTags", activeTags.asJava)
      }

      if (!tmpl.getProf.isOldTracker) {
        new ModelAndView("group-new", params)
      } else {
        new ModelAndView("group", params)
      }
    }.toJava
  }

  @ExceptionHandler(Array(classOf[GroupNotFoundException]))
  @ResponseStatus(HttpStatus.NOT_FOUND)
  def handleNotFoundException = new ModelAndView("errors/code404")
}