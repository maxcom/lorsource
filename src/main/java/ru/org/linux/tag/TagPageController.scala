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

import javax.annotation.Nonnull
import javax.servlet.http.HttpServletRequest

import akka.actor.ActorSystem
import com.typesafe.scalalogging.StrictLogging
import org.apache.commons.lang3.text.WordUtils
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.{PathVariable, RequestMapping, RequestMethod}
import org.springframework.web.context.request.async.DeferredResult
import org.springframework.web.servlet.ModelAndView
import ru.org.linux.gallery.ImageService
import ru.org.linux.group.GroupDao
import ru.org.linux.section.{Section, SectionService}
import ru.org.linux.site.Template
import ru.org.linux.topic._
import ru.org.linux.user.UserTagService
import ru.org.linux.util.RichFuture._

import scala.collection.JavaConverters._
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent._
import scala.concurrent.duration._

object TagPageController {
  val TotalNewsCount = 21
  val ForumTopicCount = 20
  val GalleryCount = 3

  val Timeout = 500 millis
}

@Controller
@RequestMapping(value = Array("/tag/{tag}"), params = Array("!section"))
class TagPageController @Autowired()
(tagService: TagService, prepareService: TopicPrepareService, topicListService: TopicListService,
 sectionService: SectionService, groupDao: GroupDao, userTagService: UserTagService, imageService: ImageService,
 actorSystem: ActorSystem) extends StrictLogging {

  private implicit val akka = actorSystem

  @RequestMapping(method = Array(RequestMethod.GET, RequestMethod.HEAD))
  def tagPage(request: HttpServletRequest, @PathVariable tag: String): DeferredResult[ModelAndView] = {
    val deadline = TagPageController.Timeout.fromNow

    val tmpl = Template.getTemplate(request)

    if (!TagName.isGoodTag(tag)) {
      throw new TagNotFoundException
    }

    val countF = tagService.countTagTopics(tag)

    val relatedF = {
      tagService.getRelatedTags(tag) map { relatedTags ⇒
        if (relatedTags.nonEmpty) {
          Some("relatedTags" -> relatedTags.asJava)
        } else {
          None
        }
      }
    }

    val favs = if (tmpl.isSessionAuthorized) {
      Seq("showFavoriteTagButton" -> !userTagService.hasFavoriteTag(tmpl.getCurrentUser, tag),
        "showUnFavoriteTagButton" -> userTagService.hasFavoriteTag(tmpl.getCurrentUser, tag),
        "showIgnoreTagButton" -> (!tmpl.isModeratorSession && !userTagService.hasIgnoreTag(tmpl.getCurrentUser, tag)),
	      "showUnIgnoreTagButton" -> (!tmpl.isModeratorSession && userTagService.hasIgnoreTag(tmpl.getCurrentUser, tag)))
    } else {
      Seq.empty
    }

    val tagInfo = tagService.getTagInfo(tag, skipZero = true)

    val sections = getNewsSection(request, tag) ++ getGallerySection(tag, tagInfo.id, tmpl) ++ getForumSection(tag, tagInfo.id)

    val model = Map(
      "tag" -> tag,
      "title" -> WordUtils.capitalize(tag),
      "favsCount" -> userTagService.countFavs(tagInfo.id),
      "ignoreCount" -> userTagService.countIgnore(tagInfo.id)
    ) ++ sections ++ favs

    val safeRelatedF = relatedF withTimeout deadline.timeLeft recover {
      case ex: TimeoutException ⇒
        logger.warn(s"Tag related search timed out (${ex.getMessage})")
        None
      case ex ⇒
        logger.warn("Unable to find related tags", ex)
        None
    }

    val safeCountF = countF withTimeout deadline.timeLeft recover {
      case ex: TimeoutException ⇒
        logger.warn(s"Tag topics count timed out (${ex.getMessage})")
        tagInfo.topicCount.toLong
      case ex ⇒
        logger.warn("Unable to count tag topics", ex)
        tagInfo.topicCount.toLong
    }

    (for {
      counter <- safeCountF
      related <- safeRelatedF
    } yield {
      new ModelAndView("tag-page", (model + ("counter" -> counter) ++ related).asJava)
    }) toDeferredResult
  }

  private def getNewsSection(request: HttpServletRequest, tag: String) = {
    val tmpl = Template.getTemplate(request)
    val newsSection = sectionService.getSection(Section.SECTION_NEWS)
    val newsTopics = topicListService.getTopicsFeed(newsSection, null, tag, 0, null, null, TagPageController.TotalNewsCount)
    val (fullNewsTopics, briefNewsTopics) = newsTopics.asScala.splitAt(1)
    val fullNews = prepareService.prepareMessagesForUser(fullNewsTopics.asJava, request.isSecure, tmpl.getCurrentUser, tmpl.getProf, false)

    val briefNewsByDate = TopicListTools.datePartition(briefNewsTopics)

    val more = if (newsTopics.size == TagPageController.TotalNewsCount) {
      Some("moreNews" -> TagTopicListController.tagListUrl(tag, newsSection))
    } else {
      None
    }

    Map(
      "fullNews" -> fullNews,
      "addNews" -> AddTopicController.getAddUrl(newsSection, tag),
      "briefNews" -> TopicListTools.split(briefNewsByDate.map(p ⇒ p._1 -> BriefTopicRef.fromTopicNoGroup(p._2)))
    ) ++ more
  }

  private def getGallerySection(tag: String, tagId: Int, tmpl: Template) = {
    val list = imageService.prepareGalleryItem(imageService.getGalleryItems(TagPageController.GalleryCount, tagId))
    val section = sectionService.getSection(Section.SECTION_GALLERY)

    val add = if (tmpl.isSessionAuthorized) {
      Some("addGallery" -> AddTopicController.getAddUrl(section, tag))
    } else {
      None
    }

    val more = if (list.size == TagPageController.GalleryCount) {
      Some("moreGallery" -> TagTopicListController.tagListUrl(tag, section))
    } else {
      None
    }

    Map(
      "gallery" -> list
    ) ++ add ++ more
  }

  private def getForumSection(@Nonnull tag: String, tagId: Int) = {
    val forumSection = sectionService.getSection(Section.SECTION_FORUM)

    val topicListDto = new TopicListDto
    topicListDto.setSection(forumSection.getId)
    topicListDto.setCommitMode(TopicListDao.CommitMode.POSTMODERATED_ONLY)
    topicListDto.setTag(tagId)
    topicListDto.setLimit(TagPageController.ForumTopicCount)

    val forumTopics = topicListService.getTopics(topicListDto)
    val topicByDate = TopicListTools.datePartition(forumTopics.asScala)

    val more = if (forumTopics.size == TagPageController.ForumTopicCount) {
      Some("moreForum" -> TagTopicListController.tagListUrl(tag, forumSection))
    } else {
      None
    }

    Map(
      "addForum" -> AddTopicController.getAddUrl(forumSection, tag),
      "forum" -> TopicListTools.split(
        topicByDate.map(p ⇒ p._1 -> BriefTopicRef.fromTopic(p._2, groupDao.getGroup(p._2.getGroupId).getTitle)))
    ) ++ more
  }
}