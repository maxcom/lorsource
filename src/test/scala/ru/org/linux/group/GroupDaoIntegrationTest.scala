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
package ru.org.linux.group

import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.annotation.{Bean, Configuration, ImportResource}
import org.springframework.test.context.ContextConfiguration
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner
import org.springframework.transaction.annotation.Transactional
import ru.org.linux.scalikejdbc.SpringDB
import ru.org.linux.section.{Section, SectionScrollModeEnum}

@RunWith(classOf[SpringJUnit4ClassRunner])
@ContextConfiguration(classes = Array(classOf[GroupDaoIntegrationTestConfiguration])) @Transactional
class GroupDaoIntegrationTest:

  @Autowired
  var groupDao: GroupDao = scala.compiletime.uninitialized

  @Test
  def testGetGroupById(): Unit =
    val group = groupDao.getGroup(126)
    assertNotNull(group)
    assertEquals("General", group.title)
    assertEquals(126, group.id)

  @Test
  def testGetGroupByIdNotFound(): Unit = assertThrows(classOf[GroupNotFoundException], () => groupDao.getGroup(99999))

  @Test
  def testGetGroups(): Unit =
    val section = new Section("forum", false, false, Section.Forum, false, SectionScrollModeEnum.SECTION, 0, false)
    val groups = groupDao.getGroups(section)
    assertTrue("Should have groups in forum section", groups.nonEmpty)

  @Test
  def testGetGroupByName(): Unit =
    val section = new Section("forum", false, false, Section.Forum, false, SectionScrollModeEnum.SECTION, 0, false)
    val group = groupDao.getGroup(section, "general")
    assertNotNull(group)
    assertEquals("General", group.title)

  @Test
  def testGetGroupOptFound(): Unit =
    val section = new Section("forum", false, false, Section.Forum, false, SectionScrollModeEnum.SECTION, 0, false)
    val groupOpt = groupDao.getGroupOpt(section, "general", false)
    assertTrue("Should find group", groupOpt.isDefined)
    assertEquals("General", groupOpt.get.title)

  @Test
  def testGetGroupOptNotFound(): Unit =
    val section = new Section("forum", false, false, Section.Forum, false, SectionScrollModeEnum.SECTION, 0, false)
    val groupOpt = groupDao.getGroupOpt(section, "nonexistent-group-12345", false)
    assertTrue("Should not find group", groupOpt.isEmpty)

end GroupDaoIntegrationTest

@Configuration @ImportResource(Array("classpath:database.xml", "classpath:common.xml"))
class GroupDaoIntegrationTestConfiguration:
  @Bean
  def groupDao(springDB: SpringDB): GroupDao = new GroupDao(springDB)
end GroupDaoIntegrationTestConfiguration
