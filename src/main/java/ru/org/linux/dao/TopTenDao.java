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

package ru.org.linux.dao;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.simple.SimpleJdbcTemplate;

import org.springframework.stereotype.Repository;
import ru.org.linux.dto.SectionDto;
import ru.org.linux.dto.TopTenMessageDto;

@Repository
public class TopTenDao {
  private SimpleJdbcTemplate jdbcTemplate;

  public SimpleJdbcTemplate getJdbcTemplate() {
    return jdbcTemplate;
  }

  @Autowired
  public void setJdbcTemplate(SimpleJdbcTemplate jdbcTemplate) {
    this.jdbcTemplate = jdbcTemplate;
  }


  /**
   *
   * @return
   */
  public List<TopTenMessageDto> getMessages() {
    String sql =
        "select topics.id as msgid, groups.urlname, groups.section, topics.title, lastmod, topics.stat1 as c  " +
            "from topics " +
            "join groups on groups.id = topics.groupid" +
            " where topics.postdate>(CURRENT_TIMESTAMP-'1 month 1 day'::interval) and not deleted and notop is null " +
            " and groupid!=8404 and groupid!=4068 order by c desc, msgid limit 10";
    return jdbcTemplate.query(sql, new RowMapper<TopTenMessageDto>() {
      @Override
      public TopTenMessageDto mapRow(ResultSet rs, int i) throws SQLException {
        TopTenMessageDto result = new TopTenMessageDto();
        result.setUrl(SectionDto.getSectionLink(rs.getInt("section")) + rs.getString("urlname") + '/' + rs.getInt("msgid"));
        result.setTitle(rs.getString("title"));
        result.setLastmod(rs.getTimestamp("lastmod"));
        result.setAnswers(rs.getInt("c"));
        return result;
      }
    });

  }


}
