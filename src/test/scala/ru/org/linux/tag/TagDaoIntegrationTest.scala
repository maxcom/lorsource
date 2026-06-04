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

import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.test.context.ContextConfiguration
import org.springframework.test.context.ContextHierarchy
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner
import org.springframework.transaction.annotation.Transactional

@RunWith(classOf[SpringJUnit4ClassRunner])
@ContextHierarchy(
  Array(
    new ContextConfiguration(value = Array("classpath:database.xml")),
    new ContextConfiguration(classes = Array(classOf[TagIntegrationTestConfiguration])))) @Transactional
class TagDaoIntegrationTest:

  @Autowired
  var tagDao: TagDao = scala.compiletime.uninitialized

  @Test
  def testTagNotFound(): Unit =
    val fetch = tagDao.getTagId("fdsfsdfdsfsdfs", false)
    assertTrue(fetch.isEmpty)

  @Test
  def createAndGetTest(): Unit =
    val id = tagDao.createTag("test-tag")
    val fetchId = tagDao.getTagId("test-tag", false)
    assertEquals(Some(id), fetchId)

  @Test
  def prefixSearchExactTest(): Unit =
    tagDao.createTag("zest")
    tagDao.createTag("zesd")

    val tags = tagDao.getTagsByPrefix("zest", 0)
    assertEquals(1, tags.size)

  @Test
  def prefixTopSearchExactTest(): Unit =
    tagDao.createTag("zest")
    tagDao.createTag("zesd")

    val tags = tagDao.getTopTagsByPrefix("zest", 0, 20)
    assertEquals(1, tags.size)

  @Test
  def prefixSearchSimpleTest(): Unit =
    val zest = tagDao.createTag("zest")
    val zesd = tagDao.createTag("zesd")

    val tags = tagDao.getTagsByPrefix("ze", 0)
    assertEquals(2, tags.size)
    assertEquals(zesd, tags(0).id)
    assertEquals(zest, tags(1).id)

  @Test
  def prefixSearchEscapeTest(): Unit =
    tagDao.createTag("zestxtest")

    assertEquals(0, tagDao.getTagsByPrefix("zest_", 0).size)
    assertEquals(0, tagDao.getTagsByPrefix("zest%", 0).size)
