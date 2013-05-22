/*
 * Copyright 1998-2012 Linux.org.ru
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
import ru.org.linux.section.SectionService;

import java.io.Serializable;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.List;

/**
 */
@Repository
public class LastMiniNewsDao {
  @Autowired
  private SectionService sectionService;

  @Autowired
  private JdbcTemplate jdbcTemplate;

  public List<LastMiniNews> getTopics(final int perPage) {
    String sql =
        "select topics.id as msgid, groups.urlname, groups.section, topics.title, lastmod, topics.stat1 as c  " +
            "from topics " +
            "join groups on groups.id = topics.groupid" +
            " where " +
            "  topics.postdate>(CURRENT_TIMESTAMP-'1 month 1 day'::interval) and " + // За последнйи месяйц
            "  not deleted and " +                                                   // Неудаленные
            "  groups.section = 1 and " +                                            // Новости
            "  minor order by topics.postdate desc limit 10";                        // 10 штук

    return jdbcTemplate.query(sql, new RowMapper<LastMiniNews>() {
      @Override
      public LastMiniNews mapRow(ResultSet rs, int i) throws SQLException {
        final int answers = rs.getInt("c");
        final int answers0 = (answers == 0) ? 1 : answers;
        final int tmp = answers0 / perPage;
        final int pages = (answers0 % perPage > 0) ? tmp + 1 : tmp;
        LastMiniNews result = new LastMiniNews(
            sectionService.getSection(rs.getInt("section")).getSectionLink()+rs.getString("urlname")+ '/' +rs.getInt("msgid"),
            rs.getTimestamp("lastmod"),
            rs.getString("title"),
            answers,
            pages
        );
        return result;
      }
    });

  }


  public static class LastMiniNews implements Serializable {
    private final String url;
    private final Timestamp lastmod;
    private final String title;
    private final int answers;
    private final int pages;

    public LastMiniNews(String url, Timestamp lastmod, String title, int answers, int pages) {
      this.url = url;
      this.lastmod = lastmod;
      this.title = title;
      this.answers = answers;
      this.pages = pages;
    }

    public String getUrl() {
      return url;
    }

    public Timestamp getLastmod() {
      return lastmod;
    }

    public String getTitle() {
      return title;
    }

    public Integer getAnswers() {
      return answers;
    }

    public int getPages() {
      return pages;
    }
  }


}
