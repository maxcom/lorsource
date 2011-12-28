/*
 * Copyright 1998-2011 Linux.org.ru
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
package ru.org.linux.section.stub;

import ru.org.linux.section.Section;
import ru.org.linux.section.SectionDao;

import java.util.ArrayList;
import java.util.List;

public class TestSectionDaoImpl implements SectionDao {

  @Override
  public List<Section> getAllSections() {
    List<Section> sectionList = new ArrayList<Section>();

    sectionList.add(new Section("Section 1", false, true, 1, false, "SECTION"));
    sectionList.add(new Section("Section 2", false, true, 2, false, "GROUP"));
    sectionList.add(new Section("Section 3", true, true, 3, false, "SECTION"));
    sectionList.add(new Section("Section 4", false, false, 4, false, "NO_SCROLL"));
    sectionList.add(new Section("Section 5", false, false, 5, true, "SECTION"));

    return sectionList;
  }

  @Override
  public String getAddInfo(int id) {
    return new StringBuilder().append("Extended info for Section ").append(id).toString();
  }
}
