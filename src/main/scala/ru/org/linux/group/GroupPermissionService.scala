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
package ru.org.linux.group

import org.springframework.stereotype.Service
import ru.org.linux.auth.{AnySession, AuthorizedSession}
import ru.org.linux.msgbase.DeleteInfoDao
import ru.org.linux.rights.SlowModeChecker
import ru.org.linux.section.Section.{Articles, Gallery, News, Polls}
import ru.org.linux.section.{Section, SectionService}
import ru.org.linux.topic.{Topic, TopicDao, TopicPermissionService}
import ru.org.linux.user.User

import scala.beans.BeanProperty
import java.time.{Duration, Instant, ZoneId}
import java.time.temporal.ChronoUnit

@Service
object GroupPermissionService:
  private val DeletePeriod = Duration.ofDays(3)
  private val CreateTagScore = 200

  case class TopicLimitInfo(
      @BeanProperty
      limit: Int,
      @BeanProperty
      currentCount: Int,
      @BeanProperty
      reached: Boolean,
      @BeanProperty
      exempt: Boolean)

@Service
class GroupPermissionService(
    sectionService: SectionService,
    deleteInfoDao: DeleteInfoDao,
    topicDao: TopicDao,
    slowModeChecker: SlowModeChecker):
  import GroupPermissionService.*

  /** Проверка может ли пользователь удалить топик
    *
    * @param user
    *   пользователь удаляющий сообщение
    * @return
    *   признак возможности удаления
    */
  private def isDeletableByUser(topic: Topic, user: User, section: Section): Boolean =
    if topic.authorUserId != user.id then
      false
    else if topic.draft then
      true
    else if section.premoderated && topic.commited then
      false
    else
      val deleteDeadline = topic.postdate.toInstant.atZone(ZoneId.systemDefault()).plus(DeletePeriod).toInstant

      deleteDeadline.isAfter(Instant.now) && topic.commentCount == 0

  def isUndeletable(topic: Topic)(using user: AnySession): Boolean =
    if !topic.deleted || !user.moderator then
      false
    else if user.administrator then
      true
    else if !topic.expired then
      true
    else
      deleteInfoDao
        .getDeleteInfo(topic.id)
        .filter(_.delDate != null)
        .map(_.delDate.toInstant)
        .exists(_.isAfter(Instant.now.minus(14, ChronoUnit.DAYS)))

  def enableAllowAnonymousCheckbox(group: Group)(using currentUser: AnySession): Boolean =
    currentUser.authorized && !group.premoderated &&
      Math.max(group.commentsRestriction, Section.getCommentPostscore(group.sectionId)) <
      TopicPermissionService.POSTSCORE_REGISTERED_ONLY

  def isImagePostingAllowed(section: Section)(using currentUser: AnySession): Boolean =
    if section.imagepost then
      true
    else if currentUser.authorized &&
      (currentUser.moderator || currentUser.corrector || currentUser.userOpt.exists(_.getScore >= 50))
    then
      section.imageAllowed
    else
      false

  def additionalImageLimit(section: Section)(using currentUser: AnySession): Int =
    if isImagePostingAllowed(section) then
      section.id match
        case Articles | Gallery | News | Polls =>
          3
        case _ =>
          0
    else
      0

  def isDeletable(topic: Topic)(using user: AuthorizedSession): Boolean =
    if user.administrator then
      true
    else
      val section = sectionService.getSection(topic.sectionId)

      val deletableByUser = isDeletableByUser(topic, user.user, section)

      if !deletableByUser && user.moderator then
        isDeletableByModerator(topic, user.user, section)
      else
        deletableByUser

  /** Проверка, может ли модератор удалить топик
    *
    * @param moderator
    *   пользователь удаляющий сообщение
    * @return
    *   признак возможности удаления
    */
  private def isDeletableByModerator(topic: Topic, moderator: User, section: Section) =
    val deleteDeadline = topic.postdate.toInstant.atZone(ZoneId.systemDefault()).plusMonths(1).toInstant

    if section.premoderated && !topic.commited then
      true
    else if section.premoderated && topic.commited && deleteDeadline.isAfter(Instant.now) then
      true
    else if !section.premoderated then
      true
    else
      false

  def canCreateTag(section: Section)(using session: AnySession): Boolean =
    val user = session.userOpt.orNull

    if section.premoderated && user != null && !user.anonymous then
      true
    else
      user != null && user.getScore >= CreateTagScore

  def canViewAllDeletedTopics(using session: AnySession): Boolean =
    session.authorized && session.userOpt.exists(_.score >= 50) &&
      !session.userOpt.exists(u => u.isFrozen || slowModeChecker.check(u).restricted)

  private def topicLimit(user: User): Int = Math.max(user.getGreenStars, 2)

  def topicLimitInfo(section: Section)(using currentUser: AnySession): GroupPermissionService.TopicLimitInfo =
    currentUser.userOpt match
      case Some(user) if user.isModerator || user.canCorrect =>
        GroupPermissionService.TopicLimitInfo(limit = 0, currentCount = 0, reached = false, exempt = true)
      case Some(user) =>
        val limit = topicLimit(user)
        val count = topicDao.countRecentTopics(user.id, section.id)
        GroupPermissionService.TopicLimitInfo(
          limit = limit,
          currentCount = count,
          reached = count >= limit,
          exempt = false)
      case None =>
        GroupPermissionService.TopicLimitInfo(limit = 0, currentCount = 0, reached = false, exempt = true)
