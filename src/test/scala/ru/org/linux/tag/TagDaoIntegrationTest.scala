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

package ru.org.linux.tag

import munit.FunSuite
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.test.context.{ContextConfiguration, ContextHierarchy}
import ru.org.linux.scalikejdbc.SpringDB
import ru.org.linux.test.TransactionalTestSupport

@ContextHierarchy(
  Array(
    new ContextConfiguration(value = Array("classpath:database.xml")),
    new ContextConfiguration(classes = Array(classOf[TagIntegrationTestConfiguration]))))
class TagDaoIntegrationTest extends FunSuite with TransactionalTestSupport:

  @Autowired
  var tagDao: TagDao = scala.compiletime.uninitialized

  @Autowired
  var springDB: SpringDB = scala.compiletime.uninitialized

  test("tagNotFound"):
    val fetch = tagDao.getTagId("fdsfsdfdsfsdfs", false)
    assert(fetch.isEmpty)

  test("createAndGetTest"):
    val id = springDB.localTx {
      tagDao.createTag("test-tag")
    }
    val fetchId = tagDao.getTagId("test-tag", false)
    assertEquals(fetchId, Some(id))

  test("prefixSearchExactTest"):
    springDB.localTx {
      tagDao.createTag("zest")
    }
    springDB.localTx {
      tagDao.createTag("zesd")
    }

    val tags = tagDao.getTagsByPrefix("zest", 0)
    assertEquals(tags.size, 1)

  test("prefixTopSearchExactTest"):
    springDB.localTx {
      tagDao.createTag("zest")
    }
    springDB.localTx {
      tagDao.createTag("zesd")
    }

    val tags = tagDao.getTopTagsByPrefix("zest", 0, 20)
    assertEquals(tags.size, 1)

  test("prefixSearchSimpleTest"):
    val zest = springDB.localTx {
      tagDao.createTag("zest")
    }
    val zesd = springDB.localTx {
      tagDao.createTag("zesd")
    }

    val tags = tagDao.getTagsByPrefix("ze", 0)
    assertEquals(tags.size, 2)
    assertEquals(tags(0).id, zesd)
    assertEquals(tags(1).id, zest)

  test("prefixSearchEscapeTest"):
    springDB.localTx {
      tagDao.createTag("zestxtest")
    }

    assertEquals(tagDao.getTagsByPrefix("zest_", 0).size, 0)
    assertEquals(tagDao.getTagsByPrefix("zest%", 0).size, 0)
