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
import ru.org.linux.test.TransactionalTestSupport

@ContextConfiguration(classes = Array(classOf[GroupDaoIntegrationTestConfiguration]))
class GroupDaoIntegrationTest extends FunSuite with TransactionalTestSupport:

  @Autowired
  var groupDao: GroupDao = scala.compiletime.uninitialized

  test("getGroupById"):
    val group = groupDao.getGroup(126)
    assert(group != null)
    assertEquals(group.title, "General")
    assertEquals(group.id, 126)

  test("getGroupByIdNotFound"):
    intercept[GroupNotFoundException] {
      groupDao.getGroup(99999)
    }

  test("getGroups"):
    val section = new Section("forum", false, false, Section.Forum, false, SectionScrollModeEnum.SECTION, 0, false)
    val groups = groupDao.getGroups(section)
    assert(groups.nonEmpty, "Should have groups in forum section")

  test("getGroupByName"):
    val section = new Section("forum", false, false, Section.Forum, false, SectionScrollModeEnum.SECTION, 0, false)
    val group = groupDao.getGroup(section, "general")
    assert(group != null)
    assertEquals(group.title, "General")

  test("getGroupOptFound"):
    val section = new Section("forum", false, false, Section.Forum, false, SectionScrollModeEnum.SECTION, 0, false)
    val groupOpt = groupDao.getGroupOpt(section, "general", false)
    assert(groupOpt.isDefined, "Should find group")
    assertEquals(groupOpt.get.title, "General")

  test("getGroupOptNotFound"):
    val section = new Section("forum", false, false, Section.Forum, false, SectionScrollModeEnum.SECTION, 0, false)
    val groupOpt = groupDao.getGroupOpt(section, "nonexistent-group-12345", false)
    assert(groupOpt.isEmpty, "Should not find group")

end GroupDaoIntegrationTest

@Configuration @ImportResource(Array("classpath:database.xml", "classpath:common.xml"))
class GroupDaoIntegrationTestConfiguration:
  @Bean
  def groupDao(springDB: SpringDB): GroupDao = new GroupDao(springDB)
end GroupDaoIntegrationTestConfiguration
