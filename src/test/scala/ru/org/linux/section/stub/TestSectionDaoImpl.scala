/*
 * Copyright 1998-2024 Linux.org.ru
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
package ru.org.linux.section.stub

import ru.org.linux.section.{Section, SectionDao}
import ru.org.linux.topic.TopicPermissionService

class TestSectionDaoImpl extends SectionDao {
  override def getAllSections = {
    Seq(
      new Section("Section 1", false, true, 1, false, "SECTION", TopicPermissionService.POSTSCORE_UNRESTRICTED),
      new Section("Section 2", false, true, 2, false, "GROUP", TopicPermissionService.POSTSCORE_UNRESTRICTED),
      new Section("Section 3", true, true, 3, false, "SECTION", TopicPermissionService.POSTSCORE_UNRESTRICTED),
      new Section("Section 5", false, false, 5, true, "SECTION", TopicPermissionService.POSTSCORE_UNRESTRICTED))
  }
}