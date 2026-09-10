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
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.test.context.{ContextConfiguration, ContextHierarchy}
import ru.org.linux.scalikejdbc.SpringDB
import ru.org.linux.test.TransactionalTestSupport
import scalikejdbc.*

object UserTagDaoIntegrationTest

@ContextHierarchy(
  Array(
    new ContextConfiguration(value = Array("classpath:database.xml")),
    new ContextConfiguration(classes = Array(classOf[SimpleIntegrationTestConfiguration]))))
class UserTagDaoIntegrationTest extends FunSuite with TransactionalTestSupport:
  @Autowired
  var userTagDao: UserTagDao = scala.compiletime.uninitialized

  @Autowired
  var springDB: SpringDB = scala.compiletime.uninitialized

  private var user1Id: Int = scala.compiletime.uninitialized
  private var user2Id: Int = scala.compiletime.uninitialized

  private var tag1Id: Int = scala.compiletime.uninitialized
  private var tag2Id: Int = scala.compiletime.uninitialized
  private var tag3Id: Int = scala.compiletime.uninitialized
  private var tag4Id: Int = scala.compiletime.uninitialized
  private var tag5Id: Int = scala.compiletime.uninitialized

  private def createUser(userName: String): Int =
    springDB.run:
      val userid = sql"select nextval('s_uid') as userid".map(rs => rs.int("userid")).single.apply().get
      sql"INSERT INTO users (id, name, nick) VALUES ($userid, $userName, $userName)".update.apply()
      userid

  private def createTag(tagName: String): Int =
    springDB.run:
      sql"INSERT INTO tags_values (value) VALUES ($tagName) RETURNING id".map(rs => rs.int("id")).single.apply().get

  private def countFavoriteByUser(userId: Int): Int =
    springDB.run:
      sql"SELECT count(user_id) FROM user_tags WHERE is_favorite=true AND user_id=$userId"
        .map(rs => rs.int(1))
        .single
        .apply()
        .get

  private def countIgnoreByUser(userId: Int): Int =
    springDB.run:
      sql"SELECT count(user_id) FROM user_tags WHERE is_favorite=false AND user_id=$userId"
        .map(rs => rs.int(1))
        .single
        .apply()
        .get

  private def countByTagId(tagId: Int): Int =
    springDB.run:
      sql"SELECT count(user_id) FROM user_tags WHERE tag_id=$tagId".map(rs => rs.int(1)).single.apply().get

  override def beforeEach(context: BeforeEach): Unit =
    super.beforeEach(context)
    user1Id = createUser("UserTagDaoIntegrationTest_user1")
    user2Id = createUser("UserTagDaoIntegrationTest_user2")

    tag1Id = createTag("UserTagDaoIntegrationTest_tag1")
    tag2Id = createTag("UserTagDaoIntegrationTest_tag2")
    tag3Id = createTag("UserTagDaoIntegrationTest_tag3")
    tag4Id = createTag("UserTagDaoIntegrationTest_tag4")
    tag5Id = createTag("UserTagDaoIntegrationTest_tag5")

  private def prepareUserTags(): Unit =
    springDB.localTx:
      userTagDao.addTag(user1Id, tag1Id, true)
      userTagDao.addTag(user2Id, tag1Id, true)
      userTagDao.addTag(user1Id, tag2Id, true)
      userTagDao.addTag(user1Id, tag2Id, false)
      userTagDao.addTag(user2Id, tag2Id, true)
      userTagDao.addTag(user2Id, tag3Id, true)
      userTagDao.addTag(user1Id, tag3Id, true)
      userTagDao.addTag(user2Id, tag4Id, true)
      userTagDao.addTag(user1Id, tag4Id, true)
      userTagDao.addTag(user1Id, tag5Id, false)
      userTagDao.addTag(user2Id, tag5Id, true)
      userTagDao.addTag(user1Id, tag5Id, true)

  test("addTest"):
    prepareUserTags()

    springDB.localTx {
      userTagDao.addTag(user1Id, tag1Id, false)
    }

    assertEquals(countFavoriteByUser(user1Id), 5, "Wrong count of user tags.")
    assertEquals(countIgnoreByUser(user1Id), 3, "Wrong count of user tags.")
    assertEquals(countFavoriteByUser(user2Id), 5, "Wrong count of user tags.")
    assertEquals(countIgnoreByUser(user2Id), 0, "Wrong count of user tags.")
    assertEquals(countByTagId(tag1Id), 3, "Wrong count of user tags.")

  test("deleteOneTest"):
    prepareUserTags()

    springDB.localTx {
      userTagDao.deleteTag(user1Id, tag1Id, true)
    }
    springDB.localTx {
      userTagDao.deleteTag(user1Id, tag2Id, true)
    }

    assertEquals(countFavoriteByUser(user1Id), 3, "Wrong count of user tags.")

    springDB.localTx {
      userTagDao.deleteTag(user1Id, tag2Id, false)
    }

    assertEquals(countFavoriteByUser(user1Id), 3, "Wrong count of user tags.")
    assertEquals(countIgnoreByUser(user1Id), 1, "Wrong count of user tags.")

  test("deleteAllTest"):
    prepareUserTags()

    springDB.localTx {
      userTagDao.deleteTags(tag2Id)
    }

    assertEquals(countFavoriteByUser(user1Id), 4, "Wrong count of user tags.")
    assertEquals(countIgnoreByUser(user1Id), 1, "Wrong count of user tags.")
    assertEquals(countFavoriteByUser(user2Id), 4, "Wrong count of user tags.")

  test("getTest"):
    prepareUserTags()

    var tags = userTagDao.getTags(user1Id, true)
    assertEquals(tags.size, 5, "Wrong count of user tags.")

    tags = userTagDao.getTags(user1Id, false)
    assertEquals(tags.size, 2, "Wrong count of user tags.")

  test("getUserIdListByTagsTest"):
    prepareUserTags()
    var userIdList = userTagDao.getUserIdListByTags(user1Id, Seq(tag1Id))
    assertEquals(userIdList.size, 1, "Wrong count of user ID's.")

    userIdList = userTagDao.getUserIdListByTags(user1Id, Seq(tag1Id, tag2Id))
    assertEquals(userIdList.size, 1, "Wrong count of user ID's.")

    springDB.localTx {
      userTagDao.deleteTag(user1Id, tag5Id, true)
    }
    userIdList = userTagDao.getUserIdListByTags(user1Id, Seq(tag5Id))
    assertEquals(userIdList.size, 1, "Wrong count of user ID's.")

  test("replaceTagTest"):
    prepareUserTags()

    springDB.localTx {
      userTagDao.replaceTag(tag2Id, tag1Id)
    }
    assertEquals(countByTagId(tag1Id), 2, "Wrong count of user tags.")

    springDB.localTx {
      userTagDao.deleteTags(tag1Id)
    }
    springDB.localTx {
      userTagDao.replaceTag(tag2Id, tag1Id)
    }
    assertEquals(countByTagId(tag1Id), 3, "Wrong count of user tags.")

end UserTagDaoIntegrationTest
