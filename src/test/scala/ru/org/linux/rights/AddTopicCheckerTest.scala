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

import munit.FunSuite
import org.mockito.Mockito.{mock, when}
import ru.org.linux.auth.{AnySession, AuthorizedSession, IpBlockInfo, NonAuthorizedSession}
import ru.org.linux.group.Group
import ru.org.linux.section.{Section, SectionScrollModeEnum, SectionService}
import ru.org.linux.topic.TopicPermissionService.*
import ru.org.linux.user.{Profile, User, UserService}

import java.sql.Timestamp

class AddTopicCheckerTest extends FunSuite:

  private val unrestrictedSection =
    Section(
      name = "Test",
      imagepost = false,
      moderate = false,
      id = 1,
      votepoll = false,
      scrollMode = SectionScrollModeEnum.NO_SCROLL,
      topicsRestriction = POSTSCORE_UNRESTRICTED,
      imageAllowed = false
    )

  private val defaultGroup =
    Group(
      premoderated = false,
      pollPostAllowed = false,
      linksAllowed = false,
      sectionId = 1,
      defaultLinkText = "",
      urlName = "test",
      image = "",
      topicRestriction = POSTSCORE_UNRESTRICTED,
      commentsRestriction = POSTSCORE_UNRESTRICTED,
      id = 1,
      stat3 = 0,
      resolvable = false,
      title = "Test Group",
      info = "",
      longInfo = ""
    )

  private def makeUser(
      nick: String = "testuser",
      id: Int = 1,
      canmod: Boolean = false,
      candel: Boolean = false,
      anonymous: Boolean = false,
      corrector: Boolean = false,
      blocked: Boolean = false,
      password: String = "password",
      score: Int = 50,
      maxScore: Int = 50,
      photo: String = null,
      email: String = null,
      fullName: String = null,
      unreadEvents: Int = 0,
      frozenUntil: Timestamp = null,
      activated: Boolean = true): User =
    User(
      nick,
      id,
      canmod,
      candel,
      anonymous,
      corrector,
      blocked,
      password,
      score,
      maxScore,
      photo,
      email,
      fullName,
      unreadEvents,
      frozenUntil,
      activated)

  private def makeSession(user: User, moderator: Boolean = false): AnySession =
    AuthorizedSession(
      user,
      corrector = false,
      moderator = moderator,
      administrator = false,
      profile = Profile.DEFAULT,
      ipBlockInfo = IpBlockInfo("127.0.0.1"))

  private val unblockedIpInfo = IpBlockInfo("127.0.0.1")

  private def makeChecker(sectionService: SectionService = mock(classOf[SectionService])): AddTopicChecker =
    new AddTopicChecker(sectionService)

  // === checkTopicPosting(section) tests ===

  test("unrestricted section: authorized user is permitted"):
    val checker = makeChecker()
    val user = makeUser()
    val session = makeSession(user)

    val result = checker.checkTopicPosting(unrestrictedSection)(using session)

    assert(result.permitted)

  test("unrestricted section: anonymous session gets anonymous user and is permitted"):
    val userService = mock(classOf[UserService])
    val anonymousUser = makeUser(anonymous = true)
    when(userService.getAnonymous).thenReturn(anonymousUser)
    val checker = makeChecker()

    val result =
      checker.checkTopicPosting(unrestrictedSection)(using
        NonAuthorizedSession(anonymousUser, ipBlockInfo = IpBlockInfo("127.0.0.1")))

    assert(result.permitted)

  test("registered-only section: authorized user is permitted"):
    val section = unrestrictedSection.copy(topicsRestriction = POSTSCORE_REGISTERED_ONLY)
    val checker = makeChecker()
    val user = makeUser()
    val session = makeSession(user)

    val result = checker.checkTopicPosting(section)(using session)

    assert(result.permitted)

  test("registered-only section: anonymous session is restricted"):
    val section = unrestrictedSection.copy(topicsRestriction = POSTSCORE_REGISTERED_ONLY)
    val userService = mock(classOf[UserService])
    val anonymousUser = makeUser(anonymous = true)
    when(userService.getAnonymous).thenReturn(anonymousUser)
    val checker = makeChecker()

    val result =
      checker.checkTopicPosting(section)(using
        NonAuthorizedSession(anonymousUser, ipBlockInfo = IpBlockInfo.apply("127.0.0.1")))

    assert(result.restricted)
    assertEquals(result.reason, "только для зарегистрированных")

  test("moderators-only section: moderator is permitted"):
    val section = unrestrictedSection.copy(topicsRestriction = POSTSCORE_MODERATORS_ONLY)
    val checker = makeChecker()
    val user = makeUser(canmod = true)
    val session = makeSession(user, moderator = true)

    val result = checker.checkTopicPosting(section)(using session)

    assert(result.permitted)

  test("moderators-only section: non-moderator is restricted"):
    val section = unrestrictedSection.copy(topicsRestriction = POSTSCORE_MODERATORS_ONLY)
    val checker = makeChecker()
    val user = makeUser()
    val session = makeSession(user)

    val result = checker.checkTopicPosting(section)(using session)

    assert(result.restricted)
    assertEquals(result.reason, "только для модераторов")

  test("no-comments section: always restricted even for moderator"):
    val section = unrestrictedSection.copy(topicsRestriction = POSTSCORE_NO_COMMENTS)
    val checker = makeChecker()
    val user = makeUser(canmod = true)
    val session = makeSession(user, moderator = true)

    val result = checker.checkTopicPosting(section)(using session)

    assert(result.restricted)
    assertEquals(result.reason, "постинг запрещен")

  test("hide-comments section: always restricted even for moderator"):
    val section = unrestrictedSection.copy(topicsRestriction = POSTSCORE_HIDE_COMMENTS)
    val checker = makeChecker()
    val user = makeUser(canmod = true)
    val session = makeSession(user, moderator = true)

    val result = checker.checkTopicPosting(section)(using session)

    assert(result.restricted)
    assertEquals(result.reason, "постинг запрещен")

  test("score threshold 100: user with score >= 100 is permitted"):
    val section = unrestrictedSection.copy(topicsRestriction = 100)
    val checker = makeChecker()
    val user = makeUser(score = 100)
    val session = makeSession(user)

    val result = checker.checkTopicPosting(section)(using session)

    assert(result.permitted)

  test("score threshold 200: user with score < 200 is restricted"):
    val section = unrestrictedSection.copy(topicsRestriction = 200)
    val checker = makeChecker()
    val user = makeUser()
    val session = makeSession(user)

    val result = checker.checkTopicPosting(section)(using session)

    assert(result.restricted)

  test("score threshold 300: user with enough score is permitted"):
    val section = unrestrictedSection.copy(topicsRestriction = 300)
    val checker = makeChecker()
    val user = makeUser(score = 300)
    val session = makeSession(user)

    val result = checker.checkTopicPosting(section)(using session)

    assert(result.permitted)

  test("score threshold 500: user with score below threshold is restricted"):
    val section = unrestrictedSection.copy(topicsRestriction = 500)
    val checker = makeChecker()
    val user = makeUser(score = 400)
    val session = makeSession(user)

    val result = checker.checkTopicPosting(section)(using session)

    assert(result.restricted)

  test("custom score threshold (e.g. 45): user below is restricted"):
    val section = unrestrictedSection.copy(topicsRestriction = 45)
    val checker = makeChecker()
    val user = makeUser(score = 30)
    val session = makeSession(user)

    val result = checker.checkTopicPosting(section)(using session)

    assert(result.restricted)

  test("score threshold 100: anonymous session is restricted"):
    val section = unrestrictedSection.copy(topicsRestriction = 100)
    val anonymousUser = makeUser(anonymous = true)
    val checker = makeChecker()

    val result =
      checker.checkTopicPosting(section)(using
        NonAuthorizedSession(anonymousUser, ipBlockInfo = IpBlockInfo("127.0.0.1")))

    assert(result.restricted)

  test("frozen user is restricted"):
    val checker = makeChecker()
    val frozenTime = new Timestamp(System.currentTimeMillis() + 100000L)
    val user = makeUser(frozenUntil = frozenTime)
    val session = makeSession(user)

    val result = checker.checkTopicPosting(unrestrictedSection)(using session)

    assert(result.restricted)
    assertEquals(result.reason, "установлен режим только для чтения")

  // === checkTopicPosting(group) tests ===

  test("group: uses max of group and section restrictions"):
    val section = unrestrictedSection.copy(topicsRestriction = 100)
    val group = defaultGroup.copy(topicRestriction = POSTSCORE_REGISTERED_ONLY, sectionId = 1)
    val sectionService = mock(classOf[SectionService])
    when(sectionService.getSection(1)).thenReturn(section)
    val checker = makeChecker(sectionService = sectionService)
    val user = makeUser()
    val session = makeSession(user)

    val result = checker.checkTopicPosting(group)(using session)

    assert(result.restricted)

  test("group: unrestricted group with unrestricted section is permitted"):
    val section = unrestrictedSection.copy(topicsRestriction = POSTSCORE_UNRESTRICTED)
    val group = defaultGroup.copy(topicRestriction = POSTSCORE_UNRESTRICTED, sectionId = 1)
    val sectionService = mock(classOf[SectionService])
    when(sectionService.getSection(1)).thenReturn(section)
    val checker = makeChecker(sectionService = sectionService)
    val user = makeUser()
    val session = makeSession(user)

    val result = checker.checkTopicPosting(group)(using session)

    assert(result.permitted)

  test("group: section restriction dominates when higher"):
    val section = unrestrictedSection.copy(topicsRestriction = 200)
    val group = defaultGroup.copy(topicRestriction = 50, sectionId = 1)
    val sectionService = mock(classOf[SectionService])
    when(sectionService.getSection(1)).thenReturn(section)
    val checker = makeChecker(sectionService = sectionService)
    val user = makeUser(score = 100)
    val session = makeSession(user)

    val result = checker.checkTopicPosting(group)(using session)

    assert(result.restricted)
