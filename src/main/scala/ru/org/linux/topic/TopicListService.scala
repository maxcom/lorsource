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

import org.joda.time.DateTime
import org.springframework.stereotype.Service
import ru.org.linux.group.Group
import ru.org.linux.section.{Section, SectionService}
import ru.org.linux.tag.TagNotFoundException
import ru.org.linux.tag.TagService
import ru.org.linux.topic.TopicListDto.CommitMode
import ru.org.linux.user.User

import java.util.Calendar
import java.util.Date
import ru.org.linux.topic.TopicListDto.CommitMode.COMMITED_ONLY
import ru.org.linux.topic.TopicListDto.CommitMode.POSTMODERATED_ONLY

@Service
object TopicListService {
  val MaxOffset = 300

  /**
   * Корректировка смещения в результатах выборки.
   *
   * @param offset оригинальное смещение
   * @return скорректированное смещение
   */
  def fixOffset(offset: Integer): Int =
    if (offset != null) {
      if (offset < 0) {
        0
      } else if (offset > MaxOffset) {
        MaxOffset
      } else {
        offset
      }
    } else {
      0
    }
}

@Service
class TopicListService(tagService: TagService, topicListDao: TopicListDao, sectionService: SectionService) {
  /**
   * Получение списка топиков.
   *
   * @param section секция
   * @param group   группа
   * @param tag     тег
   * @param offset  смещение в результатах выборки
   * @param yearMonth год, месяц
   * @return список топиков
   * @throws TagNotFoundException
   */
  @throws[TagNotFoundException]
  def getTopicsFeed(section: Section, group: Option[Group], tag: Option[String], offset: Int, yearMonth: Option[(Int, Int)],
                    count: Int, currentUser: Option[User], noTalks: Boolean, tech: Boolean): collection.Seq[Topic] = {
    val topicListDto = new TopicListDto

    topicListDto.setNotalks(noTalks)
    topicListDto.setTech(tech)

    topicListDto.setSection(section.getId)

    if (section.isPremoderated) {
      topicListDto.setCommitMode(COMMITED_ONLY)
    } else {
      topicListDto.setCommitMode(POSTMODERATED_ONLY)
    }

    group.foreach { group =>
      topicListDto.setGroup(group.id)
    }

    tag.foreach { tag =>
      topicListDto.setTag(tagService.getTagId(tag))
    }

    if (yearMonth.isDefined) {
      topicListDto.setDateLimitType(TopicListDto.DateLimitType.BETWEEN)

      val calendar = Calendar.getInstance
      calendar.set(yearMonth.get._1, yearMonth.get._2 - 1, 1, 0, 0, 0)
      topicListDto.setFromDate(calendar.getTime)
      calendar.add(Calendar.MONTH, 1)
      topicListDto.setToDate(calendar.getTime)
    } else {
      topicListDto.setLimit(count)
      topicListDto.setOffset(if (offset > 0) offset else null)

      if (tag.isEmpty && group.isEmpty && !section.isPremoderated) {
        topicListDto.setDateLimitType(TopicListDto.DateLimitType.FROM_DATE)

        val calendar = Calendar.getInstance
        calendar.setTime(new Date)
        calendar.add(Calendar.MONTH, -6)
        topicListDto.setFromDate(calendar.getTime)
      }
    }

    topicListDao.getTopics(topicListDto, currentUser)
  }

  /**
   * Получение списка топиков пользователя.
   *
   * @param user       объект пользователя
   * @param offset     смещение в результатах выборки
   * @param isFavorite true если нужно выбрать избранные сообщения пользователя
   * @return список топиков пользователя
   */
  def getUserTopicsFeed(user: User, offset: Int, isFavorite: Boolean, watches: Boolean): collection.Seq[Topic] =
    getUserTopicsFeed(user, None, None, offset, isFavorite, watches)

  /**
   * Получение списка топиков пользователя.
   *
   * @param user      объект пользователя
   * @param section   секция, из которой выбрать сообщения
   * @param offset    смещение в результатах выборки
   * @param favorites true если нужно выбрать избранные сообщения пользователя
   * @return список топиков пользователя
   */
  def getUserTopicsFeed(user: User, section: Option[Section], group: Option[Group], offset: Int,
                        favorites: Boolean, watches: Boolean): collection.Seq[Topic] = {
    val topicListDto = new TopicListDto

    topicListDto.setLimit(20)
    topicListDto.setOffset(offset)
    topicListDto.setCommitMode(CommitMode.ALL)
    topicListDto.setUserId(user.getId)
    topicListDto.setUserFavs(favorites)
    topicListDto.setUserWatches(watches)

    section.foreach { section =>
      topicListDto.setSection(section.getId)
    }

    group.foreach { group =>
      topicListDto.setGroup(group.id)
    }

    topicListDao.getTopics(topicListDto, None)
  }

