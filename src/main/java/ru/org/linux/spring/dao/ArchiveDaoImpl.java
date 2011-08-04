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

package ru.org.linux.spring.dao;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import javax.sql.DataSource;
import java.io.Serializable;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

@Repository
public class ArchiveDaoImpl {
  private JdbcTemplate jdbcTemplate;

  @Autowired
  public void setDataSource(DataSource dataSource) {
    this.jdbcTemplate = new JdbcTemplate(dataSource);
  }

  public List<ArchiveDTO> getArchiveDTO() {
    return jdbcTemplate.query(
            "select year, month, c from monthly_stats where section=1 and groupid is null" +
                    " order by year desc, month desc limit 13",
            new RowMapper<ArchiveDTO>() {
              @Override
              public ArchiveDTO mapRow(ResultSet rs, int rowNum) throws SQLException {
                ArchiveDTO dto = new ArchiveDTO();
                dto.setYear(rs.getInt("year"));
                dto.setMonth(rs.getInt("month"));
                dto.setCount(rs.getInt("c"));
                return dto;
              }
            });
  }

  public static class ArchiveDTO implements Serializable {
    private int year;
    private int month;
    private int count;
    private static final long serialVersionUID = 5862774559965251294L;

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
