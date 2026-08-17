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

package ru.org.linux.user

import munit.FunSuite
import ru.org.linux.test.Users

/** Unit Tests для User */
class UserTest extends FunSuite:
  test("maxcom: проверка администратора"):
    val resultSet = Users.getMaxcom()
    val user = User.fromResultSet(resultSet)

    assertEquals(resultSet.getInt("id"), user.id)
    assertEquals(resultSet.getString("nick"), user.nick)
    assertEquals("tango", resultSet.getString("style"))
    assert(!user.blocked)
    assert(user.isModerator)
    assert(user.isAdministrator)
    assert(!user.canCorrect)
    assert(!user.anonymous)
    assertEquals(resultSet.getInt("score"), user.getScore)
    assertEquals("<span class=\"stars\">★★★★★</span>", user.getStatus)
    assert(user.activated)
    assert(!user.isAnonymousScore)
    assertEquals(resultSet.getBoolean("corrector"), user.corrector)
    assertEquals(resultSet.getString("email"), user.email)
    assert(user.hasEmail)
    assertEquals(resultSet.getString("name"), user.getName)
    assert(!user.isFrozen)

  test("anonymous: проверка анонимуса"):
    val resultSet = Users.getAnonymous()
    val user = User.fromResultSet(resultSet)

    assertEquals(resultSet.getInt("id"), user.id)
    assertEquals(resultSet.getString("nick"), user.nick)
    assertEquals("tango", resultSet.getString("style"))
    assert(!user.blocked)
    assert(!user.isModerator)
    assert(!user.isAdministrator)
    assert(!user.canCorrect)
    assert(user.anonymous)
    assertEquals(0, user.getScore)
    assertEquals("анонимный", user.getStatus)
    assert(user.activated)
    assert(user.isAnonymousScore)
    assertEquals(resultSet.getBoolean("corrector"), user.corrector)
    assertEquals(resultSet.getString("email"), user.email)
    assert(!user.hasEmail)
    assertEquals(resultSet.getString("name"), user.getName)
    assert(!user.isFrozen)

  test("svu: проверка модератора"):
    val resultSet = Users.getModerator()
    val user = User.fromResultSet(resultSet)

    assertEquals(resultSet.getInt("id"), user.id)
    assertEquals(resultSet.getString("nick"), user.nick)
    assertEquals("tango", resultSet.getString("style"))
    assert(!user.blocked)
    assert(user.isModerator)
    assert(!user.isAdministrator)
    assert(!user.canCorrect)
    assert(!user.anonymous)
    assertEquals(resultSet.getInt("score"), user.getScore)
    assertEquals("<span class=\"stars\">★★★★★</span>", user.getStatus)
    assert(user.activated)
    assert(!user.isAnonymousScore)
    assertEquals(resultSet.getBoolean("corrector"), user.corrector)
    assertEquals(resultSet.getString("email"), user.email)
    assert(!user.hasEmail)
    assertEquals(resultSet.getString("name"), user.getName)
    assert(!user.isFrozen)

  test("user5star: проверка для пользователя с 5-ю звёздами"):
    val resultSet = Users.getUser5star()
    val user = User.fromResultSet(resultSet)

    assertEquals(resultSet.getInt("id"), user.id)
    assertEquals(resultSet.getString("nick"), user.nick)
    assertEquals("tango", resultSet.getString("style"))
    assert(!user.blocked)
    assert(!user.isModerator)
    assert(!user.isAdministrator)
    assert(!user.canCorrect)
    assert(!user.anonymous)
    assertEquals(resultSet.getInt("score"), user.getScore)
    assertEquals("<span class=\"stars\">★★★★★</span>", user.getStatus)
    assert(user.activated)
    assert(!user.isAnonymousScore)
    assertEquals(resultSet.getBoolean("corrector"), user.corrector)
    assertEquals(resultSet.getString("email"), user.email)
    assert(!user.hasEmail)
    assertEquals(resultSet.getString("name"), user.getName)
    assert(!user.isFrozen)

  test("user1star: проверка для пользователя с 1-ой звездой"):
    val resultSet = Users.getUser1star()
    val user = User.fromResultSet(resultSet)

    assertEquals(resultSet.getInt("id"), user.id)
    assertEquals(resultSet.getString("nick"), user.nick)
    assertEquals("tango", resultSet.getString("style"))
    assert(!user.blocked)
    assert(!user.isModerator)
    assert(!user.isAdministrator)
    assert(!user.canCorrect)
    assert(!user.anonymous)
    assertEquals(resultSet.getInt("score"), user.getScore)
    assertEquals("<span class=\"stars\">★</span>", user.getStatus)
    assert(user.activated)
    assert(!user.isAnonymousScore)
    assertEquals(resultSet.getBoolean("corrector"), user.corrector)
    assertEquals(resultSet.getString("email"), user.email)
    assert(!user.hasEmail)
    assertEquals(resultSet.getString("name"), user.getName)
    assert(!user.isFrozen)

  test("user45score: проверка для пользователя с < 50 score"):
    val resultSet = Users.getUser45Score()
    val user = User.fromResultSet(resultSet)

    assertEquals(resultSet.getInt("id"), user.id)
    assertEquals(resultSet.getString("nick"), user.nick)
    assertEquals("tango", resultSet.getString("style"))
    assert(!user.blocked)
    assert(!user.isModerator)
    assert(!user.isAdministrator)
    assert(!user.canCorrect)
    assert(!user.anonymous)
    assertEquals(resultSet.getInt("score"), user.getScore)
    assertEquals("анонимный", user.getStatus)
    assert(user.activated)
    assert(user.isAnonymousScore)
    assertEquals(resultSet.getBoolean("corrector"), user.corrector)
    assertEquals(resultSet.getString("email"), user.email)
    assert(!user.hasEmail)
    assertEquals(resultSet.getString("name"), user.getName)
    assert(!user.isFrozen)

  test("userBlocked: проверка для заблокированного пользователя с < 50 score"):
    val resultSet = Users.getUser45ScoreBlocked()
    val user = User.fromResultSet(resultSet)

    assertEquals(resultSet.getInt("id"), user.id)
    assertEquals(resultSet.getString("nick"), user.nick)
    assertEquals("tango", resultSet.getString("style"))
    assert(user.blocked)
    assert(!user.isModerator)
    assert(!user.isAdministrator)
    assert(!user.canCorrect)
    assert(!user.anonymous)
    assertEquals(resultSet.getInt("score"), user.getScore)
    assertEquals("анонимный", user.getStatus)
    assert(user.activated)
    assert(user.isAnonymousScore)
    assertEquals(resultSet.getBoolean("corrector"), user.corrector)
    assertEquals(resultSet.getString("email"), user.email)
    assert(!user.hasEmail)
    assertEquals(resultSet.getString("name"), user.getName)
    assert(!user.isFrozen)

  test("userDefrosed: проверка размороженного пользователя"):
    val resultSet = Users.getUserDefrosted()
    val user = User.fromResultSet(resultSet)

    assertEquals(resultSet.getInt("id"), user.id)
    assertEquals(resultSet.getString("nick"), user.nick)
    assertEquals("tango", resultSet.getString("style"))
    assert(!user.blocked)
    assert(!user.isModerator)
    assert(!user.isAdministrator)
    assert(!user.canCorrect)
    assert(!user.anonymous)
    assertEquals(resultSet.getInt("score"), user.getScore)
    assertEquals("<span class=\"stars\">★</span>", user.getStatus)
    assert(user.activated)
    assert(!user.isAnonymousScore)
    assertEquals(resultSet.getBoolean("corrector"), user.corrector)
    assertEquals(resultSet.getString("email"), user.email)
    assert(!user.hasEmail)
    assertEquals(resultSet.getString("name"), user.getName)
    assert(!user.isFrozen)

  test("userFrozen: проверка замороженного пользователя"):
    val resultSet = Users.getUserFrozen()
    val user = User.fromResultSet(resultSet)

    assertEquals(resultSet.getInt("id"), user.id)
    assertEquals(resultSet.getString("nick"), user.nick)
    assertEquals("tango", resultSet.getString("style"))
    assert(!user.blocked)
    assert(!user.isModerator)
    assert(!user.isAdministrator)
    assert(!user.canCorrect)
    assert(!user.anonymous)
    assertEquals(resultSet.getInt("score"), user.getScore)
    assertEquals("<span class=\"stars\">★</span>", user.getStatus)
    assert(user.activated)
    assert(!user.isAnonymousScore)
    assertEquals(resultSet.getBoolean("corrector"), user.corrector)
    assertEquals(resultSet.getString("email"), user.email)
    assert(!user.hasEmail)
    assertEquals(resultSet.getString("name"), user.getName)
    assert(user.isFrozen)
    assertEquals(resultSet.getTimestamp("frozen_until"), user.frozenUntil)