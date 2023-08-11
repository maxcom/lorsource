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
package ru.org.linux.tag

import akka.actor.ActorSystem
import com.typesafe.scalalogging.StrictLogging
import org.apache.commons.text.WordUtils
import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.{PathVariable, RequestMapping, RequestMethod}
import org.springframework.web.servlet.ModelAndView
import org.springframework.web.servlet.view.RedirectView
import ru.org.linux.auth.AuthUtil
import ru.org.linux.gallery.ImageService
import ru.org.linux.group.GroupDao
import ru.org.linux.section.{Section, SectionService}
import ru.org.linux.site.Template
import ru.org.linux.tag.TagPageController.isRecent
import ru.org.linux.topic.*
import ru.org.linux.topic.TopicListDto.CommitMode
import ru.org.linux.user.{User, UserTagService}
import ru.org.linux.util.RichFuture.*

import java.time
import java.time.Instant
import java.util.concurrent.CompletionStage
import scala.compat.java8.FutureConverters.*
import scala.concurrent.*
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration.*
import scala.jdk.CollectionConverters.*
import scala.jdk.OptionConverters.RichOption

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
 sectionService: SectionService, groupDao: GroupDao, userTagService: UserTagService, imageService: ImageService,
 actorSystem: ActorSystem) extends StrictLogging {

  private implicit val akka: ActorSystem = actorSystem

  @RequestMapping(method = Array(RequestMethod.GET, RequestMethod.HEAD))
  def tagPage(@PathVariable tag: String): CompletionStage[ModelAndView] = AuthUtil.AuthorizedOpt { currentUserObj =>
    val currentUser = currentUserObj.map(_.user)

    val deadline = TagPageController.Timeout.fromNow

    if (!TagName.isGoodTag(tag)) {
      throw new TagNotFoundException
    }

    val countF = tagService.countTagTopics(tag)

    val relatedF = {
      tagService.getRelatedTags(tag) map { relatedTags =>
        if (relatedTags.nonEmpty) {
          Some("relatedTags" -> relatedTags.asJava)
        } else {
          None
        }
      }
    }

    val favs = currentUser match {
      case Some(currentUser) =>
        Seq("showFavoriteTagButton" -> !userTagService.hasFavoriteTag(currentUser, tag),
          "showUnFavoriteTagButton" -> userTagService.hasFavoriteTag(currentUser, tag),
          "showIgnoreTagButton" -> (currentUserObj.forall(!_.moderator) && !userTagService.hasIgnoreTag(currentUser, tag)),
          "showUnIgnoreTagButton" -> (currentUserObj.forall(!_.moderator) && userTagService.hasIgnoreTag(currentUser, tag)))
      case None =>
        Seq.empty
    }

    tagService.getTagInfo(tag, skipZero = currentUserObj.forall(!_.moderator)) match {
      case None =>
        tagService.getTagBySynonym(tag).map { mainName =>
          Future.successful(new ModelAndView(new RedirectView(mainName.url.get, false, false))).toJava
        }.getOrElse(throw new TagNotFoundException())
      case Some(tagInfo) =>
        val (news, newsDate) = getNewsSection(tag, currentUser)
        val (forum, forumDate) = getTopicList(tag, tagInfo.id, Section.SECTION_FORUM, CommitMode.POSTMODERATED_ONLY, currentUser)
        val gallery = getGallerySection(tag, tagInfo.id, currentUser)
        val (polls, _) = getTopicList(tag, tagInfo.id, Section.SECTION_POLLS, CommitMode.COMMITED_ONLY, currentUser)
        val (articles, _) = getTopicList(tag, tagInfo.id, Section.SECTION_ARTICLES, CommitMode.COMMITED_ONLY, currentUser)

        val newsFirst = newsDate.isDefined && (newsDate.exists(isRecent) || newsDate.zip(forumDate).exists(p => p._1.isAfter(p._2)))

        val sections = news ++ gallery ++ forum ++ polls ++ articles

        val tmpl = Template.getTemplate

        val synonyms = tagService.getSynonymsFor(tagInfo.id)

        val model = Map(
          "tag" -> tag,
          "title" -> WordUtils.capitalize(tag),
          "favsCount" -> userTagService.countFavs(tagInfo.id),
          "ignoreCount" -> userTagService.countIgnore(tagInfo.id),
          "showAdsense" -> Boolean.box(currentUser.isEmpty || !tmpl.getProf.isHideAdsense),
          "showDelete" -> Boolean.box(currentUserObj.exists(_.moderator)),
          "synonyms" -> synonyms.asJava,
          "newsFirst" -> Boolean.box(newsFirst)
        ) ++ sections ++ favs

        val safeRelatedF = relatedF withTimeout deadline.timeLeft recover {
          case ex: TimeoutException =>
            logger.warn(s"Tag related search timed out (${ex.getMessage})")
            None
          case ex =>
            logger.warn("Unable to find related tags", ex)
            None
        }

        val safeCountF = countF withTimeout deadline.timeLeft recover {
          case ex: TimeoutException =>
            logger.warn(s"Tag topics count timed out (${ex.getMessage})")
            tagInfo.topicCount.toLong
          case ex =>
            logger.warn("Unable to count tag topics", ex)
            tagInfo.topicCount.toLong
        }

        (for {
          counter <- safeCountF
          related <- safeRelatedF
        } yield {
          new ModelAndView("tag-page", (model + ("counter" -> counter) ++ related).asJava)
        }).toJava
    }
}

  private def getNewsSection(tag: String, currentUser: Option[User]) = {
    val newsSection = sectionService.getSection(Section.SECTION_NEWS)
    val newsTopics = topicListService.getTopicsFeed(newsSection, null, tag, 0, None.toJava, None.toJava,
      TagPageController.TotalNewsCount, currentUser.orNull, false, false).asScala

    val (fullNewsTopics, briefNewsTopics) = if (newsTopics.headOption.map(_.commitDate.toInstant).exists(isRecent)) {
      newsTopics.splitAt(1)
    } else {
      (Seq.empty, newsTopics.dropRight(1))
    }

    val tmpl = Template.getTemplate
    val fullNews = prepareService.prepareTopicsForUser(fullNewsTopics.asJava, currentUser.orNull, tmpl.getProf, loadUserpics = false)

    val briefNewsByDate = TopicListTools.datePartition(briefNewsTopics)

    val more = if (newsTopics.size == TagPageController.TotalNewsCount) {
      Some("moreNews" -> TagTopicListController.tagListUrl(tag, newsSection))
    } else {
      None
    }

    val newestDate = newsTopics.headOption.map(_.commitDate.toInstant)

    (Map(
      "fullNews" -> fullNews,
      "addNews" -> AddTopicController.getAddUrl(newsSection, tag),
      "briefNews" -> TopicListTools.split(briefNewsByDate.map(p => p._1 -> BriefTopicRef.fromTopicNoGroup(p._2)))
    ) ++ more, newestDate)
  }

  private def getGallerySection(tag: String, tagId: Int, currentUser: Option[User]) = {
    val list = imageService.prepareGalleryItem(imageService.getGalleryItems(TagPageController.GalleryCount, tagId))
    val section = sectionService.getSection(Section.SECTION_GALLERY)

    val add = if (currentUser.isDefined) {
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

  private def getTopicList(tag: String, tagId: Int, section: Int, mode: CommitMode, currentUser: Option[User]) = {
    val forumSection = sectionService.getSection(section)

    val topicListDto = new TopicListDto
    topicListDto.setSection(forumSection.getId)
    topicListDto.setCommitMode(mode)
    topicListDto.setTag(tagId)
    topicListDto.setLimit(TagPageController.ForumTopicCount)

    val forumTopics = topicListService.getTopics(topicListDto, currentUser.orNull).asScala
    val topicByDate = TopicListTools.datePartition(forumTopics)

    val more = if (forumTopics.size == TagPageController.ForumTopicCount) {
      Some(forumSection.getUrlName+"More" -> TagTopicListController.tagListUrl(tag, forumSection))
    } else {
      None
    }

    val newestDate = forumTopics.headOption.map(t => Instant.ofEpochMilli(t.getEffectiveDate.getMillis))

    (Map(
      forumSection.getUrlName+"Add" -> AddTopicController.getAddUrl(forumSection, tag),
      forumSection.getUrlName -> TopicListTools.split(
        topicByDate.map(p => p._1 -> BriefTopicRef.fromTopic(p._2, groupDao.getGroup(p._2.groupId).getTitle)))
    ) ++ more, newestDate)
  }
}
