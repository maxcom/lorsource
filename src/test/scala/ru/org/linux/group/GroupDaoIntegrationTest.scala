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

import org.junit.Test
import org.junit.runner.RunWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.test.context.ContextConfiguration
import org.springframework.test.context.junit4.SpringRunner
import ru.org.linux.section.{Section, SectionScrollModeEnum}

import scala.jdk.CollectionConverters._
import org.junit.Assert.assertEquals

@RunWith(classOf[SpringRunner])
@ContextConfiguration(Array("integration-tests-context.xml"))
class GroupDaoIntegrationTest {
  @Autowired
  private val groupService: GroupService = null

  @Test
  def groupsTest(): Unit = {
    val sectionDto = new Section("forum", false, false, Section.Forum, false, SectionScrollModeEnum.SECTION, 0, false)

    val groupDtoList = groupService.getGroups(sectionDto)
    assertEquals(16, groupDtoList.size)

    val groupDto = groupService.getGroup(sectionDto, "general")
    assertEquals("General", groupDto.title)
  }
}