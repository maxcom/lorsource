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
import ru.org.linux.group.Group
import ru.org.linux.section.{Section, SectionService}
import ru.org.linux.topic.TopicPermissionService.*
import ru.org.linux.user.User

@Service
class AddTopicChecker(sectionService: SectionService):
  def checkTopicPosting(section: Section)(using session: AnySession): Permission =
    checkTopicPostingImpl(section.topicsRestriction)

  def checkTopicPosting(group: Group)(using session: AnySession): Permission =
    val section = sectionService.getSection(group.sectionId)
    val maxPostscore = Math.max(group.topicRestriction, section.topicsRestriction)
    checkTopicPostingImpl(maxPostscore)

  def checkTopicPostingAnywhere(using session: AnySession): Permission =
    val minRestriction = sectionService.sections.view.map(_.topicsRestriction).min

    checkTopicPostingImpl(minRestriction)

  private def postScoreCheckerChain(user: User, postscore: Int): RestrictionChain =
    postscore match
      case POSTSCORE_UNRESTRICTED =>
        Unrestricted
      case POSTSCORE_MODERATORS_ONLY =>
        Unrestricted.restrict(!user.isModerator, "только для модераторов")
      case POSTSCORE_REGISTERED_ONLY =>
        Unrestricted.restrict(user.anonymous, "только для зарегистрированных")
      case POSTSCORE_NO_COMMENTS | POSTSCORE_HIDE_COMMENTS =>
        Restricted("постинг запрещен") // в топиках не бывает, но пусть будет на всякий случай
      case 100 | 200 | 300 | 400 =>
        Unrestricted.restrict(
          user.anonymous || user.score < postscore,
          s"только для зарегистрированных, минимум ${User.getStars(postscore, postscore, false)}")
      case 500 =>
        Unrestricted.restrict(
          user.anonymous || user.score < postscore,
          s"только для зарегистрированных, ${User.getStars(postscore, postscore, false)}")
      case _ =>
        Unrestricted.restrict(
          user.anonymous || user.score < postscore,
          s"только для зарегистрированных, score>=$postscore")

  private def checkTopicPostingImpl(restriction: Int)(using session: AnySession): Permission =
    Unrestricted
      .restrict(FrozenUserChecker.checkChain)
      .restrict(postScoreCheckerChain(session.user, restriction))
      .restrict(IpBlockChecker.checkChain)
      .seal
