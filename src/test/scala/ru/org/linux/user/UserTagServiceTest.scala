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
import org.mockito.ArgumentMatchers.{any, anyBoolean, anyString, eq as eqTo}
import org.mockito.Mockito.*
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.dao.DuplicateKeyException
import org.springframework.test.context.ContextConfiguration
import ru.org.linux.tag.{TagDao, TagNotFoundException, TagService}
import ru.org.linux.test.SpringTestSupport

import java.sql.{ResultSet, SQLException}
import scala.jdk.CollectionConverters.*

@ContextConfiguration(Array("unit-tests-context.xml"))
class UserTagServiceTest extends FunSuite with SpringTestSupport:
  @Autowired
  var tagDao: TagDao = scala.compiletime.uninitialized

  @Autowired
  var tagService: TagService = scala.compiletime.uninitialized

  @Autowired
  var userTagDao: UserTagDao = scala.compiletime.uninitialized

  @Autowired
  var userTagService: UserTagService = scala.compiletime.uninitialized

  private var user: User = scala.compiletime.uninitialized

  override def beforeEach(context: BeforeEach): Unit =
    super.beforeEach(context)
    reset(userTagDao)
    reset(tagService)

    when(tagService.getTagId(eqTo("tag1"), anyBoolean())).thenReturn(2)
    when(tagService.getTagIdOptWithSynonym(eqTo("tag1"))).thenReturn(Some(2))
    user = getUser(1)

  private def getUser(id: Int): User =
    val rs = mock(classOf[ResultSet])
    try
      when(rs.getInt("id")).thenReturn(id)
      User.fromResultSet(rs)
    catch
      case _: SQLException =>
        null

  test("favoriteAddTest"):
    when(tagDao.getTagId("tag1", false)).thenReturn(Some(2))
    userTagService.favoriteAdd(user, "tag1")
    verify(userTagDao).addTag(eqTo(1), eqTo(2), eqTo(true))(using any())

  test("favoriteDelTest"):
    userTagService.favoriteDel(user, "tag1")
    verify(userTagDao).deleteTag(eqTo(1), eqTo(2), eqTo(true))(using any())

  test("ignoreAddTest"):
    userTagService.ignoreAdd(user, "tag1")
    verify(userTagDao).addTag(eqTo(1), eqTo(2), eqTo(false))(using any())

  test("ignoreDelTest"):
    userTagService.ignoreDel(user, "tag1")
    verify(userTagDao).deleteTag(eqTo(1), eqTo(2), eqTo(false))(using any())

  test("favoritesGetTest"):
    val etalon = List("tag1")
    when(userTagDao.getTags(1, true)).thenReturn(etalon)

    val actual = userTagService.favoritesGet(user)
    assertEquals(actual.size, etalon.size)
    assertEquals(actual.get(0), etalon.head)

  test("ignoresGetTest"):
    val etalon = List("tag1")
    when(userTagDao.getTags(1, false)).thenReturn(etalon)

    val actual = userTagService.ignoresGet(user)
    assertEquals(actual.size, etalon.size)
    assertEquals(actual.get(0), etalon.head)

  test("getUserIdListByTagsTest"):
    val etalon = List(123)
    when(userTagDao.getUserIdListByTags(1, Seq(2))).thenReturn(etalon)

    val actual = userTagService.getUserIdListByTagsJava(user.id, List("tag1").asJava)
    assertEquals(actual.size, etalon.size)
    assertEquals(actual.get(0).intValue(), etalon.head)

  test("addMultiplyTagsTest"):
    val mockUserTagService = mock(classOf[UserTagService])
    when(mockUserTagService.addMultiplyTags(any(classOf[User]), anyString, anyBoolean)).thenCallRealMethod()
    try
      doThrow(new TagNotFoundException()).when(mockUserTagService).favoriteAdd(eqTo(user), eqTo("uytutut"))
      doThrow(new DuplicateKeyException("duplicate")).when(mockUserTagService).favoriteAdd(eqTo(user), eqTo("tag3"))
    catch
      case _: Exception =>

    var strErrors = mockUserTagService.addMultiplyTags(user, "tag1, tag2, tag3, uytutut, @#$%$#", true)
    try
      verify(mockUserTagService).favoriteAdd(eqTo(user), eqTo("tag1"))
      verify(mockUserTagService).favoriteAdd(eqTo(user), eqTo("tag2"))
      verify(mockUserTagService).favoriteAdd(eqTo(user), eqTo("uytutut"))
      verify(mockUserTagService, never()).favoriteAdd(eqTo(user), eqTo("@#$%$#"))
      verify(mockUserTagService, never()).ignoreAdd(any(classOf[User]), anyString)
    catch
      case _: Exception =>
    assertEquals(strErrors.size, 3)

    reset(mockUserTagService)
    when(mockUserTagService.addMultiplyTags(any(classOf[User]), anyString, anyBoolean)).thenCallRealMethod()
    try
      doThrow(new TagNotFoundException()).when(mockUserTagService).ignoreAdd(eqTo(user), eqTo("uytutut"))
      doThrow(new DuplicateKeyException("duplicate")).when(mockUserTagService).ignoreAdd(eqTo(user), eqTo("tag3"))
    catch
      case _: Exception =>

    strErrors = mockUserTagService.addMultiplyTags(user, "tag1, tag2, tag3, uytutut, @#$%$#", false)
    try
      verify(mockUserTagService).ignoreAdd(eqTo(user), eqTo("tag1"))
      verify(mockUserTagService).ignoreAdd(eqTo(user), eqTo("tag2"))
      verify(mockUserTagService).ignoreAdd(eqTo(user), eqTo("uytutut"))
      verify(mockUserTagService, never()).ignoreAdd(eqTo(user), eqTo("@#$%$#"))
      verify(mockUserTagService, never()).favoriteAdd(any(classOf[User]), anyString)
    catch
      case _: Exception =>
    assertEquals(strErrors.size, 3)

end UserTagServiceTest
