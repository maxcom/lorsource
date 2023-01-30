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

import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.{PathVariable, RequestMapping, RequestMethod, RequestParam}
import org.springframework.web.servlet.view.RedirectView
import org.springframework.web.servlet.{ModelAndView, View}
import org.springframework.web.util.{UriComponentsBuilder, UriTemplate}
import ru.org.linux.auth.AuthUtil
import ru.org.linux.section.{Section, SectionService}
import ru.org.linux.site.Template
import ru.org.linux.tag.{TagName, TagNotFoundException, TagService}
import ru.org.linux.user.UserTagService

import javax.servlet.http.HttpServletResponse

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
class TagTopicListController (
  userTagService: UserTagService,
  sectionService: SectionService,
  tagService: TagService,
  topicListService: TopicListService,
  prepareService: TopicPrepareService
) {
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
  def tagFeed(
               response: HttpServletResponse,
               @PathVariable tag: String,
               @RequestParam(value = "offset", defaultValue = "0") rawOffset: Int,
               @RequestParam(value = "section", defaultValue = "0") sectionId: Int
  ): ModelAndView = {
    TagName.checkTag(tag)

    tagService.getTagInfo(tag, skipZero = true) match {
      case Some(tagInfo) =>
        val section = if (sectionId != 0) {
          sectionService.idToSection.get(sectionId)
        } else {
          None
        }

        val modelAndView = new ModelAndView("tag-topics")

        section.foreach(s => modelAndView.addObject("section", s))
        TopicListController.setExpireHeaders(response, null, null)

        val title = getTitle(tag, section)

        modelAndView.addObject("navtitle", title)
        modelAndView.addObject("ptitle", title)

        val tmpl = Template.getTemplate

        val offset = topicListService.fixOffset(rawOffset)

        modelAndView.addObject("offsetNavigation", true)
        modelAndView.addObject("tag", tag)
        modelAndView.addObject("section", sectionId)
        modelAndView.addObject("offset", offset)
        modelAndView.addObject("sectionList", sectionService.getSectionList)

        if (tmpl.isSessionAuthorized) {
          modelAndView.addObject("isShowFavoriteTagButton", !userTagService.hasFavoriteTag(AuthUtil.getCurrentUser, tag))
          modelAndView.addObject("isShowUnFavoriteTagButton", userTagService.hasFavoriteTag(AuthUtil.getCurrentUser, tag))

          if (!tmpl.isModeratorSession) {
            modelAndView.addObject("isShowIgnoreTagButton", !userTagService.hasIgnoreTag(AuthUtil.getCurrentUser, tag))
            modelAndView.addObject("isShowUnIgnoreTagButton", userTagService.hasIgnoreTag(AuthUtil.getCurrentUser, tag))
          }
        }

        val topics = topicListService.getTopicsFeed(section.orNull, null, tag, offset, null, null, 20, AuthUtil.getCurrentUser)

        val preparedTopics =
          prepareService.prepareTopicsForUser(topics, AuthUtil.getCurrentUser, tmpl.getProf, loadUserpics = false)

        modelAndView.addObject("messages", preparedTopics)

        modelAndView.addObject("counter", tagInfo.topicCount)
        modelAndView.addObject("url", TagTopicListController.tagListUrl(tag))
        modelAndView.addObject("favsCount", userTagService.countFavs(tagInfo.id))

        if (offset < 200 && preparedTopics.size == 20) {
          modelAndView.addObject("nextLink", TagTopicListController.buildTagUri(tag, sectionId, offset + 20))
        }

        if (offset >= 20) {
          modelAndView.addObject("prevLink", TagTopicListController.buildTagUri(tag, sectionId, offset - 20))
        }

        modelAndView
      case None =>
        tagService.getTagBySynonym(tag).map { mainName =>
          new ModelAndView(new RedirectView(TagTopicListController.buildTagUri(mainName.name, sectionId, 0), false, false))
        }.getOrElse(throw new TagNotFoundException())
    }
  }

  @RequestMapping(
    value = Array("/view-news.jsp"),
    method = Array(RequestMethod.GET, RequestMethod.HEAD),
    params = Array("tag")
  )
  def tagFeedOld(@RequestParam tag: String): View =
    new RedirectView(TagTopicListController.tagListUrl(tag))
}
