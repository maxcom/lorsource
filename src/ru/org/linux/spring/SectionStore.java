/*
 * Copyright 1998-2010 Linux.org.ru
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

package ru.org.linux.spring;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.ResultSet;
import java.sql.Statement;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;

import ru.org.linux.site.Section;
import ru.org.linux.site.SectionNotFoundException;
import ru.org.linux.site.LorDataSource;

public class SectionStore {
  private final ImmutableMap<Integer, Section> sections;
  private final ImmutableList<Section> sectionsList;

  public SectionStore() throws SQLException {
    Connection db = LorDataSource.getConnection();

    try {
      ImmutableMap.Builder<Integer, Section> sections = ImmutableMap.builder();
      ImmutableList.Builder<Section> sectionsList = ImmutableList.builder();

      Statement st = db.createStatement();

      ResultSet rs = st.executeQuery("SELECT id, name, imagepost, vote, moderate FROM sections ORDER BY id");

      while (rs.next()) {
        Section section = new Section(rs);

        sections.put(section.getId(), section);
        sectionsList.add(section);
      }

      this.sections = sections.build();
      this.sectionsList = sectionsList.build();
    } finally {
      db.close();
    }
  }

  public Section getSection(int id) throws SectionNotFoundException {
    Section section = sections.get(id);

    if (section==null) {
      throw new SectionNotFoundException(id);
    }

    return section;
  }

  public ImmutableList<Section> getSectionsList() {
    return sectionsList;
  }
}
