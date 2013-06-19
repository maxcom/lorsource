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

package ru.org.linux.tag;

import com.google.common.collect.ImmutableMap;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowCallbackHandler;
import org.springframework.stereotype.Repository;

import javax.sql.DataSource;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;
import java.util.SortedSet;
import java.util.TreeSet;

@Repository
public class TagDao {

  private static final int TOP_TAGS_COUNT = 50;

  private static final String QUERY_TAG_ID_BY_NAME = "SELECT id FROM tags_values WHERE value=?";

  private JdbcTemplate jdbcTemplate;

  @Autowired
  public void setDataSource(DataSource ds) {
    jdbcTemplate = new JdbcTemplate(ds);
  }

  /**
   * Создать новый тег.
   *
   * @param tagName название нового тега
   */
  public void createTag(String tagName) {
    jdbcTemplate.update("INSERT INTO tags_values (value) VALUES(?)", tagName);
  }

  /**
   * Изменить название существующего тега.
   *
   * @param tagId   идентификационный номер существующего тега
   * @param tagName новое название тега
   */
  public void changeTag(Integer tagId, String tagName) {
    jdbcTemplate.update(
      "UPDATE tags_values set value=? WHERE id=?", tagName, tagId);
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
   * Получение списка тегов по первой букве.
   *
   * @param firstLetter       фильтр: первая буква для тегов, которые должны быть показаны
   * @return список тегов
   */
  Map<String, Integer> getTagsByFirstLetter(String firstLetter) {
    final ImmutableMap.Builder<String, Integer> builder = ImmutableMap.builder();
    StringBuilder query = new StringBuilder();
    query.append("select counter, value from tags_values where lower(substr(value,1,1)) = ? ");
    query.append(" and counter > 0 ");
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
   * Увеличить счётчик использования тега.
   *
   * @param tagId    идентификационный номер тега
   * @param tagCount на какое значение изменить счётчик
   */
  public void increaseCounterById(int tagId, int tagCount) {
    jdbcTemplate.update("UPDATE tags_values SET counter=counter+? WHERE id=?", tagCount, tagId);
  }

  /**
   * Уменьшить счётчик использования тега.
   *
   * @param tagId    идентификационный номер тега
   * @param tagCount на какое значение изменить счётчик
   */
  public void decreaseCounterById(int tagId, int tagCount) {
    jdbcTemplate.update("UPDATE tags_values SET counter=counter-? WHERE id=?", tagCount, tagId);
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
}
