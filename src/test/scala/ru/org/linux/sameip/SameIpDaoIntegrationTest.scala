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

package ru.org.linux.sameip

import org.junit.{Assert, Test}
import org.junit.runner.RunWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.test.context.{ContextConfiguration, ContextHierarchy}
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner
import org.springframework.transaction.annotation.Transactional
import ru.org.linux.scalikejdbc.SpringDB

@RunWith(classOf[SpringJUnit4ClassRunner])
@ContextHierarchy(
  Array(
    new ContextConfiguration(value = Array("classpath:database.xml")),
    new ContextConfiguration(classes = Array(classOf[SameIpDaoIntegrationTestConfiguration]))
  )) @Transactional
class SameIpDaoIntegrationTest:
  @Autowired
  var sameIpDao: SameIpDao = scala.compiletime.uninitialized

  @Test
  def testGetCommentsNoFilters(): Unit =
    val result = sameIpDao.getComments(ip = None, userAgent = None, score = None, limit = 10)
    Assert.assertNotNull(result)

  @Test
  def testGetCommentsWithIpFilter(): Unit =
    val result = sameIpDao.getComments(ip = Some("127.0.0.1"), userAgent = None, score = None, limit = 10)
    Assert.assertNotNull(result)
