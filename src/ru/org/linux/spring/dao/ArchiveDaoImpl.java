/*
 * Copyright 1998-2009 Linux.org.ru
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

package ru.org.linux.spring.dao;

import java.util.List;
import java.util.HashMap;
import java.sql.ResultSet;
import java.sql.SQLException;

import org.springframework.jdbc.core.simple.SimpleJdbcTemplate;
import org.springframework.jdbc.core.simple.ParameterizedRowMapper;

/**
 * User: sreentenko
 * Date: 01.05.2009
 * Time: 23:34:51
 */
public class ArchiveDaoImpl {

  private SimpleJdbcTemplate jdbcTemplate;

  public SimpleJdbcTemplate getJdbcTemplate() {
    return jdbcTemplate;
  }

  public void setJdbcTemplate(SimpleJdbcTemplate jdbcTemplate) {
    this.jdbcTemplate = jdbcTemplate;
  }

  public List<ArchiveDTO> getArchiveDTO(){
    String sql = "select year, month, c from monthly_stats where section=1" +
      " order by year desc, month desc limit 13";
    return jdbcTemplate.query(sql, new ParameterizedRowMapper<ArchiveDTO>() {
      public ArchiveDTO mapRow(ResultSet rs, int rowNum) throws SQLException {
        ArchiveDTO dto = new ArchiveDTO();
        dto.setYear(rs.getInt("year"));
        dto.setMonth(rs.getInt("month"));
        dto.setCount(rs.getInt("c"));
        return dto;
      }
    }, new HashMap());
  }

  public static class ArchiveDTO{
    int year;
    int month;
    int count;

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
  }
}