  /**
   * Получение списка черновиков пользователя.
   *
   * @param user   объект пользователя
   * @param offset смещение в результатах выборки
   * @return список топиков пользователя
   */
  def getDrafts(user: User, offset: Int): collection.Seq[Topic] = {
    val topicListDto = new TopicListDto

    topicListDto.setLimit(20)
    topicListDto.setOffset(offset)
    topicListDto.setCommitMode(CommitMode.ALL)
    topicListDto.setUserId(user.getId)
    topicListDto.setShowDraft(true)

    topicListDao.getTopics(topicListDto, None)
  }

  /**
   * Получение списка топиков для RSS-ленты.
   *
   * @param section  секция
   * @param group    группа
   * @param fromDate от какой даты получить список
   * @param noTalks  без Talks
   * @param tech     только технические
   * @return список топиков для RSS-ленты
   */
  def getRssTopicsFeed(section: Section, group: Option[Group], fromDate: Date, noTalks: Boolean, tech: Boolean): collection.Seq[Topic] = {
    val topicListDto = new TopicListDto

    topicListDto.setSection(section.getId)

    group.foreach { group =>
      topicListDto.setGroup(group.id)
    }

    topicListDto.setDateLimitType(TopicListDto.DateLimitType.FROM_DATE)
    topicListDto.setFromDate(fromDate)
    topicListDto.setNotalks(noTalks)
    topicListDto.setTech(tech)
    topicListDto.setLimit(30)

    if (section.isPremoderated) {
      topicListDto.setCommitMode(CommitMode.COMMITED_ONLY)
    } else {
      topicListDto.setCommitMode(CommitMode.POSTMODERATED_ONLY)
    }

    topicListDao.getTopics(topicListDto, None)
  }

  def getUncommitedTopic(section: Option[Section], fromDate: Date, includeAnonymous: Boolean): collection.Seq[Topic] = {
    val topicListDto = new TopicListDto

    topicListDto.setCommitMode(CommitMode.UNCOMMITED_ONLY)

    section.foreach { section =>
      topicListDto.setSection(section.getId)
    }

    topicListDto.setDateLimitType(TopicListDto.DateLimitType.FROM_DATE)
    topicListDto.setFromDate(fromDate)
    topicListDto.setIncludeAnonymous(includeAnonymous)

    topicListDao.getTopics(topicListDto, None)
  }

  def getDeletedTopics(sectionId: Int, skipBadReason: Boolean, includeAnonymous: Boolean): Seq[DeletedTopic] =
    topicListDao.getDeletedTopics(sectionId, skipBadReason, includeAnonymous)

  def getMainPageFeed(showGalleryOnMain: Boolean, count: Int, hideMinor: Boolean): collection.Seq[Topic] = {
    val topicListDto = new TopicListDto

    topicListDto.setLimit(count)
    topicListDto.setDateLimitType(TopicListDto.DateLimitType.FROM_DATE)

    topicListDto.setFromDate(DateTime.now.minusMonths(3).toDate)

    if (hideMinor) {
      topicListDto.setMiniNewsMode(TopicListDto.MiniNewsMode.MAJOR)
    }

    topicListDto.setCommitMode(CommitMode.COMMITED_ONLY)

    if (showGalleryOnMain) {
      topicListDto.setSection(Section.SECTION_NEWS, Section.SECTION_GALLERY)
    } else {
      topicListDto.setSection(Section.SECTION_NEWS)
    }

    topicListDao.getTopics(topicListDto, None)
  }

  def getTopics(topicListDto: TopicListDto, currentUser: Option[User]): collection.Seq[Topic] =
    topicListDao.getTopics(topicListDto, currentUser)

  def getDeletedUserTopics(user: User, topics: Int): Seq[DeletedTopic] =
    topicListDao.getDeletedUserTopics(user, topics)

  def getUserSections(user: User): Seq[Section] =
    topicListDao.getUserSections(user).map(sectionService.idToSection)
}