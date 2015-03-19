/*
 * Copyright 1998-2015 Linux.org.ru
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

package ru.org.linux.section;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowCallbackHandler;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

@Component
public class SectionDaoImpl implements SectionDao {
  private JdbcTemplate jdbcTemplate;

  @Autowired
  public void setDataSource(DataSource ds) {
    jdbcTemplate = new JdbcTemplate(ds);
  }

  @Override
  public List<Section> getAllSections() {
    final List<Section> sectionList = new ArrayList<>();
    jdbcTemplate.query("SELECT id, name, imagepost, imageallowed, vote, moderate, scroll_mode, restrict_topics FROM sections ORDER BY id",
      new RowCallbackHandler() {
        @Override
        public void processRow(ResultSet rs) throws SQLException {
          Section section = new Section(rs);
          sectionList.add(section);
        }
      });
    return sectionList;
  }

  @Override
  public String getAddInfo(int id) {
    List<String> infos = jdbcTemplate.queryForList("select add_info from sections where id=?", String.class, id);

    if (infos.isEmpty()) {
      return null;
    } else {
      return infos.get(0);
    }
  }
}
