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

import org.springframework.stereotype.Service
import ru.org.linux.auth.AnySession
import ru.org.linux.group.{Group, GroupService}
import ru.org.linux.topic.Topic
import ru.org.linux.user.User

@Service
class AddCommentChecker(groupService: GroupService, postScoreChecker: PostScoreChecker):
  private def checkCommentPostingChain(group: Group, topic: Topic, user: User): RestrictionChain =
    assert(topic.groupId == group.id)

    val restriction = postScoreChecker.commentRestrictionScore(group, topic)
    val viewByAuthor = user.id == topic.authorUserId

    Unrestricted
      .restrict(topic.deleted, "топик удален")
      .restrict(topic.expired, "топик перемещен в архив")
      .restrict(topic.draft, "черновик топики еще не опубликован")
      .restrict(postScoreChecker.postScoreCheckerChain(user, restriction, viewByAuthor))

  def checkCommentPosting(group: Group, topic: Topic)(using session: AnySession): Permission =
    Unrestricted
      .restrict(checkCommentPostingChain(group, topic, session.user))
      .restrict(FrozenUserChecker.checkChain)
      .restrict(IpBlockChecker.checkChain)
      .seal

  def checkCommentPostingForUser(group: Group, topic: Topic, user: User): Permission =
    Unrestricted
      .restrict(user.blocked, "пользователь заблокирован")
      .restrict(checkCommentPostingChain(group, topic, user))
      .seal

  def checkCommentPosting(topic: Topic)(using anySession: AnySession): Permission =
    val group = groupService.getGroup(topic.groupId)

    checkCommentPosting(group, topic)