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

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.HashMap;
import java.util.List;
import java.io.Serializable;

import org.springframework.jdbc.core.simple.ParameterizedRowMapper;
import org.springframework.jdbc.core.simple.SimpleJdbcTemplate;

/**
 * User: rsvato
 * Date: May 4, 2009
 * Time: 1:56:22 PM
 */
public class TopTenDaoImpl {
  private SimpleJdbcTemplate jdbcTemplate;

  public SimpleJdbcTemplate getJdbcTemplate() {
    return jdbcTemplate;
  }

  public void setJdbcTemplate(SimpleJdbcTemplate jdbcTemplate) {
    this.jdbcTemplate = jdbcTemplate;
  }


  public List<TopTenMessageDTO> getMessages(){
    String sql = "select topics.id as msgid, topics.title, lastmod, stat1 as c, top10.mess_order from topics left outer join top10 " +
      " on topics.id = top10.msgid " +
      " where topics.postdate>(CURRENT_TIMESTAMP-'1 month 1 day'::interval) and not deleted and notop is null " +
      " and groupid!=8404 and groupid!=4068 order by c desc, msgid limit 10";
    return getJdbcTemplate().query(sql, new ParameterizedRowMapper<TopTenMessageDTO>() {
      public TopTenMessageDTO mapRow(ResultSet rs, int i) throws SQLException {
        TopTenMessageDTO result = new TopTenMessageDTO();
        result.setMsgid(rs.getInt("msgid"));
        result.setTitle(rs.getString("title"));
        result.setLastmod(rs.getTimestamp("lastmod"));
        result.setAnswers(rs.getInt("c"));
        Integer savedOrder = rs.getInt("mess_order");
        if (rs.wasNull()){
          savedOrder = null;
        }
        result.setMovedUp(savedOrder == null || (i + 1) > savedOrder);
        return result;
      }
    }, new HashMap());

  }


  public static class TopTenMessageDTO implements Serializable{
    private Integer msgid;
    private Timestamp lastmod;
    private String title;
    private boolean movedUp = false;
    private Integer pages;
    private Integer answers;
    private static final long serialVersionUID = 166352344159392938L;

    public Integer getMsgid() {
      return msgid;
    }

    public void setMsgid(Integer msgid) {
      this.msgid = msgid;
    }

    public Timestamp getLastmod() {
      return lastmod;
    }

    public void setLastmod(Timestamp lastmod) {
      this.lastmod = lastmod;
    }

    public String getTitle() {
      return title;
    }

    public void setTitle(String title) {
      this.title = title;
    }

    public boolean getMovedUp() {
      return movedUp;
    }

    public void setMovedUp(boolean movedUp) {
      this.movedUp = movedUp;
    }

    public Integer getPages() {
      return pages;
    }

    public void setPages(Integer pages) {
      this.pages = pages;
    }

    public Integer getAnswers() {
      return answers;
    }

    public void setAnswers(Integer answers) {
      this.answers = answers;
    }
  }
}
