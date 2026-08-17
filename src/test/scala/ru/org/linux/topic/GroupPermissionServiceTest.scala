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

package ru.org.linux.topic

import munit.FunSuite
import org.mockito.ArgumentMatchers.anyString
import org.mockito.Mockito.{mock, when}
import ru.org.linux.auth.{AuthorizedSession, IpBlockInfo}
import ru.org.linux.group.GroupPermissionService
import ru.org.linux.section.{Section, SectionScrollModeEnum, SectionService}
import ru.org.linux.user.{Profile, User}

import java.sql.{ResultSet, Timestamp}
import java.util.{Calendar, Date}

class GroupPermissionServiceTest extends FunSuite:
  private def sessionOf(user: User): AuthorizedSession =
    AuthorizedSession(user, user.corrector, user.isModerator, user.isAdministrator,
      Profile.DEFAULT, IpBlockInfo("127.0.0.1"))

  private def createUser(id: Int, moderator: Boolean, corrector: Boolean, administrator: Boolean): User =
    User(nick = "testuser", id = id, canmod = moderator, candel = administrator, anonymous = false,
      corrector = corrector, blocked = false, password = "", score = 0, maxScore = 0,
      photo = null, email = null, fullName = null, unreadEvents = 0, frozenUntil = null,
      activated = true)

  private def mockResultSet(minutesAgo: Int, defaultInt: Int, defaultBool: Boolean,
                             section: Int, moderate: Boolean): ResultSet =
    val calendar = Calendar.getInstance()
    calendar.setTime(new Date())
    calendar.add(Calendar.MINUTE, -minutesAgo)
    val ts = new Timestamp(calendar.getTimeInMillis)

    val resultSet = mock(classOf[ResultSet])
    when(resultSet.getInt("postscore")).thenReturn(-9999)
    when(resultSet.wasNull()).thenReturn(false)
    when(resultSet.getTimestamp(anyString())).thenReturn(ts)
    when(resultSet.getInt(anyString())).thenReturn(defaultInt)
    when(resultSet.getString(anyString())).thenReturn("any")
    when(resultSet.getBoolean(anyString())).thenReturn(defaultBool)

    when(resultSet.getInt("section")).thenReturn(section)
    when(resultSet.getBoolean("moderate")).thenReturn(moderate)
    resultSet

  private def realSection(id: Int, premoderated: Boolean): Section =
    Section(name = "section-" + id, imagepost = true, premoderated = premoderated, id = id,
      votepoll = false, scrollMode = SectionScrollModeEnum.SECTION, topicsRestriction = 0,
      imageAllowed = true)

  /**
    * Проверка, что пользователь МОЖЕТ удалить топик, автором которого он является,
    * и прошло меньше часа с момента постинга
    */
  test("isDeletableByUserTest1"):
    val resultSet = mockResultSet(minutesAgo = 10, defaultInt = 13, defaultBool = false, section = 3,
      moderate = true)
    when(resultSet.getInt("stat1")).thenReturn(0)

    val user = createUser(13, moderator = false, corrector = false, administrator = false)

    val message = Topic.fromResultSet(resultSet)

    assert(!user.isModerator)
    assertEquals(user.id, resultSet.getInt("userid"))
    assertEquals(user.id, message.authorUserId)

    val sectionService = mock(classOf[SectionService])
    when(sectionService.getSection(3)).thenReturn(realSection(3, premoderated = false))

    val permissionService = new GroupPermissionService(sectionService, null, null, null)

    assert(permissionService.isDeletable(message)(using sessionOf(user)))

  /**
    * Проверка, что пользователь НЕ МОЖЕТ удалить топик, автором которого он является,
    * и прошло больше часа с момента постинга
    */
  test("isDeletableByUserTest2"):
    val resultSet = mockResultSet(minutesAgo = 70, defaultInt = 13, defaultBool = false, section = 3,
      moderate = true)

    val user = createUser(13, moderator = false, corrector = false, administrator = false)

    val message = Topic.fromResultSet(resultSet)

    assert(!user.isModerator)
    assertEquals(user.id, resultSet.getInt("userid"))
    assertEquals(user.id, message.authorUserId)

    val sectionService = mock(classOf[SectionService])
    when(sectionService.getSection(3)).thenReturn(realSection(3, premoderated = false))

    val permissionService = new GroupPermissionService(sectionService, null, null, null)

    assert(!permissionService.isDeletable(message)(using sessionOf(user)))

  /**
    * Проверка, что пользователь НЕ МОЖЕТ удалить топик, автором которого он не является,
    * и прошло больше часа с момента постинга
    */
  test("isDeletableByUserTest3"):
    val resultSet = mockResultSet(minutesAgo = 70, defaultInt = 13, defaultBool = false, section = 3,
      moderate = true)

    val user = createUser(14, moderator = false, corrector = false, administrator = false)

    val message = Topic.fromResultSet(resultSet)

    assert(!user.isModerator)
    assertNotEquals(user.id, resultSet.getInt("userid"))
    assertNotEquals(user.id, message.authorUserId)

    val sectionService = mock(classOf[SectionService])
    when(sectionService.getSection(3)).thenReturn(realSection(3, premoderated = false))

    val permissionService = new GroupPermissionService(sectionService, null, null, null)

    assert(!permissionService.isDeletable(message)(using sessionOf(user)))

  /**
    * Проверка, что пользователь НЕ МОЖЕТ удалить топик, автором которого он не является,
    * и прошло меньше часа с момента постинга
    */
  test("isDeletableByUserTest4"):
    val resultSet = mockResultSet(minutesAgo = 5, defaultInt = 13, defaultBool = false, section = 3,
      moderate = true)

    val user = createUser(14, moderator = false, corrector = false, administrator = false)

    val message = Topic.fromResultSet(resultSet)

    assert(!user.isModerator)
    assertNotEquals(user.id, resultSet.getInt("userid"))
    assertNotEquals(user.id, message.authorUserId)

    val sectionService = mock(classOf[SectionService])
    when(sectionService.getSection(3)).thenReturn(realSection(3, premoderated = false))

    val permissionService = new GroupPermissionService(sectionService, null, null, null)

    assert(!permissionService.isDeletable(message)(using sessionOf(user)))

  /** Проверка для модератора */
  test("isDeletableByModeratorTest"):
    val calendar = Calendar.getInstance()

    calendar.setTime(new Date())
    calendar.add(Calendar.MONTH, -2)
    val oldTime = calendar.getTimeInMillis

    calendar.setTime(new Date())
    calendar.add(Calendar.DAY_OF_MONTH, -2)
    val newTime = calendar.getTimeInMillis

    def mockFor(time: Long, defaultInt: Int, moderate: Boolean): ResultSet =
      val resultSet = mock(classOf[ResultSet])
      when(resultSet.getInt("postscore")).thenReturn(-9999)
      when(resultSet.wasNull()).thenReturn(false)
      when(resultSet.getTimestamp(anyString())).thenReturn(new Timestamp(time))
      when(resultSet.getInt(anyString())).thenReturn(defaultInt)
      when(resultSet.getString(anyString())).thenReturn("any")
      when(resultSet.getBoolean(anyString())).thenReturn(false)
      when(resultSet.getBoolean("moderate")).thenReturn(moderate)
      when(resultSet.getTimestamp("postdate")).thenReturn(new Timestamp(time))
      resultSet

    val resultSetModerateOld = mockFor(oldTime, defaultInt = 13, moderate = true)
    when(resultSetModerateOld.getInt("section")).thenReturn(1)

    val resultSetNotModerateOld = mockFor(oldTime, defaultInt = 2, moderate = false)

    val resultSetModerateNew = mockFor(newTime, defaultInt = 1, moderate = true)

    val resultSetNotModerateNew = mockFor(newTime, defaultInt = 1, moderate = false)

    val user = createUser(13, moderator = true, corrector = false, administrator = false)

    // проверка что данные в mock user верные
    assert(user.isModerator)

    val sectionService = mock(classOf[SectionService])
    when(sectionService.getSection(1)).thenReturn(realSection(1, premoderated = true))
    when(sectionService.getSection(2)).thenReturn(realSection(2, premoderated = false))

    val permissionService = new GroupPermissionService(sectionService, null, null, null)

    // проверка, что данные в mock resultSet верные
    assert(resultSetModerateNew.getBoolean("moderate"))
    assert(resultSetModerateOld.getBoolean("moderate"))
    assert(!resultSetNotModerateNew.getBoolean("moderate"))
    assert(!resultSetNotModerateOld.getBoolean("moderate"))

    assertEquals(0, new Timestamp(newTime).compareTo(resultSetModerateNew.getTimestamp("postdate")))
    assertEquals(0, new Timestamp(oldTime).compareTo(resultSetModerateOld.getTimestamp("postdate")))
    assertEquals(0, new Timestamp(newTime).compareTo(resultSetNotModerateNew.getTimestamp("postdate")))
    assertEquals(0, new Timestamp(oldTime).compareTo(resultSetNotModerateOld.getTimestamp("postdate")))

    val messageModerateOld = Topic.fromResultSet(resultSetModerateOld)
    val messageNotModerateOld = Topic.fromResultSet(resultSetNotModerateOld)
    val messageModerateNew = Topic.fromResultSet(resultSetModerateNew)
    val messageNotModerateNew = Topic.fromResultSet(resultSetNotModerateNew)

    // проверка, что данные в mock message верные
    assert(messageModerateNew.commited)
    assert(messageModerateOld.commited)
    assert(!messageNotModerateNew.commited)
    assert(!messageNotModerateOld.commited)

    assertEquals(0, new Timestamp(newTime).compareTo(messageModerateNew.postdate))
    assertEquals(0, new Timestamp(oldTime).compareTo(messageModerateOld.postdate))
    assertEquals(0, new Timestamp(newTime).compareTo(messageNotModerateNew.postdate))
    assertEquals(0, new Timestamp(oldTime).compareTo(messageNotModerateOld.postdate))

    // нельзя удалять старые подтверждённые топики в премодерируемом разделе
    assert(!permissionService.isDeletable(messageModerateOld)(using sessionOf(user)))
    // можно удалять старые не подтверждённые топики в премодерируемом разделе
    assert(permissionService.isDeletable(messageNotModerateOld)(using sessionOf(user)))

    // можно удалять новые подтверждённые топики в премодерируемом разделе
    assert(permissionService.isDeletable(messageModerateNew)(using sessionOf(user)))
    // можно удалять новые не подтверждённые топики в премодерируемом разделе
    assert(permissionService.isDeletable(messageNotModerateNew)(using sessionOf(user)))