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
package ru.org.linux.tag

import com.typesafe.scalalogging.StrictLogging
import org.apache.commons.text.WordUtils
import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.{PathVariable, RequestMapping, RequestMethod}
import org.springframework.web.servlet.ModelAndView
import org.springframework.web.servlet.view.RedirectView
import ru.org.linux.auth.AnySession
import ru.org.linux.auth.AuthUtil.MaybeAuthorized
import ru.org.linux.gallery.ImageService
import ru.org.linux.group.GroupPermissionService
import ru.org.linux.section.{Section, SectionService}
import ru.org.linux.tag.TagPageController.isRecent
import ru.org.linux.topic.*
import ru.org.linux.topic.TopicListDto.CommitMode
import ru.org.linux.user.UserTagService

import java.time
import java.time.Instant
import java.util.concurrent.CompletionStage
import scala.concurrent.*
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration.*
import scala.jdk.CollectionConverters.*
import scala.jdk.FutureConverters.FutureOps

object TagPageController {
  private val TotalNewsCount = 21
  private val ForumTopicCount = 20
  private val GalleryCount = 6

  private val RecentPeriod: time.Duration = java.time.Duration.ofDays(365)

  val Timeout: FiniteDuration = 500.millis

  def isRecent(date: Instant): Boolean = date.isAfter(Instant.now().minus(RecentPeriod))
}

