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
package ru.org.linux.rights

import ru.org.linux.auth.AnySession
import ru.org.linux.section.Section
import ru.org.linux.topic.TopicPermissionService.POSTSCORE_NO_COMMENTS
import ru.org.linux.topic.{PreparedTopic, Topic}
import ru.org.linux.user.UserPermissionService

import java.time.{Duration, Instant, ZoneId}

object EditTopicChecker:
  private val EditPeriod: Duration = Duration.ofDays(14)

  def checkCommit(topic: Topic)(using session: AnySession): Permission =
    Unrestricted
      .restrict(!session.moderator && !session.corrector, "только для корректоров и модераторов")
      .restrict(session.corrector && topic.authorUserId == session.user.id, "нельзя подтверждать собственные топики")
      .restrict(preCheck(topic))
      .seal
  end checkCommit

  def checkContentEdit(preparedTopic: PreparedTopic)(using session: AnySession): Permission =
    val topic = preparedTopic.message

    preCheck(topic)
      .restrict(
        !UserPermissionService.allowedFormats(session.user).contains(preparedTopic.markupType),
        s"запрещено редактирование текстов в формате ${preparedTopic.markupType}"
      )
      .permit(session.administrator)
      .restrict(topic.expired && !topic.draft, "нельзя править архивные топики")
      .permit(session.moderator)
      .restrict(topic.postscore == POSTSCORE_NO_COMMENTS, "нельзя править топики с выключенными комментариями")
      .permit(session.corrector && preparedTopic.section.premoderated)
      .concat(checkEditByAuthor(preparedTopic))
      .seal
  end checkContentEdit

  def checkTagsEdit(preparedTopic: PreparedTopic)(using session: AnySession): Permission =
    val topic = preparedTopic.message

    preCheck(topic)
      .permit(session.administrator || session.moderator || session.corrector)
      .concat(checkEditByAuthor(preparedTopic))
      .seal
  end checkTagsEdit

  def checkAnythingEdit(preparedTopic: PreparedTopic)(using session: AnySession): Permission =
    checkTagsEdit(preparedTopic).or(checkContentEdit(preparedTopic))
  end checkAnythingEdit

  private def preCheck(topic: Topic)(using session: AnySession): RestrictionChain =
    Unrestricted
      .restrict(topic.deleted, "нельзя править удаленные топики")
      .restrict(!session.authorized, "только для зарегистрированных")
      .restrict(FrozenUserChecker.checkChain)
      .restrict(IpBlockChecker.checkChain)
  end preCheck

  private def checkEditByAuthor(preparedTopic: PreparedTopic)(using session: AnySession): PermissionChain =
    val topic = preparedTopic.message
    lazy val editDeadline =
      val base =
        if topic.commited && preparedTopic.section.id == Section.Articles then
          topic.commitDate
        else
          topic.postdate
      base.toInstant.atZone(ZoneId.systemDefault()).plus(EditPeriod).toInstant

    Unrestricted
      .restrict(preparedTopic.author.id != session.user.id, "нельзя править чужие топики")
      .permit(topic.draft)
      .restrict(
        topic.commited && preparedTopic.section.premoderated && preparedTopic.section.id != Section.Articles,
        "в этом разделе нельзя править подтвержденные топики"
      )
      .permit(!topic.commited && topic.sticky)
      .permit(!topic.commited && preparedTopic.section.premoderated)
      .restrict(editDeadline.isBefore(Instant.now()), "истек срок редактирования топика")
  end checkEditByAuthor

