/*
 * Copyright 1998-2015 Linux.org.ru
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
package ru.org.linux.tag

import java.util.Map
import javax.annotation.Nonnull
import javax.servlet.http.HttpServletRequest

import com.google.common.collect.{ImmutableMap, Multimaps}
import com.typesafe.scalalogging.StrictLogging
import org.apache.commons.lang3.text.WordUtils
import org.elasticsearch.ElasticsearchException
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.{PathVariable, RequestMapping, RequestMethod}
import org.springframework.web.servlet.ModelAndView
import ru.org.linux.gallery.ImageService
import ru.org.linux.group.GroupDao
import ru.org.linux.section.{Section, SectionService}
import ru.org.linux.site.Template
import ru.org.linux.topic._
import ru.org.linux.user.UserTagService

import scala.collection.JavaConverters._
import scala.concurrent._
import scala.concurrent.duration._

object TagPageController {
  val TotalNewsCount = 21
  val ForumTopicCount = 20
  val GalleryCount = 3
}

@Controller
@RequestMapping(value = Array("/tag/{tag}"), params = Array("!section"))
class TagPageController @Autowired()
(tagService: TagService, prepareService: TopicPrepareService, topicListService: TopicListService,
 sectionService: SectionService, groupDao: GroupDao, userTagService: UserTagService, imageService: ImageService) extends StrictLogging {

  @RequestMapping(method = Array(RequestMethod.GET, RequestMethod.HEAD))
  def tagPage(request: HttpServletRequest, @PathVariable tag: String): ModelAndView = {
    val tmpl = Template.getTemplate(request)

    if (!TagName.isGoodTag(tag)) {
      throw new TagNotFoundException
    }

    val futureCount = tagService.countTagTopics(tag)

    val mv = new ModelAndView("tag-page")
    mv.addObject("tag", tag)
    mv.addObject("title", WordUtils.capitalize(tag))

    val tagInfo = tagService.getTagInfo(tag, skipZero = true)

    if (tmpl.isSessionAuthorized) {
      mv.addObject("showFavoriteTagButton", !userTagService.hasFavoriteTag(tmpl.getCurrentUser, tag))
      mv.addObject("showUnFavoriteTagButton", userTagService.hasFavoriteTag(tmpl.getCurrentUser, tag))
    }

    val tagId = tagInfo.id

    mv.addObject("favsCount", userTagService.countFavs(tagId))

    val relatedTags = tagService.getRelatedTags(tagId)
    if (relatedTags.size > 1) {
      mv.addObject("relatedTags", relatedTags.asJava)
    }

    mv.addAllObjects(getNewsSection(request, tag))
    mv.addAllObjects(getGallerySection(tag, tagId, tmpl))
    mv.addAllObjects(getForumSection(tag, tagId))

    try {
      mv.addObject("counter", Await.result(futureCount, 300 millis))
    } catch {
      case ex: ElasticsearchException ⇒
        logger.warn("Unable to count tag topics", ex)
        mv.addObject("counter", tagInfo.topicCount)
      case ex: TimeoutException ⇒
        logger.warn(s"Tag topics count timed out (${ex.getMessage})")
        mv.addObject("counter", tagInfo.topicCount)
    }

    mv
  }

  private def getNewsSection(request: HttpServletRequest, tag: String): Map[String, AnyRef] = {
    val tmpl = Template.getTemplate(request)
    val newsSection = sectionService.getSection(Section.SECTION_NEWS)
    val newsTopics = topicListService.getTopicsFeed(newsSection, null, tag, 0, null, null, TagPageController.TotalNewsCount)
    val (fullNewsTopics, briefNewsTopics) = newsTopics.asScala.splitAt(1)
    val fullNews = prepareService.prepareMessagesForUser(fullNewsTopics.asJava, request.isSecure, tmpl.getCurrentUser, tmpl.getProf, false)

    val briefNewsByDate = TopicListTools.datePartition(briefNewsTopics)

    val out = ImmutableMap.builder[String, AnyRef]
    out.put("addNews", AddTopicController.getAddUrl(newsSection, tag))

    if (newsTopics.size == TagPageController.TotalNewsCount) {
      out.put("moreNews", TagTopicListController.tagListUrl(tag, newsSection))
    }

    out.put("fullNews", fullNews)

    out.put("briefNews", TopicListTools.split(Multimaps.transformValues(briefNewsByDate,
      new com.google.common.base.Function[Topic, BriefTopicRef]() {
        override def apply(input: Topic): BriefTopicRef = {
          BriefTopicRef.apply(input.getLink, input.getTitle, input.getCommentCount)
        }
      })))

    out.build
  }

  private def getGallerySection(tag: String, tagId: Int, tmpl: Template): Map[String, AnyRef] = {
    val list = imageService.prepareGalleryItem(imageService.getGalleryItems(TagPageController.GalleryCount, tagId))
    val out = ImmutableMap.builder[String, AnyRef]
    val section = sectionService.getSection(Section.SECTION_GALLERY)

    if (tmpl.isSessionAuthorized) {
      out.put("addGallery", AddTopicController.getAddUrl(section, tag))
    }

    if (list.size == TagPageController.GalleryCount) {
      out.put("moreGallery", TagTopicListController.tagListUrl(tag, section))
    }

    out.put("gallery", list)

    out.build
  }

  private def getForumSection(@Nonnull tag: String, tagId: Int): ImmutableMap[String, AnyRef] = {
    val forumSection = sectionService.getSection(Section.SECTION_FORUM)

    val topicListDto = new TopicListDto
    topicListDto.setSection(forumSection.getId)
    topicListDto.setCommitMode(TopicListDao.CommitMode.POSTMODERATED_ONLY)
    topicListDto.setTag(tagId)
    topicListDto.setLimit(TagPageController.ForumTopicCount)

    val forumTopics = topicListService.getTopics(topicListDto)
    val topicByDate = TopicListTools.datePartition(forumTopics.asScala)

    val out = ImmutableMap.builder[String, AnyRef]

    if (forumTopics.size == TagPageController.ForumTopicCount) {
      out.put("moreForum", TagTopicListController.tagListUrl(tag, forumSection))
    }

    out.put("addForum", AddTopicController.getAddUrl(forumSection, tag))

    out.put("forum", TopicListTools.split(Multimaps.transformValues(topicByDate,
      new com.google.common.base.Function[Topic, BriefTopicRef]() {
        override def apply(input: Topic): BriefTopicRef = {
          BriefTopicRef.apply(input.getLink, input.getTitle, input.getCommentCount, groupDao.getGroup(input.getGroupId).getTitle)
        }
      }
      )))

    out.build
  }
}