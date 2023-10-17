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
import org.springframework.web.bind.annotation.{ExceptionHandler, PathVariable, RequestMapping, RequestMethod, RequestParam, ResponseStatus}
import org.springframework.web.servlet.view.RedirectView
import org.springframework.web.servlet.{ModelAndView, View}
import org.springframework.web.util.{UriComponentsBuilder, UriTemplate}
import ru.org.linux.auth.AuthUtil.AuthorizedOpt
import ru.org.linux.group.GroupListDao
import ru.org.linux.section.{Section, SectionNotFoundException, SectionService}
import ru.org.linux.site.Template
import ru.org.linux.tag.{TagName, TagNotFoundException, TagPageController, TagService}
import ru.org.linux.user.UserTagService

import scala.concurrent.ExecutionContext.Implicits.global
import scala.compat.java8.FutureConverters.*
import java.util.concurrent.CompletionStage
import scala.concurrent.Future
import scala.jdk.CollectionConverters.{ListHasAsScala, SeqHasAsJava}
import scala.jdk.OptionConverters.RichOption

@Controller
object TagTopicListController {
  private val TagUriTemplate = new UriTemplate("/tag/{tag}")
  private val TagUriSectionTemplate = new UriTemplate("/tag/{tag}?section={section}")
  private val TagsUriTemplate = new UriTemplate("/tags/{tag}")

  def tagListUrl(tag: String): String = TagUriTemplate.expand(tag).toString

  def tagListUrl(tag: String, section: Section): String =
    TagUriSectionTemplate.expand(tag, Integer.valueOf(section.getId)).toString

  def tagsUrl(letter: Char): String = TagsUriTemplate.expand(letter.toString).toString

  private def buildTagUri(tag: String, section: Int, offset: Int) = {
    val builder: UriComponentsBuilder = UriComponentsBuilder.fromUri(TagUriTemplate.expand(tag))

    if (section != 0) {
      builder.queryParam("section", Integer.valueOf(section))
    }

    if (offset != 0) {
      builder.queryParam("offset", Integer.valueOf(offset))
    }

    builder.build.toUriString
  }
}

@Controller
class TagTopicListController(userTagService: UserTagService, sectionService: SectionService, tagService: TagService,
                             topicListService: TopicListService, prepareService: TopicPrepareService,
                             topicTagDao: TopicTagDao, groupListDao: GroupListDao) {

  private def getTitle(tag: String, section: Section) = s"${tag.capitalize} (${section.getName})"

  @RequestMapping(
    value = Array("/tag/{tag}"),
    method = Array(RequestMethod.GET, RequestMethod.HEAD),
    params = Array("section"))
  def tagFeed(@PathVariable tag: String,
               @RequestParam(value = "offset", defaultValue = "0") rawOffset: Int,
               @RequestParam(value = "section", defaultValue = "0") sectionId: Int
  ): CompletionStage[ModelAndView] = AuthorizedOpt { currentUserOpt =>
    TagName.checkTag(tag)

    val deadline = TagPageController.Timeout.fromNow

    val section = sectionService.getSection(sectionId)

    val countF = tagService.countTagTopics(tag = tag, section = Some(section), deadline = deadline)

    (tagService.getTagInfo(tag, skipZero = true) match {
      case Some(tagInfo) =>
        val forumMode = section.getId == Section.SECTION_FORUM  && currentUserOpt.exists(_.user.isAdministrator)

        // todo old/new
        // todo tag-topics-forum
        val modelAndView = new ModelAndView(if (forumMode) "tracker-new" else "tag-topics")

        modelAndView.addObject("section", section)

        modelAndView.addObject("tagTitle", tag.capitalize)
        modelAndView.addObject("ptitle", getTitle(tag, section))

        val offset = TopicListService.fixOffset(rawOffset)

        modelAndView.addObject("offsetNavigation", true)
        modelAndView.addObject("tag", tag)
        modelAndView.addObject("section", sectionId)
        modelAndView.addObject("offset", offset)

        val sections = topicTagDao.getTagSections(tagInfo.id).map(sectionService.idToSection)

        modelAndView.addObject("sectionList", sections.asJava)

        currentUserOpt.foreach { currentUser =>
          modelAndView.addObject("showFavoriteTagButton", !userTagService.hasFavoriteTag(currentUser.user, tag))
          modelAndView.addObject("showUnFavoriteTagButton", userTagService.hasFavoriteTag(currentUser.user, tag))

          if (!currentUser.moderator) {
            modelAndView.addObject("showIgnoreTagButton", !userTagService.hasIgnoreTag(currentUser.user, tag))
            modelAndView.addObject("showUnIgnoreTagButton", userTagService.hasIgnoreTag(currentUser.user, tag))
          }
        }

        val prof = Template.getTemplate.getProf

        val (preparedTopics, pageSize) = if (forumMode) {
          (groupListDao.getSectionListTopics(section, currentUserOpt.map(_.user).toJava,
            prof.getTopics, offset, prof.getMessages, tagInfo.id), prof.getTopics)
        } else {
          val topics = topicListService.getTopicsFeed(section, None, Some(tag), offset, None,
            20, currentUserOpt.map(_.user), noTalks = false, tech = false)

          (prepareService.prepareTopicsForUser(topics, currentUserOpt.map(_.user), prof, loadUserpics = false), 20)
        }

        modelAndView.addObject("messages", preparedTopics)

        modelAndView.addObject("url", TagTopicListController.tagListUrl(tag))
        modelAndView.addObject("favsCount", userTagService.countFavs(tagInfo.id))
        modelAndView.addObject("ignoreCount", userTagService.countIgnore(tagInfo.id))

        if (offset < TopicListService.MaxOffset && preparedTopics.size == pageSize) {
          modelAndView.addObject("nextLink", TagTopicListController.buildTagUri(tag, sectionId, offset + pageSize))
        }

        if (offset > pageSize) {
          modelAndView.addObject("prevLink", TagTopicListController.buildTagUri(tag, sectionId, offset - pageSize))
        }

        if (preparedTopics.isEmpty) {
          Future.successful(new ModelAndView("errors/code404"))
        } else {
          countF.map { count =>
            modelAndView.addObject("counter", count.getOrElse(0))
            modelAndView
          }
        }
      case None =>
        tagService.getTagBySynonym(tag).map { mainName =>
          Future.successful(new ModelAndView(new RedirectView(TagTopicListController.buildTagUri(mainName.name, sectionId, 0), false, false)))
        }.getOrElse(throw new TagNotFoundException())
    }).toJava
  }

  @RequestMapping(
    value = Array("/view-news.jsp"),
    method = Array(RequestMethod.GET, RequestMethod.HEAD),
    params = Array("tag"))
  def tagFeedOld(@RequestParam tag: String): View = new RedirectView(TagTopicListController.tagListUrl(tag))

  @ExceptionHandler(Array(classOf[SectionNotFoundException]))
  @ResponseStatus(HttpStatus.NOT_FOUND)
  def handleNotFoundException = new ModelAndView("errors/code404")
}
