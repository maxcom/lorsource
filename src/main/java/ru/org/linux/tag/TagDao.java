/*
 * Copyright 1998-2013 Linux.org.ru
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

package ru.org.linux.tag;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowCallbackHandler;
import org.springframework.jdbc.core.simple.SimpleJdbcInsert;
import org.springframework.stereotype.Repository;

import javax.sql.DataSource;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.SortedSet;
import java.util.TreeSet;

@Repository
public class TagDao {
  private static final Logger logger = LoggerFactory.getLogger(TagDao.class);

  private static final int TOP_TAGS_COUNT = 50;

  private static final String QUERY_TAG_ID_BY_NAME = "SELECT id FROM tags_values WHERE value=?";

  private JdbcTemplate jdbcTemplate;
  private SimpleJdbcInsert simpleJdbcInsert;


  @Autowired
  public void setDataSource(DataSource ds) {
    jdbcTemplate = new JdbcTemplate(ds);
    simpleJdbcInsert = new SimpleJdbcInsert(ds)
            .withTableName("tags_values")
            .usingColumns("value")
            .usingGeneratedKeyColumns("id");
  }

  /**
   * Создать новый тег.
   *
   * @param tagName название нового тега
   * @return tag id
   */
  public int createTag(String tagName) {
    Preconditions.checkArgument(TagName.isGoodTag(tagName), "Tag name must be valid");

    int id = simpleJdbcInsert.executeAndReturnKey(ImmutableMap.<String, Object>of("value", tagName)).intValue();

    logger.debug("Создан тег: '{}' id={}", tagName, id);

    return id;
  }

  /**
   * Изменить название существующего тега.
   *
   * @param tagId   идентификационный номер существующего тега
   * @param tagName новое название тега
   */
  public void changeTag(int tagId, String tagName) {
    jdbcTemplate.update("UPDATE tags_values set value=? WHERE id=?", tagName, tagId);
  }

  /**
   * Удалить тег.
   *
   * @param tagId идентификационный номер тега
   */
  public void deleteTag(int tagId) {
    jdbcTemplate.update("DELETE FROM tags_values WHERE id=?", tagId);
  }

  public SortedSet<String> getTopTags() {
    final SortedSet<String> set = new TreeSet<>();

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
   * Получение списка первых букв тегов.
   *
   * @return список первых букв тегов.
   */
  SortedSet<String> getFirstLetters() {
    final SortedSet<String> set = new TreeSet<>();

    StringBuilder query = new StringBuilder();
    query.append("select distinct firstchar from ")
      .append("(select lower(substr(value,1,1)) as firstchar from tags_values ");

    query.append(" where counter > 0 ");
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

  /**
   * Получение списка тегов по префиксу.
   *
   * @param prefix       префикс имени тега
   * @return список тегов
   */
  List<TagInfo> getTagsByPrefix(String prefix, int minCount) {
    final ImmutableList.Builder<TagInfo> builder = ImmutableList.builder();
    StringBuilder query = new StringBuilder();
    query.append("select counter, value, id from tags_values where value like ?");
    query.append(" and counter >= ? ");
    query.append(" order by value");

    jdbcTemplate.query(query.toString(),
      new RowCallbackHandler() {
        @Override
        public void processRow(ResultSet resultSet) throws SQLException {
          builder.add(
                  new TagInfo(
                          resultSet.getString("value"),
                          resultSet.getInt("counter"),
                          resultSet.getInt("id")
                  )
          );
        }
      },
            escapeLikeWildcards(prefix) + "%", minCount
    );

    return builder.build();
  }

  /**
   * Получение списка тегов по префиксу.
   *
   * @param prefix       префикс имени тега
   * @return список тегов
   */
  SortedSet<String> getTopTagsByPrefix(String prefix, int minCount, int count) {
    final SortedSet<String> res = new TreeSet<>();

    String query = "select counter, value " +
            "from tags_values " +
            "where value like ? and counter >= ? " +
            "order by counter DESC LIMIT ?";

    jdbcTemplate.query(query,
      new RowCallbackHandler() {
        @Override
        public void processRow(ResultSet resultSet) throws SQLException {
          res.add(resultSet.getString("value"));
        }
      },
            escapeLikeWildcards(prefix) + "%", minCount, count
    );

    return res;
  }

  private static String escapeLikeWildcards(String str) {
    return str.replaceAll("[_%]", "\\\\$0");
  }

  /**
   * Получение идентификационного номера тега по названию.
   *
   * @param tag название тега
   * @param skipZero пропускать неиспользуемые теги
   * @return идентификационный номер
   * @throws TagNotFoundException
   */
  public int getTagId(String tag, boolean skipZero) throws TagNotFoundException {
    List<Integer> res = jdbcTemplate.queryForList(
            QUERY_TAG_ID_BY_NAME + (skipZero?" AND counter>0":""),
            Integer.class,
            tag
    );

    if (res.isEmpty()) {
      throw new TagNotFoundException();
    } else {
      return res.get(0);
    }
  }

  public int getTagId(String tagName) throws TagNotFoundException {
    return getTagId(tagName, false);
  }

  public int getCounter(int tagId) {
    return jdbcTemplate.queryForObject("SELECT counter FROM tags_values WHERE id=?", Integer.class, tagId);
  }

  public List<String> relatedTags(int tagid) {
    return jdbcTemplate.queryForList(
            "select value from " +
                    "(select st.tagid, count(*) as cnt from tags as mt join tags as st on mt.msgid=st.msgid " +
                    "where mt.tagid=? and mt.tagid<>st.tagid group by st.tagid having count(*)>2) as q " +
                    "join tags_values on q.tagid=tags_values.id where counter>5 order by cnt::real/counter desc limit 10",
            String.class,
            tagid
    );
  }
}
