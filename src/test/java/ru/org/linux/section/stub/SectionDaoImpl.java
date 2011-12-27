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

public class SectionDaoImpl implements SectionDao {

  @Override
  public List<Section> getAllSections() {
    List<Section> sectionList = new ArrayList<Section>();
    Section section = new Section();
    section.setId(1);
    section.setName("Section 1");
    section.setModerate(true);
    section.setVotepoll(false);
    section.setImagepost(false);
    sectionList.add(section);

    section = new Section();
    section.setId(2);
    section.setName("Section 2");
    section.setModerate(true);
    section.setVotepoll(false);
    section.setImagepost(false);
    sectionList.add(section);

    section = new Section();
    section.setId(3);
    section.setName("Section 3");
    section.setModerate(true);
    section.setVotepoll(false);
    section.setImagepost(true);
    sectionList.add(section);

    section = new Section();
    section.setId(4);
    section.setName("Section 4");
    section.setModerate(false);
    section.setVotepoll(false);
    section.setImagepost(false);
    sectionList.add(section);

    section = new Section();
    section.setId(5);
    section.setName("Section 5");
    section.setModerate(false);
    section.setVotepoll(true);
    section.setImagepost(false);
    sectionList.add(section);

    return sectionList;
  }

  @Override
  public String getAddInfo(int id) {
    return new StringBuilder().append("Extended info for Section ").append(id).toString();
  }

}
