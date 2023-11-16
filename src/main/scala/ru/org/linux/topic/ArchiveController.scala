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
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.servlet.ModelAndView
import ru.org.linux.auth.AuthUtil
import ru.org.linux.group.{GroupDao, GroupNotFoundException, GroupPermissionService}
import ru.org.linux.section.Section
import ru.org.linux.section.SectionService

@Controller
class ArchiveController(sectionService: SectionService, groupDao: GroupDao, archiveDao: ArchiveDao,
                        groupPermissionService: GroupPermissionService) {
  private def archiveList(sectionid: Int, groupName: Option[String] = None) = AuthUtil.AuthorizedOpt { currentUserOpt =>
    val mv = new ModelAndView("view-news-archive")

    val section = sectionService.getSection(sectionid)

    mv.getModel.put("section", section)

    val group = groupName.map(name => groupDao.getGroup(section, name))

    mv.getModel.put("group", group.orNull)

    val items = archiveDao.getArchiveStats(section, group.orNull)

    mv.getModel.put("items", items)

    val addUrl = group match {
      case Some(group) if groupPermissionService.isTopicPostingAllowed(group, currentUserOpt.map(_.user).orNull) =>
        AddTopicController.getAddUrl(group)
      case None if groupPermissionService.isTopicPostingAllowed(section, currentUserOpt.map(_.user)) =>
        AddTopicController.getAddUrl(section)
      case _ =>
        ""
    }

    mv.getModel.put("addUrl", addUrl)

    mv
  }

  @RequestMapping(path = Array("/gallery/archive"))
  def galleryArchive: ModelAndView = archiveList(Section.SECTION_GALLERY)

  @RequestMapping(path = Array("/news/archive"))
  def newsArchive: ModelAndView = archiveList(Section.SECTION_NEWS)

  @RequestMapping(path = Array("/polls/archive"))
  def pollsArchive: ModelAndView = archiveList(Section.SECTION_POLLS)

  @RequestMapping(path = Array("/articles/archive"))
  def articlesArchive: ModelAndView = archiveList(Section.SECTION_ARTICLES)

  @RequestMapping(path = Array("/forum/{group}/archive"))
  def forumArchive(@PathVariable group: String): ModelAndView = archiveList(Section.SECTION_FORUM, Some(group))

  @ExceptionHandler(Array(classOf[GroupNotFoundException]))
  @ResponseStatus(HttpStatus.NOT_FOUND)
  def handleNotFoundException = new ModelAndView("errors/code404")
}