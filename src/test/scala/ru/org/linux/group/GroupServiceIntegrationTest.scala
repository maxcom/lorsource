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

import munit.FunSuite
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.annotation.{Bean, Configuration, ImportResource}
import org.springframework.test.context.ContextConfiguration
import ru.org.linux.scalikejdbc.SpringDB
import ru.org.linux.section.{Section, SectionScrollModeEnum}
import ru.org.linux.test.SpringTestSupport

@ContextConfiguration(classes = Array(classOf[GroupServiceIntegrationTestConfiguration]))
class GroupServiceIntegrationTest extends FunSuite with SpringTestSupport:
  @Autowired
  var groupService: GroupService = scala.compiletime.uninitialized

  test("getGroups"):
    val section = new Section("forum", false, false, Section.Forum, false, SectionScrollModeEnum.SECTION, 0, false)
    val groups = groupService.getGroups(section)
    assertEquals(groups.size, 16)

  test("getGroupBySection"):
    val section = new Section("forum", false, false, Section.Forum, false, SectionScrollModeEnum.SECTION, 0, false)
    val group = groupService.getGroup(section, "general")
    assert(group != null)
    assertEquals(group.title, "General")

  test("getGroupById"):
    val section = new Section("forum", false, false, Section.Forum, false, SectionScrollModeEnum.SECTION, 0, false)
    val group = groupService.getGroup(section, "general")
    val groupById = groupService.getGroup(group.id)
    assert(groupById != null)
    assertEquals(groupById.id, group.id)
    assertEquals(groupById.title, group.title)

  test("cachingWorksOnGetGroupById"):
    val section = new Section("forum", false, false, Section.Forum, false, SectionScrollModeEnum.SECTION, 0, false)
    val group = groupService.getGroup(section, "general")

    // Вызываем дважды — второй раз должен прийти из кеша (тот же объект)
    val first = groupService.getGroup(group.id)
    val second = groupService.getGroup(group.id)
    assert(second eq first)

  test("getGroupOptFound"):
    val section = new Section("forum", false, false, Section.Forum, false, SectionScrollModeEnum.SECTION, 0, false)
    val groupOpt = groupService.getGroupOpt(section, "general", false)
    assert(groupOpt.isDefined)
    assertEquals(groupOpt.get.title, "General")

  test("getGroupOptNotFound"):
    val section = new Section("forum", false, false, Section.Forum, false, SectionScrollModeEnum.SECTION, 0, false)
    val groupOpt = groupService.getGroupOpt(section, "nonexistent-group-12345", false)
    assert(groupOpt.isEmpty)

end GroupServiceIntegrationTest

@Configuration @ImportResource(Array("classpath:database.xml", "classpath:common.xml"))
class GroupServiceIntegrationTestConfiguration:
  @Bean
  def groupDao(springDB: SpringDB): GroupDao = new GroupDao(springDB)

  @Bean
  def groupService(groupDao: GroupDao): GroupService = new GroupService(groupDao)