@Controller
@RequestMapping(value = Array("/tag/{tag}"), params = Array("!section"))
class TagPageController(tagService: TagService, prepareService: TopicPrepareService, topicListService: TopicListService,
                        sectionService: SectionService, userTagService: UserTagService,
                        imageService: ImageService, groupPermissionService: GroupPermissionService) extends StrictLogging {

  @RequestMapping(method = Array(RequestMethod.GET, RequestMethod.HEAD))
  def tagPage(@PathVariable tag: String): CompletionStage[ModelAndView] = MaybeAuthorized { implicit currentUser =>
    val deadline = TagPageController.Timeout.fromNow

    if (!TagName.isGoodTag(tag)) {
      throw new TagNotFoundException
    }

    val countF = tagService.countTagTopics(tag = tag, section = None, deadline = deadline)

    val relatedF =
      tagService.getRelatedTags(tag, deadline) map { relatedTags =>
        if (relatedTags.nonEmpty) {
          Some("relatedTags" -> relatedTags.asJava)
        } else {
          None
        }
      }

    val favs = currentUser.userOpt match {
      case Some(user) =>
        Seq("showFavoriteTagButton" -> !userTagService.hasFavoriteTag(user, tag),
          "showUnFavoriteTagButton" -> userTagService.hasFavoriteTag(user, tag),
          "showIgnoreTagButton" -> (!currentUser.moderator && !userTagService.hasIgnoreTag(user, tag)),
          "showUnIgnoreTagButton" -> (!currentUser.moderator && userTagService.hasIgnoreTag(user, tag)))
      case None =>
        Seq.empty
    }

    tagService.getTagInfo(tag, skipZero = !currentUser.moderator) match {
      case None =>
        tagService.getTagBySynonym(tag).map { mainName =>
          Future.successful(new ModelAndView(new RedirectView(mainName.url.get, false, false))).asJava
        }.getOrElse(throw new TagNotFoundException())
      case Some(tagInfo) =>
        val (news, newsDate) = getNewsSection(tag)
        val (forum, forumDate) = getTopicList(tag, tagInfo.id, Section.SECTION_FORUM, CommitMode.POSTMODERATED_ONLY)
        val gallery = getGallerySection(tag, tagInfo.id)
        val (polls, _) = getTopicList(tag, tagInfo.id, Section.SECTION_POLLS, CommitMode.COMMITED_ONLY)
        val (articles, _) = getTopicList(tag, tagInfo.id, Section.SECTION_ARTICLES, CommitMode.COMMITED_ONLY)

        val newsFirst = newsDate.isDefined && (newsDate.exists(isRecent) || newsDate.zip(forumDate).exists(p => p._1.isAfter(p._2)))

        val sections = news ++ gallery ++ forum ++ polls ++ articles

        val synonyms = tagService.getSynonymsFor(tagInfo.id)

        val model = Map[String, AnyRef](
          "tag" -> tag,
          "title" -> WordUtils.capitalize(tag),
          "favsCount" -> Int.box(userTagService.countFavs(tagInfo.id)),
          "ignoreCount" -> Int.box(userTagService.countIgnore(tagInfo.id)),
          "showAdsense" -> Boolean.box(!currentUser.authorized || !currentUser.profile.hideAdsense),
          "showDelete" -> Boolean.box(currentUser.moderator),
          "synonyms" -> synonyms.asJava,
          "newsFirst" -> Boolean.box(newsFirst)
        ) ++ sections ++ favs

        val safeRelatedF = relatedF.recover {
          case ex: TimeoutException =>
            logger.warn(s"Tag related search timed out (${ex.getMessage})")
            None
          case ex =>
            logger.warn("Unable to find related tags", ex)
            None
        }

        val safeCountF = countF.map(_.getOrElse(tagInfo.topicCount.toLong))

        (for {
          counter <- safeCountF
          related <- safeRelatedF
        } yield {
          new ModelAndView("tag-page", (model + ("counter" -> counter) ++ related).asJava)
        }).asJava
    }
}

  private def getNewsSection(tag: String)(implicit currentUser: AnySession) = {
    val newsSection = sectionService.getSection(Section.SECTION_NEWS)
    val newsTopics = topicListService.getTopicsFeed(newsSection, None, Some(tag), 0, None,
      TagPageController.TotalNewsCount, noTalks = false, tech = false)

    val (fullNewsTopics, briefNewsTopics) = if (newsTopics.headOption.map(_.commitDate.toInstant).exists(isRecent)) {
      newsTopics.splitAt(1)
    } else {
      (Seq.empty, newsTopics.take(TagPageController.TotalNewsCount-1))
    }

    val fullNews = prepareService.prepareTopics(fullNewsTopics, loadUserpics = false)

    val briefNewsByDate = TopicListTools.datePartition(briefNewsTopics)

    val more = if (newsTopics.size == TagPageController.TotalNewsCount) {
      Some("moreNews" -> TagTopicListController.tagListUrl(tag, newsSection))
    } else {
      None
    }

    val newestDate = newsTopics.headOption.map(_.commitDate.toInstant)

    val addNews = if (groupPermissionService.isTopicPostingAllowed(newsSection)) {
      Some("addNews" -> AddTopicController.getAddUrl(newsSection, tag))
    } else {
      None
    }

    (Map[String, AnyRef](
      "fullNews" -> fullNews.asJava,
      "briefNews" -> TopicListTools.split(briefNewsByDate.map(p => p._1 -> prepareService.prepareBrief(p._2, groupInTitle = false)))
    ) ++ more ++ addNews, newestDate)
  }

  private def getGallerySection(tag: String, tagId: Int)(implicit  currentUser: AnySession) = {
    val list = imageService.prepareGalleryItem(imageService.getGalleryItems(TagPageController.GalleryCount, tagId).asJava)
    val section = sectionService.getSection(Section.SECTION_GALLERY)

    val add = if (groupPermissionService.isTopicPostingAllowed(section)) {
      Some("addGallery" -> AddTopicController.getAddUrl(section, tag))
    } else {
      None
    }

    val more = if (list.size == TagPageController.GalleryCount) {
      Some("moreGallery" -> TagTopicListController.tagListUrl(tag, section))
    } else {
      None
    }

    Map[String, AnyRef]("gallery" -> list) ++ add ++ more
  }

  private def getTopicList(tag: String, tagId: Int, section: Int, mode: CommitMode)(implicit currentUser: AnySession) = {
    val forumSection = sectionService.getSection(section)

    val topicListDto = new TopicListDto
    topicListDto.setSection(forumSection.getId)
    topicListDto.setCommitMode(mode)
    topicListDto.setTag(tagId)
    topicListDto.setLimit(TagPageController.ForumTopicCount)

    val forumTopics = topicListService.getTopics(topicListDto)
    val topicByDate = TopicListTools.datePartition(forumTopics)

    val more = if (forumTopics.size == TagPageController.ForumTopicCount) {
      Some(forumSection.getUrlName+"More" -> TagTopicListController.tagListUrl(tag, forumSection))
    } else {
      None
    }

    val add = if (groupPermissionService.isTopicPostingAllowed(forumSection)) {
      Some(forumSection.getUrlName+"Add" -> AddTopicController.getAddUrl(forumSection, tag))
    } else {
      None
    }

    val newestDate = forumTopics.headOption.map(t => Instant.ofEpochMilli(t.getEffectiveDate.getMillis))

    (Map[String, AnyRef](
      forumSection.getUrlName -> TopicListTools.split(
        topicByDate.map(p => p._1 -> prepareService.prepareBrief(p._2, groupInTitle = true)))
    ) ++ more ++ add, newestDate)
  }
}
