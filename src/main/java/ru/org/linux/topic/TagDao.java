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

  private static final String QUERY_ALL_TAGS = "SELECT counter,value FROM tags_values WHERE counter>0";

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

  /**
   * Получение списка первых букв тегов.
   *
   * @param skipEmptyUsages пропускать ли буквы, теги которых нигде не используются
   * @return список первых букв тегов.
   */
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

  /**
   * Получение списка тегов по первой букве.
   *
   * @param firstLetter       фильтр: первая буква для тегов, которые должны быть показаны
   * @param skip_empty_usages пропускать ли буквы, теги которых нигде не используются
   * @return список тегов
   */
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
   * Получить только те теги, которые используются.
   *
   * @return список тегов
   */
  public Map<String, Integer> getAllTags() {
    final ImmutableMap.Builder<String, Integer> builder = ImmutableMap.builder();
    jdbcTemplate.query(QUERY_ALL_TAGS, new RowCallbackHandler() {
      @Override
      public void processRow(ResultSet resultSet) throws SQLException {
        builder.put(resultSet.getString("value"), resultSet.getInt("counter"));
      }
    });
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
   * Добавление тега к топику.
   *
   * @param msgId идентификационный номер топика
   * @param tagId идентификационный номер тега
   */
  public void addTagToTopic(int msgId, int tagId) {
    jdbcTemplate.update("INSERT INTO tags VALUES(?,?)", msgId, tagId);
  }

  /**
   * Удаление тега у топика.
   *
   * @param msgId идентификационный номер топика
   * @param tagId идентификационный номер тега
   */
  public void deleteTagFromTopic(int msgId, int tagId) {
    jdbcTemplate.update("DELETE FROM tags WHERE msgid=? and tagid=?", msgId, tagId);

  }

  /**
   * Замена тега в топиках другим тегом.
   *
   * @param oldTagId идентификационный номер старого тега
   * @param newTagId идентификационный номер нового тега
   */
  public void replaceTagForTopics(int oldTagId, int newTagId) {
    jdbcTemplate.update(
      "UPDATE tags SET tagid=? WHERE tagid=? AND msgid NOT IN (SELECT msgid FROM tags WHERE tagid=?)",
      newTagId,
      oldTagId,
      newTagId
    );
  }

  /**
   * Удаление тега из топиков.
   *
   * @param tagId идентификационный номер тега
   */
  public void deleteTagFromTopics(int tagId) {
    jdbcTemplate.update("DELETE FROM tags WHERE tagid=?", tagId);
  }

  /**
   * Получение количества тегов, которые будут изменены для топиков (величина прироста использования тега).
   *
   * @param oldTagId идентификационный номер старого тега
   * @param newTagId идентификационный номер нового тега
   * @return величина прироста использования тега
   */
  public int getCountReplacedTagsForTopic(int oldTagId, int newTagId) {
    List<Integer> res = jdbcTemplate.queryForList(
      "SELECT count (tagid) FROM tags WHERE tagid=? AND msgid NOT IN (SELECT msgid FROM tags WHERE tagid=?)",
      Integer.class,
      oldTagId,
      newTagId
    );

    return res.get(0);
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

  public void recalcAllCounters() {
    jdbcTemplate.update("update tags_values set counter = (select count(*) from tags join topics on tags.msgid=topics.id where tags.tagid=tags_values.id and not deleted)");
  }
}
