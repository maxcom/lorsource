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
import ru.org.linux.section.{Section, SectionNotFoundException, SectionService}
import ru.org.linux.site.Template
import ru.org.linux.tag.{TagName, TagNotFoundException, TagService}
import ru.org.linux.user.UserTagService

import scala.jdk.CollectionConverters.SeqHasAsJava
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
class TagTopicListController (userTagService: UserTagService, sectionService: SectionService, tagService: TagService,
    topicListService: TopicListService, prepareService: TopicPrepareService, topicTagDao: TopicTagDao) {

  private def getTitle(tag: String, section: Option[Section]) = {
    section match {
      case None    => tag.capitalize
      case Some(s) => s"${tag.capitalize} (${s.getName})"
    }
  }

  @RequestMapping(
    value = Array("/tag/{tag}"),
    method = Array(RequestMethod.GET, RequestMethod.HEAD),
    params = Array("section"))
  def tagFeed(@PathVariable tag: String,
               @RequestParam(value = "offset", defaultValue = "0") rawOffset: Int,
               @RequestParam(value = "section", defaultValue = "0") sectionId: Int
  ): ModelAndView = AuthorizedOpt { currentUserOpt =>
    TagName.checkTag(tag)

    tagService.getTagInfo(tag, skipZero = true) match {
      case Some(tagInfo) =>
        val section = if (sectionId != 0) {
          Some(sectionService.idToSection.getOrElse(sectionId, throw new SectionNotFoundException()))
        } else {
          None
        }

        val modelAndView = new ModelAndView("tag-topics")

        section.foreach(s => modelAndView.addObject("section", s))

        modelAndView.addObject("navtitle", getTitle(tag, None))
        modelAndView.addObject("ptitle", getTitle(tag, section))

        val offset = TopicListService.fixOffset(rawOffset)

        modelAndView.addObject("offsetNavigation", true)
        modelAndView.addObject("tag", tag)
        modelAndView.addObject("section", sectionId)
        modelAndView.addObject("offset", offset)

        val sectionIds = topicTagDao.getTagSectoins(tagInfo.id).toSet
        val sections = sectionService.sections.filter(s => sectionIds.contains(s.getId))

        modelAndView.addObject("sectionList", sections.asJava)

        currentUserOpt.foreach { currentUser =>
          modelAndView.addObject("showFavoriteTagButton", !userTagService.hasFavoriteTag(currentUser.user, tag))
          modelAndView.addObject("showUnFavoriteTagButton", userTagService.hasFavoriteTag(currentUser.user, tag))

          if (!currentUser.moderator) {
            modelAndView.addObject("showIgnoreTagButton", !userTagService.hasIgnoreTag(currentUser.user, tag))
            modelAndView.addObject("showUnIgnoreTagButton", userTagService.hasIgnoreTag(currentUser.user, tag))
          }
        }

        val topics = topicListService.getTopicsFeed(section.orNull, null, tag, offset, None.toJava, None.toJava,
          20, currentUserOpt.map(_.user).orNull, false, false)

        val tmpl = Template.getTemplate

        val preparedTopics = prepareService.prepareTopicsForUser(topics, currentUserOpt.map(_.user).orNull,
          tmpl.getProf, loadUserpics = false)

        modelAndView.addObject("messages", preparedTopics)

        modelAndView.addObject("counter", tagInfo.topicCount)
        modelAndView.addObject("url", TagTopicListController.tagListUrl(tag))
        modelAndView.addObject("favsCount", userTagService.countFavs(tagInfo.id))
        modelAndView.addObject("ignoreCount", userTagService.countIgnore(tagInfo.id))

        if (offset < 200 && preparedTopics.size == 20) {
          modelAndView.addObject("nextLink", TagTopicListController.buildTagUri(tag, sectionId, offset + 20))
        }

        if (offset >= 20) {
          modelAndView.addObject("prevLink", TagTopicListController.buildTagUri(tag, sectionId, offset - 20))
        }

        if (topics.isEmpty) {
          new ModelAndView("errors/code404")
        } else {
          modelAndView
        }
      case None =>
        tagService.getTagBySynonym(tag).map { mainName =>
          new ModelAndView(new RedirectView(TagTopicListController.buildTagUri(mainName.name, sectionId, 0), false, false))
        }.getOrElse(throw new TagNotFoundException())
    }
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
