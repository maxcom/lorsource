/*
 * Copyright 1998-2017 Linux.org.ru
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

package ru.org.linux.topic;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;
import ru.org.linux.group.Group;
import ru.org.linux.section.Section;

import javax.sql.DataSource;
import java.util.List;

@Repository
public class ArchiveDao {
  private JdbcTemplate jdbcTemplate;

  @Autowired
  public void setDataSource(DataSource dataSource) {
    jdbcTemplate = new JdbcTemplate(dataSource);
  }

  public List<ArchiveStats> getArchiveStats(Section section, Group group) {
    RowMapper<ArchiveStats> mapper = mapper(section, group);

    if (group == null) {
      return jdbcTemplate.query(
              "select year, month, c from monthly_stats where section=? and groupid is null" +
                      " order by year, month",
              mapper,
              section.getId()
      );
    } else {
      return jdbcTemplate.query(
              "select year, month, c from monthly_stats where section=? and groupid=? order by year, month",
              mapper,
              section.getId(),
              group.getId()
      );
    }
  }

  private RowMapper<ArchiveStats> mapper(final Section section, final Group group) {
    return (rs, rowNum) -> new ArchiveStats(section, group, rs.getInt("year"), rs.getInt("month"), rs.getInt("c"));
  }

  public int getArchiveCount(int groupid, int year, int month) {
    List<Integer> res = jdbcTemplate.queryForList("SELECT c FROM monthly_stats WHERE groupid=? AND year=? AND month=?", Integer.class, groupid, year, month);

    if (!res.isEmpty()) {
      return res.get(0);
    } else {
      return 0;
    }
  }

  public static class ArchiveStats {
    private final int year;
    private final int month;
    private final int count;
    private final Section section;
    private final Group group;

    public ArchiveStats(Section section, Group group, int year, int month, int count) {
      this.year = year;
      this.month = month;
      this.count = count;
      this.section = section;
      this.group = group;
    }

    public int getYear() {
      return year;
    }

    public int getMonth() {
      return month;
    }

    public int getCount() {
      return count;
    }

    public String getLink() {
      if (group!=null) {
        return group.getArchiveLink(year, month);
      }
      return section.getArchiveLink(year, month);
    }
  }
}
