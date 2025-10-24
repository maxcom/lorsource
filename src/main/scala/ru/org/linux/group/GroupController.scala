/*
 * Copyright 1998-2025 Linux.org.ru
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

import jakarta.servlet.http.HttpServletRequest
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.*
import org.springframework.web.servlet.view.RedirectView
import org.springframework.web.servlet.{ModelAndView, View}
import ru.org.linux.auth.AuthUtil.MaybeAuthorized
import ru.org.linux.auth.{AccessViolationException, AnySession}
import ru.org.linux.section.{Section, SectionController, SectionService}
import ru.org.linux.tag.{TagInfo, TagPageController, TagService}
import ru.org.linux.topic.{ArchiveDao, TagTopicListController, TopicPrepareService}
import ru.org.linux.util.ServletParameterBadValueException

import java.util
import java.util.concurrent.CompletionStage
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.jdk.CollectionConverters.{ListHasAsScala, SeqHasAsJava}
import scala.jdk.FutureConverters.FutureOps

private object GroupController {
  private val MaxOffset = 300
}

@Controller
class GroupController(groupDao: GroupDao, archiveDao: ArchiveDao, sectionService: SectionService,
                      prepareService: GroupInfoPrepareService, groupPermissionService: GroupPermissionService,
                      groupListDao: GroupListDao, tagService: TagService, topicPrepareService: TopicPrepareService) {
  @RequestMapping(path = Array("/group.jsp"))
  def topics(@RequestParam("group") groupId: Int,
             @RequestParam(value = "offset", required = false) offsetObject: Integer): View = {
    val group = groupDao.getGroup(groupId)

    if (offsetObject != null) {
      new RedirectView(group.getUrl + "?offset=" + offsetObject)
    } else {
      new RedirectView(group.getUrl)
    }
  }

  @RequestMapping(path = Array("/group-lastmod.jsp"))
  def topicsLastmod(@RequestParam("group") groupId: Int,
                    @RequestParam(value = "offset", required = false) offsetObject: Integer): View = {
    val group = groupDao.getGroup(groupId)

    if (offsetObject != null) {
      new RedirectView(s"${group.getUrl}?offset=$offsetObject&lastmod=true")
    } else {
      new RedirectView(s"${group.getUrl}?lastmod=true")
    }
  }

  @RequestMapping(path = Array("/forum/{group}/{year:\\d{4}}/{month:\\d+}"))
  def forumArchive(@PathVariable("group") groupName: String,
                   @RequestParam(defaultValue = "0", value = "offset") offset: Int,
                   @PathVariable year: Int, @PathVariable month: Int,
                   @RequestParam(value = "showignored", defaultValue = "false") showIgnored: Boolean): CompletionStage[ModelAndView] = MaybeAuthorized { implicit currentUserOpt =>
    val section = sectionService.getSection(Section.SECTION_FORUM)
    val group = groupDao.getGroup(section, groupName)

    if (year < 1990 || year > 3000) {
      throw new ServletParameterBadValueException("year", "указан некорректный год")
    }

    if (month < 1 || month > 12) {
      throw new ServletParameterBadValueException("month", "указан некорректный месяц")
    }

    forum(section, group, offset, lastmod = false, Some((year, month)), tagInfo = None,
      showDeleted = false, showIgnored = showIgnored)
  }

  private def isFirstPage(offset: Int): Boolean = {
    if (offset != 0) {
      if (offset < 0) {
        throw new ServletParameterBadValueException("offset", "offset не может быть отрицательным")
      }

      false
    } else {
      true
    }
  }

  @RequestMapping(path = Array("/forum/{group}"))
  def forum(@PathVariable("group") groupName: String, @RequestParam(defaultValue = "0", value = "offset") offset: Int,
            @RequestParam(defaultValue = "false") lastmod: Boolean, @RequestParam(required = false) tag: String,
            @RequestParam(defaultValue = "false") showDeleted: Boolean,
            @RequestParam(value = "showignored", defaultValue = "false") showIgnored: Boolean,
            request: HttpServletRequest): CompletionStage[ModelAndView] = MaybeAuthorized { implicit currentUserOpt =>
    val section = sectionService.getSection(Section.SECTION_FORUM)
    val group = groupDao.getGroup(section, groupName)

    if (showDeleted && !currentUserOpt.authorized) {
      throw new AccessViolationException("Вы не авторизованы")
    }

    if (showDeleted && !("POST" == request.getMethod)) {
      Future.successful(new ModelAndView(new RedirectView(group.getUrl))).asJava
    } else if (!isFirstPage(offset) && offset > GroupController.MaxOffset) {
      Future.successful(new ModelAndView(new RedirectView(s"${group.getUrl}archive"))).asJava
    } else {
      val tagOpt = Option(tag)
      val tagInfo: Option[TagInfo] = tagOpt.flatMap(v => tagService.getTagInfo(v, skipZero = true))

      if (tagOpt.isDefined && tagInfo.isEmpty) {
        Future.successful(new ModelAndView("errors/code404")).asJava
      } else {
        forum(section, group, offset, lastmod, None, tagInfo, showDeleted = showDeleted,
          showIgnored = showIgnored)
      }
    }
  }

  private def forum(section: Section, group: Group, offset: Int, lastmod: Boolean,
                    yearMonth: Option[(Int, Int)], tagInfo: Option[TagInfo], showDeleted: Boolean,
                    showIgnored: Boolean)(implicit currentUser: AnySession): CompletionStage[ModelAndView] = {
    val deadline = TagPageController.Timeout.fromNow

    val firstPage = isFirstPage(offset)

    val activeTagsF = tagService.getActiveTopTags(section, Some(group), None, deadline).map { tags =>
      tags.map(tag => tag.copy(url = Some(TagTopicListController.tagListUrl(tag.name, section))))
    }

    val params = new util.HashMap[String, AnyRef]

    params.put("showDeleted", Boolean.box(showDeleted))

    params.put("groupList", SectionController.groupsSorted(groupDao.getGroups(section).asScala).asJava)

    params.put("firstPage", Boolean.box(firstPage))
    params.put("offset", Integer.valueOf(offset))

    params.put("prevPage", Integer.valueOf(offset - currentUser.profile.topics))
    params.put("nextPage", Integer.valueOf(offset + currentUser.profile.topics))
    params.put("lastmod", Boolean.box(lastmod))

    params.put("showIgnored", Boolean.box(showIgnored))
    params.put("group", group)

    params.put("section", section)
    params.put("groupInfo", prepareService.prepareGroupInfo(group))

    val tagId = tagInfo.map(_.id)

    tagInfo.foreach { t =>
      params.put("tag", TagService.tagRef(t, 0))
      params.put("tagTitle", t.name.capitalize)
    }

    val mainTopics = (if (!lastmod) {
      groupListDao.getGroupListTopics(group.id, offset, showIgnored, showDeleted, yearMonth, tagId)
    } else {
      groupListDao.getGroupTrackerTopics(group.id, offset, tagId)
    }).map(topicPrepareService.prepareListItem)

    yearMonth match {
      case Some((year, month)) =>
        params.put("year", Integer.valueOf(year))
        params.put("month", Integer.valueOf(month))
        params.put("url", s"${group.getUrl}$year/$month/")

        params.put("hasNext",
          Boolean.box(offset + currentUser.profile.topics < archiveDao.getArchiveCount(group.id, year, month)))
      case None =>
        params.put("url", group.getUrl)
        params.put("hasNext",
          Boolean.box(offset < GroupController.MaxOffset && mainTopics.size == currentUser.profile.topics))
    }

    if (yearMonth.isEmpty && offset == 0 && !lastmod) {
      val stickyTopics = groupListDao.getGroupStickyTopics(group, tagId).map(topicPrepareService.prepareListItem)

      params.put("topicsList", (stickyTopics.view ++ mainTopics).toSeq.asJava)
    } else {
      params.put("topicsList", mainTopics.asJava)
    }

    params.put("addable", Boolean.box(groupPermissionService.isTopicPostingAllowed(group)))

    activeTagsF.map { activeTags =>
      if (activeTags.nonEmpty) {
        params.put("activeTags", activeTags.asJava)
      }

      if (!currentUser.profile.oldTracker) {
        new ModelAndView("group-new", params)
      } else {
        new ModelAndView("group", params)
      }
    }.asJava
  }

  @ExceptionHandler(Array(classOf[GroupNotFoundException]))
  @ResponseStatus(HttpStatus.NOT_FOUND)
  def handleNotFoundException = new ModelAndView("errors/code404")
}