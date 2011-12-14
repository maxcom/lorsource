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

package ru.org.linux.archive;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;
import ru.org.linux.group.Group;
import ru.org.linux.section.Section;

import javax.sql.DataSource;
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

  /**
   * Получить список архивов по секции и группе.
   *
   * @param section секция сообщений
   * @param group   группа сообщений
   * @return список архивов
   */
  public List<ArchiveDto> getArchiveDto(Section section, Group group) {
    return getArchiveInternal(section, group, 0);
  }

  /**
   * Получить список архивов по секции с ограничением на количество архивов.
   *
   * @param section секция сообщений
   * @param limit   количество архивов
   * @return список архивов
   */
  public List<ArchiveDto> getArchiveDto(Section section, int limit) {
    return getArchiveInternal(section, null, limit);
  }

  /**
   * Получить список архивов по секции и группе с ограничением на количество архивов.
   *
   * @param section секция сообщений
   * @param group   группа сообщений
   * @param limit   количество архивов
   * @return список архивов
   */
  private List<ArchiveDto> getArchiveInternal(final Section section, final Group group, int limit) {
    RowMapper<ArchiveDto> mapper = new RowMapper<ArchiveDto>() {
      @Override
      public ArchiveDto mapRow(ResultSet rs, int rowNum) throws SQLException {
        ArchiveDto dto = new ArchiveDto();
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
}
