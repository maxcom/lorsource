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

import org.springframework.stereotype.Service
import ru.org.linux.auth.{AnySession, NonAuthorizedSession}
import ru.org.linux.group.Group
import ru.org.linux.section.{Section, SectionService}
import ru.org.linux.tag.TagService
import ru.org.linux.topic.TopicListRequest.CommitMode
import ru.org.linux.topic.TopicListRequest.DateLimit
import ru.org.linux.user.User

import java.util.Calendar
import java.util.Date
import java.time.{Instant, ZonedDateTime}

@Service
object TopicListService:
  val MaxOffset = 300

  def fixOffset(offset: Int): Int =
    if offset < 0 then
      0
    else if offset > MaxOffset then
      MaxOffset
    else
      offset

@Service
class TopicListService(tagService: TagService, topicListDao: TopicListDao, sectionService: SectionService):
  def getTopicsFeed(
      section: Section,
      group: Option[Group],
      tag: Option[String],
      offset: Int,
      yearMonth: Option[(Int, Int)],
      count: Int,
      noTalks: Boolean,
      tech: Boolean)(using currentUser: AnySession): collection.Seq[Topic] =
    val commitMode =
      if section.isPremoderated then
        CommitMode.CommittedOnly
      else
        CommitMode.PostmoderatedOnly

    val base = TopicListRequest(
      notalks = noTalks,
      tech = tech,
      sections = Set(section.id),
      commitMode = commitMode,
      group = group.map(_.id).getOrElse(0),
      tag = tag.map(tagService.getTagId(_)).getOrElse(0)
    )

    val dto =
      if yearMonth.isDefined then
        val calendar = Calendar.getInstance
        calendar.set(yearMonth.get._1, yearMonth.get._2 - 1, 1, 0, 0, 0)
        val fromDate = calendar.getTime
        calendar.add(Calendar.MONTH, 1)
        val toDate = calendar.getTime
        base.copy(dateLimit = DateLimit.Between(fromDate, toDate))
      else
        val withLimit = base.copy(limit = Some(count), offset = Some(TopicListService.fixOffset(offset)).filter(_ > 0))
        if tag.isEmpty && group.isEmpty && !section.isPremoderated then
          val calendar = Calendar.getInstance
          calendar.setTime(new Date)
          calendar.add(Calendar.MONTH, -6)
          withLimit.copy(dateLimit = DateLimit.FromDate(calendar.getTime))
        else
          withLimit

    topicListDao.getTopics(dto, currentUser)

  def getUserTopicsFeed(
      user: User,
      section: Option[Section] = None,
      offset: Int,
      favorites: Boolean,
      watches: Boolean): collection.Seq[Topic] =
    val dto = TopicListRequest(
      limit = Some(20),
      offset = Some(offset),
      commitMode = CommitMode.All,
      userId = user.id,
      userFavs = favorites,
      userWatches = watches,
      sections = section.map(s => Set(s.id)).getOrElse(Set.empty)
    )

    topicListDao.getTopics(dto, NonAuthorizedSession)

  def getDrafts(user: User, offset: Int): collection.Seq[Topic] =
    val dto = TopicListRequest(
      limit = Some(20),
      offset = Some(offset),
      commitMode = CommitMode.All,
      userId = user.id,
      showDraft = true)

    topicListDao.getTopics(dto, NonAuthorizedSession)

  def getRssTopicsFeed(
      section: Section,
      group: Option[Group],
      fromDate: Instant,
      noTalks: Boolean,
      tech: Boolean): collection.Seq[Topic] =
    val commitMode =
      if section.isPremoderated then
        CommitMode.CommittedOnly
      else
        CommitMode.PostmoderatedOnly

    val dto = TopicListRequest(
      sections = Set(section.id),
      group = group.map(_.id).getOrElse(0),
      dateLimit = DateLimit.FromDate(Date.from(fromDate)),
      notalks = noTalks,
      tech = tech,
      limit = Some(30),
      commitMode = commitMode
    )

    topicListDao.getTopics(dto, NonAuthorizedSession)

  def getUncommitedTopic(section: Option[Section], fromDate: Date): collection.Seq[Topic] =
    val dto = TopicListRequest(
      commitMode = CommitMode.UncommittedOnly,
      sections = section.map(s => Set(s.id)).getOrElse(Set.empty),
      dateLimit = DateLimit.FromDate(fromDate))

    topicListDao.getTopics(dto, NonAuthorizedSession)

  def getDeletedTopics(sectionId: Int, skipBadReason: Boolean): Seq[DeletedTopic] =
    topicListDao.getDeletedTopics(sectionId, skipBadReason = skipBadReason)

  def getMainPageFeed(count: Int)(using session: AnySession): collection.Seq[Topic] =
    val sections =
      if session.profile.showGalleryOnMain then
        Set(Section.News, Section.Gallery, Section.Polls, Section.Articles)
      else
        Set(Section.News)

    val dto = TopicListRequest(
      limit = Some(count),
      dateLimit = DateLimit.FromDate(Date.from(ZonedDateTime.now.minusMonths(3).toInstant)),
      commitMode = CommitMode.CommittedOnly,
      sections = sections
    )

    topicListDao.getTopics(dto, NonAuthorizedSession)

  def getTopics(topicListRequest: TopicListRequest)(using currentUser: AnySession): collection.Seq[Topic] =
    topicListDao.getTopics(topicListRequest, currentUser)

  def getDeletedUserTopics(user: User, topics: Int): Seq[DeletedTopic] = topicListDao.getDeletedUserTopics(user, topics)

  def getUserSections(user: User): Seq[Section] = topicListDao.getUserSections(user).map(sectionService.idToSection)
