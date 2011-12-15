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

package ru.org.linux.topic;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.ConnectionCallback;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowCallbackHandler;
import org.springframework.stereotype.Repository;
import ru.org.linux.site.UserErrorException;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;
import java.util.regex.Pattern;

@Repository
public final class TagDao {
  private static final Pattern tagRE = Pattern.compile("([\\p{L}\\d \\+-]+)", Pattern.CASE_INSENSITIVE);

  private static final int TOP_TAGS_COUNT = 50;

  private static final String queryAllTags = "SELECT counter,value FROM tags_values WHERE counter>0";

  private JdbcTemplate jdbcTemplate;

  @Autowired
  public void setDataSource(DataSource ds) {
    jdbcTemplate = new JdbcTemplate(ds);
  }

  private static synchronized int getOrCreateTag(Connection con, String tag) throws SQLException {
    PreparedStatement st2 = con.prepareStatement("SELECT id FROM tags_values WHERE value=?");
    st2.setString(1,tag);
    ResultSet rs = st2.executeQuery();
    int id;

    if (rs.next()) {
      id = rs.getInt("id");
    } else {
      PreparedStatement st = con.prepareStatement("INSERT INTO tags_values (value) VALUES(?)");
      st.setString(1,tag);
      st.executeUpdate();
      st.close();
      
      rs = st2.executeQuery();
      rs.next();
      id = rs.getInt("id");
    }

    rs.close();
    st2.close();

    return id;
  }

  public ImmutableList<String> getMessageTags(int msgid) {
    final ImmutableList.Builder<String> tags = ImmutableList.builder();

    jdbcTemplate.query(
            "SELECT tags_values.value FROM tags, tags_values WHERE tags.msgid=? AND tags_values.id=tags.tagid ORDER BY value",
            new RowCallbackHandler() {
              @Override
              public void processRow(ResultSet rs) throws SQLException {
                tags.add(rs.getString("value"));
              }
            },
            msgid
    );

    return tags.build();
  }

  public static String toString(Collection<String> tags) {
    if (tags.isEmpty()) {
      return "";
    }

    StringBuilder str = new StringBuilder();

    for (String tag : tags) {
      str.append(str.length() > 0 ? "," : "").append(tag);
    }

    return str.toString();
  }

  public SortedSet<String> getTopTags()  {
    final SortedSet<String> set = new TreeSet<String>();

    jdbcTemplate.query(
            "SELECT counter,value FROM tags_values WHERE counter>1 ORDER BY counter DESC LIMIT " + TOP_TAGS_COUNT,
            new RowCallbackHandler() {
              @Override
              public void processRow(ResultSet rs) throws SQLException {
                set.add(rs.getString("value"));
              }
            }
    );

    return set;
  }

  /**
   * Получить все тэги со счетчиком
   * @return список всех тегов
   */
  public Map<String,Integer> getAllTags() {
    final ImmutableMap.Builder<String, Integer> builder = ImmutableMap.builder();
    jdbcTemplate.query(queryAllTags, new RowCallbackHandler() {
      @Override
      public void processRow(ResultSet resultSet) throws SQLException {
        builder.put(resultSet.getString("value"), resultSet.getInt("counter"));
      }
    });
    return builder.build();
  }

  public static void checkTag(String tag) throws UserErrorException {
    // обработка тега: только буквы/цифры/пробелы, никаких спецсимволов, запятых, амперсандов и <>
    if (!isGoodTag(tag)) {
      throw new UserErrorException("Некорректный тег: '"+tag+ '\'');
    }
  }

  private static boolean isGoodTag(String tag) {
    return tagRE.matcher(tag).matches();
  }

  public void updateCounters(final List<String> oldTags, final List<String> newTags) {
    jdbcTemplate.execute(new ConnectionCallback<String>() {
      @Override
      public String doInConnection(Connection con) throws SQLException, DataAccessException {
        PreparedStatement stInc = con.prepareStatement("UPDATE tags_values SET counter=counter+1 WHERE id=?");
        PreparedStatement stDec = con.prepareStatement("UPDATE tags_values SET counter=counter-1 WHERE id=?");

        for (String tag : newTags) {
          if (!oldTags.contains(tag)) {
            int id = getOrCreateTag(con, tag);
            stInc.setInt(1, id);
            stInc.executeUpdate();
          }
        }

        for (String tag : oldTags) {
          if (!newTags.contains(tag)) {
            int id = getOrCreateTag(con, tag);
            stDec.setInt(1, id);
            stDec.executeUpdate();
          }
        }
        return null;
      }
    });
  }

