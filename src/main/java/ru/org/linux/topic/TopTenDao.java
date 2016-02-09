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

package ru.org.linux.topic;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import ru.org.linux.section.SectionService;

import javax.sql.DataSource;
import java.sql.Timestamp;
import java.util.List;

@Repository
public class TopTenDao {
  @Autowired
  private SectionService sectionService;

  private JdbcTemplate jdbcTemplate;

  @Autowired
  public void setDataSource(DataSource dataSource) {
    jdbcTemplate = new JdbcTemplate(dataSource);
  }

  public List<TopTenMessageDTO> getMessages(){
    String sql =
      "select topics.id as msgid, groups.urlname, groups.section, topics.title, lastmod, topics.stat1 as c  " +
        "from topics " +
        "join groups on groups.id = topics.groupid" +
      " where topics.postdate>(CURRENT_TIMESTAMP-'1 month 1 day'::interval) and not deleted and not notop " +
      " and groupid!=8404 and groupid!=4068 and groupid!=19390 order by c desc, msgid limit 10";

    return jdbcTemplate.query(sql, (rs, i) -> {
      return new TopTenMessageDTO(
              sectionService.getSection(rs.getInt("section")).getSectionLink()+rs.getString("urlname")+ '/' +rs.getInt("msgid"),
              rs.getString("title"),
              rs.getTimestamp("lastmod"),
              rs.getInt("c")
      );
    });
  }

  public static class TopTenMessageDTO {
    private final String url;
    private final Timestamp lastmod;
    private final String title;
    private Integer pages;
    private final int commentCount;

    public TopTenMessageDTO(String url, String title, Timestamp lastmod, int commentCount) {
      this.url = url;
      this.title = title;
      this.lastmod = lastmod;
      this.commentCount = commentCount;
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

    public Integer getPages() {
      return pages;
    }

    public void setPages(Integer pages) {
      this.pages = pages;
    }

    public int getCommentCount() {
      return commentCount;
    }
  }
}
