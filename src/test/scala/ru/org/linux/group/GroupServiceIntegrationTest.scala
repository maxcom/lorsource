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

import org.junit.Assert.{assertEquals, assertNotNull, assertSame}
import org.junit.Test
import org.junit.runner.RunWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.annotation.{Bean, Configuration, ImportResource}
import org.springframework.test.context.ContextConfiguration
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner
import ru.org.linux.section.{Section, SectionScrollModeEnum}

import javax.sql.DataSource

@RunWith(classOf[SpringJUnit4ClassRunner])
@ContextConfiguration(classes = Array(classOf[GroupServiceIntegrationTestConfiguration]))
class GroupServiceIntegrationTest:
  @Autowired
  var groupService: GroupService = scala.compiletime.uninitialized

  @Test
  def testGetGroups(): Unit =
    val section = new Section("forum", false, false, Section.Forum, false, SectionScrollModeEnum.SECTION, 0, false)
    val groups = groupService.getGroups(section)
    assertEquals(16, groups.size)

  @Test
  def testGetGroupBySection(): Unit =
    val section = new Section("forum", false, false, Section.Forum, false, SectionScrollModeEnum.SECTION, 0, false)
    val group = groupService.getGroup(section, "general")
    assertNotNull(group)
    assertEquals("General", group.title)

  @Test
  def testGetGroupById(): Unit =
    val section = new Section("forum", false, false, Section.Forum, false, SectionScrollModeEnum.SECTION, 0, false)
    val group = groupService.getGroup(section, "general")
    val groupById = groupService.getGroup(group.id)
    assertNotNull(groupById)
    assertEquals(group.id, groupById.id)
    assertEquals(group.title, groupById.title)

  @Test
  def testCachingWorksOnGetGroupById(): Unit =
    val section = new Section("forum", false, false, Section.Forum, false, SectionScrollModeEnum.SECTION, 0, false)
    val group = groupService.getGroup(section, "general")

    // Вызываем дважды — второй раз должен прийти из кеша (тот же объект)
    val first = groupService.getGroup(group.id)
    val second = groupService.getGroup(group.id)
    assertSame(first, second)

  @Test
  def testGetGroupOptFound(): Unit =
    val section = new Section("forum", false, false, Section.Forum, false, SectionScrollModeEnum.SECTION, 0, false)
    val groupOpt = groupService.getGroupOpt(section, "general", false)
    assert(groupOpt.isDefined)
    assertEquals("General", groupOpt.get.title)

  @Test
  def testGetGroupOptNotFound(): Unit =
    val section = new Section("forum", false, false, Section.Forum, false, SectionScrollModeEnum.SECTION, 0, false)
    val groupOpt = groupService.getGroupOpt(section, "nonexistent-group-12345", false)
    assert(groupOpt.isEmpty)

@Configuration @ImportResource(Array("classpath:database.xml", "classpath:common.xml"))
class GroupServiceIntegrationTestConfiguration:
  @Bean
  def groupDao(dataSource: DataSource): GroupDao =
    val dao = new GroupDao()
    dao.setDateSource(dataSource)
    dao

  @Bean
  def groupService(groupDao: GroupDao): GroupService = new GroupService(groupDao)
