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

import org.springframework.http.HttpStatus
import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.{ExceptionHandler, PathVariable, RequestAttribute, RequestMapping,
  ResponseStatus}
import org.springframework.web.servlet.ModelAndView
import ru.org.linux.auth.AuthUtil.MaybeAuthorized
import ru.org.linux.auth.IpBlockInfo
import ru.org.linux.group.{GroupNotFoundException, GroupService}
import ru.org.linux.rights.TopicPostingChecker
import ru.org.linux.section.{Section, SectionService}

@Controller
class ArchiveController(
    sectionService: SectionService,
    groupService: GroupService,
    archiveDao: ArchiveDao,
    topicPostingChecker: TopicPostingChecker,
    topicService: TopicService):
  private def archiveList(sectionid: Int, groupName: Option[String] = None, ipBlockInfo: IpBlockInfo) =
    MaybeAuthorized { implicit currentUserOpt =>
      val mv = new ModelAndView("view-news-archive")

      val section = sectionService.getSection(sectionid)

      mv.getModel.put("section", section)

      val group = groupName.map(name => groupService.getGroup(section, name))

      mv.getModel.put("group", group.orNull)

      val items = archiveDao.getArchiveStats(section, group)

      mv.getModel.put("items", items)

      val postingCheck =
        group match
          case Some(group) =>
            topicPostingChecker.checkTopicPosting(group, ipBlockInfo)
          case None =>
            topicPostingChecker.checkTopicPosting(section, ipBlockInfo)

      val addUrl =
        group match
          case Some(group) if postingCheck.permitted =>
            AddTopicController.getAddUrl(group)
          case None if postingCheck.permitted =>
            AddTopicController.getAddUrl(section)
          case _ =>
            ""

      mv.getModel.put("addUrl", addUrl)
      mv.getModel.put("addUrlReason", postingCheck.reason)

      if section.isPremoderated then
        mv.getModel.put("uncommitedCount", topicService.getUncommitedCount(section))

      mv
    }

  @RequestMapping(path = Array("/gallery/archive"))
  def galleryArchive(
      @RequestAttribute("ipBlockInfo")
      ipBlockInfo: IpBlockInfo): ModelAndView = archiveList(Section.Gallery, ipBlockInfo = ipBlockInfo)

  @RequestMapping(path = Array("/news/archive"))
  def newsArchive(
      @RequestAttribute("ipBlockInfo")
      ipBlockInfo: IpBlockInfo): ModelAndView = archiveList(Section.News, ipBlockInfo = ipBlockInfo)

  @RequestMapping(path = Array("/polls/archive"))
  def pollsArchive(
      @RequestAttribute("ipBlockInfo")
      ipBlockInfo: IpBlockInfo): ModelAndView = archiveList(Section.Polls, ipBlockInfo = ipBlockInfo)

  @RequestMapping(path = Array("/articles/archive"))
  def articlesArchive(
      @RequestAttribute("ipBlockInfo")
      ipBlockInfo: IpBlockInfo): ModelAndView = archiveList(Section.Articles, ipBlockInfo = ipBlockInfo)

  @RequestMapping(path = Array("/forum/{group}/archive"))
  def forumArchive(
      @PathVariable
      group: String,
      @RequestAttribute("ipBlockInfo")
      ipBlockInfo: IpBlockInfo): ModelAndView = archiveList(Section.Forum, Some(group), ipBlockInfo = ipBlockInfo)

  @ExceptionHandler(Array(classOf[GroupNotFoundException])) @ResponseStatus(HttpStatus.NOT_FOUND)
  def handleNotFoundException = new ModelAndView("errors/code404")
