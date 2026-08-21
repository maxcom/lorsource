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
import org.mockito.Mockito.mock
import ru.org.linux.group.GroupService
import ru.org.linux.msgbase.DeleteInfoDao
import ru.org.linux.section.SectionService
import ru.org.linux.topic.TopicPermissionService.*
import ru.org.linux.user.User

import java.sql.Timestamp

class PostScoreCheckerTest extends FunSuite:

  private val checker = new PostScoreChecker(
    mock(classOf[SectionService]), mock(classOf[GroupService]), mock(classOf[DeleteInfoDao]))

  private def makeUser(
      id: Int = 1,
      canmod: Boolean = false,
      anonymous: Boolean = false,
      blocked: Boolean = false,
      score: Int = 50,
      maxScore: Int = 50,
      frozenUntil: Timestamp = null): User =
    User(
      nick = "testuser",
      id = id,
      canmod = canmod,
      candel = false,
      anonymous = anonymous,
      corrector = false,
      blocked = blocked,
      password = "password",
      score = score,
      maxScore = maxScore,
      photo = null,
      email = null,
      fullName = null,
      unreadEvents = 0,
      frozenUntil = frozenUntil,
      activated = true)

  // === POSTSCORE_MOD_AUTHOR (moderator bypass restoration) ===

  test("MOD_AUTHOR: moderator who is not the author is permitted"):
    val mod = makeUser(canmod = true, score = 0)

    val result = checker.postScoreCheckerChain(mod, POSTSCORE_MOD_AUTHOR, byAuthor = false).seal

    assert(result.permitted)

  test("MOD_AUTHOR: non-moderator non-author is restricted"):
    val user = makeUser(score = 9999)

    val result = checker.postScoreCheckerChain(user, POSTSCORE_MOD_AUTHOR, byAuthor = false).seal

    assert(result.restricted)
    assertEquals(result.reason, "только для модераторов и автора")

  test("MOD_AUTHOR: author (non-moderator) is permitted"):
    val author = makeUser(score = 0)

    val result = checker.postScoreCheckerChain(author, POSTSCORE_MOD_AUTHOR, byAuthor = true).seal

    assert(result.permitted)

  // === score-based (100-500) moderator bypass restoration ===

  test("score 100: moderator with insufficient score is permitted"):
    val mod = makeUser(canmod = true, score = 0)

    val result = checker.postScoreCheckerChain(mod, 100, byAuthor = false).seal

    assert(result.permitted)

  test("score 500: moderator with insufficient score is permitted"):
    val mod = makeUser(canmod = true, score = 0)

    val result = checker.postScoreCheckerChain(mod, 500, byAuthor = false).seal

    assert(result.permitted)

  test("score 100: non-moderator with insufficient score is restricted"):
    val user = makeUser(score = 50)

    val result = checker.postScoreCheckerChain(user, 100, byAuthor = false).seal

    assert(result.restricted)

  test("score 500: non-moderator with sufficient score is permitted"):
    val user = makeUser(score = 500)

    val result = checker.postScoreCheckerChain(user, 500, byAuthor = false).seal

    assert(result.permitted)

  // === bypass must NOT apply to NO_COMMENTS / HIDE_COMMENTS ===

  test("NO_COMMENTS: moderator is restricted"):
    val mod = makeUser(canmod = true, score = 0)

    val result = checker.postScoreCheckerChain(mod, POSTSCORE_NO_COMMENTS, byAuthor = false).seal

    assert(result.restricted)

  test("HIDE_COMMENTS: moderator is restricted"):
    val mod = makeUser(canmod = true, score = 0)

    val result = checker.postScoreCheckerChain(mod, POSTSCORE_HIDE_COMMENTS, byAuthor = false).seal

    assert(result.restricted)

  // === MODERATORS_ONLY: moderator permitted, non-moderator restricted ===

  test("MODERATORS_ONLY: moderator is permitted"):
    val mod = makeUser(canmod = true, score = 0)

    val result = checker.postScoreCheckerChain(mod, POSTSCORE_MODERATORS_ONLY, byAuthor = false).seal

    assert(result.permitted)
