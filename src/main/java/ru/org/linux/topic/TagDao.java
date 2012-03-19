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

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.ConnectionCallback;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowCallbackHandler;
import org.springframework.stereotype.Repository;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;
import java.util.SortedSet;
import java.util.TreeSet;

@Repository
public class TagDao {

  private static final int TOP_TAGS_COUNT = 50;

  private static final String queryAllTags = "SELECT counter,value FROM tags_values WHERE counter>0";

  private static final String QUERY_TAG_ID_BY_NAME = "SELECT id FROM tags_values WHERE value=?";

  private JdbcTemplate jdbcTemplate;

  @Autowired
  public void setDataSource(DataSource ds) {
    jdbcTemplate = new JdbcTemplate(ds);
  }

  private static synchronized int getOrCreateTag(Connection con, String tag) throws SQLException {
    PreparedStatement st2 = con.prepareStatement("SELECT id FROM tags_values WHERE value=?");
    st2.setString(1, tag);
    ResultSet rs = st2.executeQuery();
    int id;

    if (rs.next()) {
      id = rs.getInt("id");
    } else {
      PreparedStatement st = con.prepareStatement("INSERT INTO tags_values (value) VALUES(?)");
      st.setString(1, tag);
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

  /**
   * Создать новый тег.
   *
   * @param tagName название нового тега
   */
  public void createTag(String tagName) {
    jdbcTemplate.update(
      "INSERT INTO tags_values (value) VALUES(?)",
      new Object[]{tagName}
    );
  }

  /**
   * Изменить название существующего тега.
   *
   * @param tagId   идентификационный номер существующего тега
   * @param tagName новое название тега
   */
  public void changeTag(Integer tagId, String tagName) {
    jdbcTemplate.update(
      "UPDATE tags_values set value=? WHERE id=?",
      new Object[]{tagName, tagId}
    );
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

  public SortedSet<String> getTopTags() {
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

  SortedSet<String> getFirstLetters(boolean skipEmptyUsages) {
    final SortedSet<String> set = new TreeSet<String>();

    StringBuilder query = new StringBuilder();
    query.append("select distinct firstchar from ")
      .append("(select lower(substr(value,1,1)) as firstchar from tags_values ");

    if (skipEmptyUsages) {
      query.append(" where counter > 0 ");
    }
    query.append(" order by firstchar) firstchars");

    jdbcTemplate.query(
      query.toString(),
      new RowCallbackHandler() {
        @Override
        public void processRow(ResultSet rs) throws SQLException {
          set.add(rs.getString("firstchar"));
        }
      }
    );
    return set;
  }

  Map<String, Integer> getTagsByFirstLetter(String firstLetter, boolean skip_empty_usages) {
    final ImmutableMap.Builder<String, Integer> builder = ImmutableMap.builder();
    StringBuilder query = new StringBuilder();
    query.append("select counter, value from tags_values where lower(substr(value,1,1)) = ? ");

    if (skip_empty_usages) {
      query.append(" and counter > 0 ");
    }
    query.append(" order by value");

    jdbcTemplate.query(query.toString(),
      new RowCallbackHandler() {
        @Override
        public void processRow(ResultSet resultSet) throws SQLException {
          builder.put(resultSet.getString("value"), resultSet.getInt("counter"));
        }
      },
      firstLetter
    );
    return builder.build();
  }

  /**
   * Получить все тэги со счетчиком
   *
   * @return список всех тегов
   */
  public Map<String, Integer> getAllTags() {
    final ImmutableMap.Builder<String, Integer> builder = ImmutableMap.builder();
    jdbcTemplate.query(queryAllTags, new RowCallbackHandler() {
      @Override
      public void processRow(ResultSet resultSet) throws SQLException {
        builder.put(resultSet.getString("value"), resultSet.getInt("counter"));
      }
    });
    return builder.build();
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

  /**
   * Получение идентификационного номера тега по названию. Тег должен использоваться.
   *
   * @param tag название тега
   * @return идентификационный номер
   * @throws TagNotFoundException
   */
  public int getTagId(String tag)
    throws TagNotFoundException {
    List<Integer> res = jdbcTemplate.queryForList(QUERY_TAG_ID_BY_NAME + " AND counter>0", Integer.class, tag);

    if (res.isEmpty()) {
      throw new TagNotFoundException();
    } else {
      return res.get(0);
    }
  }

  /**
   * Получение идентификационного номера тега по названию.
   *
   * @param tagName название тега
   * @return идентификационный номер
   * @throws TagNotFoundException
   */
  public int getTagIdByName(String tagName)
    throws TagNotFoundException {
    List<Integer> res = jdbcTemplate.queryForList(QUERY_TAG_ID_BY_NAME, Integer.class, tagName);
    if (res.isEmpty()) {
      throw new TagNotFoundException();
    } else {
      return res.get(0);
    }
  }
}