  public static ImmutableList<String> parseTags(String tags) throws UserErrorException {
    Set<String> tagSet = new HashSet<String>();

    // Теги разделяютчя пайпом или запятой
    tags = tags.replaceAll("\\|",",");
    String [] tagsArr = tags.split(",");

    if (tagsArr.length==0) {
      return ImmutableList.of();
    }

    for (String aTagsArr : tagsArr) {
      String tag = StringUtils.stripToNull(aTagsArr.toLowerCase());
      // плохой тег - выбрасываем
      if (tag == null) {
        continue;
      }

      // обработка тега: только буквы/цифры/пробелы, никаких спецсимволов, запятых, амперсандов и <>
      checkTag(tag);

      tagSet.add(tag);
    }

    return ImmutableList.copyOf(tagSet);
  }

  public static ImmutableList<String> parseSanitizeTags(String tags) {
    if (tags==null) {
      return ImmutableList.of();
    }

    Set<String> tagSet = new HashSet<String>();

    // Теги разделяютчя пайпом или запятой
    tags = tags.replaceAll("\\|",",");
    String [] tagsArr = tags.split(",");

    if (tagsArr.length==0) {
      return ImmutableList.of();
    }

    for (String aTagsArr : tagsArr) {
      String tag = StringUtils.stripToNull(aTagsArr.toLowerCase());
      // плохой тег - выбрасываем
      if (tag == null) {
        continue;
      }

      // обработка тега: только буквы/цифры/пробелы, никаких спецсимволов, запятых, амперсандов и <>
      if (isGoodTag(tag)) {
        tagSet.add(tag);
      }
    }

    return ImmutableList.copyOf(tagSet);
  }

  // TODO: move to JSP
  public static String getEditTags(Collection<String> tags) {
    StringBuilder out = new StringBuilder();
    boolean first = true;

    for (String tag : tags) {
      if (!first) {
        out.append(", ");
      }
      out.append("<a onclick=\"addTag('").append(tag).append("')\">");
      out.append(tag);
      out.append("</a>");
      first = false;
    }

    return out.toString();
  }

  public boolean updateTags(final int msgid, final List<String> tagList) {
    final List<String> oldTags = getMessageTags(msgid);

    return jdbcTemplate.execute(new ConnectionCallback<Boolean>() {
      @Override
      public Boolean doInConnection(Connection con) throws SQLException, DataAccessException {

        PreparedStatement insertStatement = con.prepareStatement("INSERT INTO tags VALUES(?,?)");
        PreparedStatement deleteStatement = con.prepareStatement("DELETE FROM tags WHERE msgid=? and tagid=?");

        insertStatement.setInt(1, msgid);
        deleteStatement.setInt(1, msgid);

        boolean modified = false;
        for (String tag : tagList) {
          if (!oldTags.contains(tag)) {
            int id = getOrCreateTag(con, tag);

            insertStatement.setInt(2, id);
            insertStatement.executeUpdate();
            modified = true;
          }
        }

        for (String tag : oldTags) {
          if (!tagList.contains(tag)) {
            int id = getOrCreateTag(con, tag);

            deleteStatement.setInt(2, id);
            deleteStatement.executeUpdate();
            modified = true;
          }
        }

        insertStatement.close();
        deleteStatement.close();

        return modified;
      }
    });
  }

  public int getTagId(String tag) throws UserErrorException, TagNotFoundException {
    checkTag(tag);

    List<Integer> res = jdbcTemplate.queryForList("SELECT id FROM tags_values WHERE value=? AND counter>0", Integer.class, tag);

    if (res.isEmpty()) {
      throw new TagNotFoundException();
    } else {
      return res.get(0);
    }
  }
}
