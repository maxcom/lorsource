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
import ru.org.linux.auth.{AnySession, CaptchaMode, IpBlockInfo}
import ru.org.linux.group.Group
import ru.org.linux.section.{Section, SectionService}
import ru.org.linux.topic.TopicPermissionService.*
import ru.org.linux.user.{User, UserService}

@Service
class TopicPostingChecker(sectionService: SectionService, userService: UserService, ipBlockChecker: IpBlockChecker):
  def checkTopicPosting(section: Section, addr: String)(using currentUser: AnySession): Permission[CaptchaMode] =
    checkTopicPostingImpl(section.topicsRestriction, currentUser.userOpt, addr)

  def checkTopicPosting(group: Group, addr: String)(using currentUser: AnySession): Permission[CaptchaMode] =
    val section = sectionService.getSection(group.sectionId)

    val maxPostscore = Math.max(group.topicRestriction, section.topicsRestriction)

    checkTopicPostingImpl(maxPostscore, currentUser.userOpt, addr)

  private def postScoreCheckerChain(user: User, postscore: Int): RestrictionChain[Unit] =
    postscore match
      case POSTSCORE_UNRESTRICTED =>
        Unrestricted.unit
      case POSTSCORE_MODERATORS_ONLY =>
        Unrestricted.unit.restrict(!user.isModerator, "только для модераторов")
      case POSTSCORE_REGISTERED_ONLY =>
        Unrestricted.unit.restrict(user.anonymous, "только для зарегистрированных")
      case POSTSCORE_NO_COMMENTS | POSTSCORE_HIDE_COMMENTS =>
        Restricted("постинг запрещен") // в топиках не бывает, но пусть будет на всякий случай
      case 100 | 200 | 300 | 400 =>
        Unrestricted
          .unit
          .restrict(
            user.anonymous || user.score < postscore,
            s"только для зарегистрированных, минимум ${User.getStars(postscore, postscore, false)}")
      case 500 =>
        Unrestricted
          .unit
          .restrict(
            user.anonymous || user.score < postscore,
            s"только для зарегистрированных, ${User.getStars(postscore, postscore, false)}")
      case _ =>
        Unrestricted
          .unit
          .restrict(user.anonymous || user.score < postscore, s"только для зарегистрированных, score>=$postscore")

  private def checkTopicPostingImpl(
      restriction: Int,
      currentUserOpt: Option[User],
      addr: String): Permission[CaptchaMode] =
    val currentUser = currentUserOpt.getOrElse(userService.getAnonymous)

    Unrestricted
      .unit
      .restrict(currentUser.blocked, "пользователь заблокирован")
      .restrict(FrozenUserChecker.checkChain(currentUser))
      .restrict(postScoreCheckerChain(currentUser, restriction))
      .restrict(ipBlockChecker.checkChain(addr, currentUserOpt))
      .seal
