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

class UserPermissionServiceTest extends FunSuite:
  test("shouldNotifyLogin: false for low-score user without roles"):
    val user = User.fromResultSet(Users.getUser45Score())
    assertEquals(UserPermissionService.shouldNotifyLogin(user), false)

  test("shouldNotifyLogin: true for score >= 500"):
    val user = User.fromResultSet(Users.getUser5star())
    assertEquals(UserPermissionService.shouldNotifyLogin(user), true)

  test("shouldNotifyLogin: true for moderator with low score"):
    val user = User.fromResultSet(Users.getModeratorLowScore())
    assertEquals(UserPermissionService.shouldNotifyLogin(user), true)

  test("shouldNotifyLogin: true for administrator with low score"):
    val user = User.fromResultSet(Users.getAdministratorLowScore())
    assertEquals(UserPermissionService.shouldNotifyLogin(user), true)

  test("shouldNotifyLogin: true for corrector with low score"):
    val user = User.fromResultSet(Users.getCorrector())
    assertEquals(UserPermissionService.shouldNotifyLogin(user), true)

  test("shouldNotifyLogin: false for anonymous"):
    val user = User.fromResultSet(Users.getAnonymous())
    assertEquals(UserPermissionService.shouldNotifyLogin(user), false)