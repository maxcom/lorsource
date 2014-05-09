/*
 * Copyright 1998-2014 Linux.org.ru
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
import java.io.Serializable;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

@Repository
public class ArchiveDao {
  private JdbcTemplate jdbcTemplate;

  @Autowired
  public void setDataSource(DataSource dataSource) {
    jdbcTemplate = new JdbcTemplate(dataSource);
  }

  public List<ArchiveDTO> getArchiveDTO(Section section, Group group) {
    return getArchiveInternal(section, group, 0);
  }

  public List<ArchiveDTO> getArchiveDTO(Section section, int limit) {
    return getArchiveInternal(section, null, limit);
  }

  private List<ArchiveDTO> getArchiveInternal(final Section section, final Group group, int limit) {
    RowMapper<ArchiveDTO> mapper = new RowMapper<ArchiveDTO>() {
      @Override
      public ArchiveDTO mapRow(ResultSet rs, int rowNum) throws SQLException {
        ArchiveDTO dto = new ArchiveDTO();
        dto.setYear(rs.getInt("year"));
        dto.setMonth(rs.getInt("month"));
        dto.setCount(rs.getInt("c"));
        dto.setSection(section);
        dto.setGroup(group);
        return dto;
      }
    };

    if (limit > 0) {
      return jdbcTemplate.query(
              "select year, month, c from monthly_stats where section=? and groupid is null" +
                      " order by year desc, month desc limit ?",
              mapper,
              section.getId(),
              limit
      );
    } else {
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
  }

  public static class ArchiveDTO implements Serializable {
    private int year;
    private int month;
    private int count;
    private Section section;
    private Group group;

    private static final long serialVersionUID = 5862774559965251295L;

    public int getYear() {
      return year;
    }

    public void setYear(int year) {
      this.year = year;
    }

    public int getMonth() {
      return month;
    }

    public void setMonth(int month) {
      this.month = month;
    }

    public int getCount() {
      return count;
    }

    public void setCount(int count) {
      this.count = count;
    }

    public Section getSection() {
      return section;
    }

    public void setSection(Section section) {
      this.section = section;
    }

    public Group getGroup() {
      return group;
    }

    public void setGroup(Group group) {
      this.group = group;
    }

    public String getLink() {
      if (group!=null) {
        return group.getArchiveLink(year, month);
      }
      return section.getArchiveLink(year, month);
    }
  }
}
