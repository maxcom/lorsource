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
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowCallbackHandler;
import org.springframework.stereotype.Repository;

import javax.annotation.Nonnull;
import javax.sql.DataSource;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

@Repository
public class TopicTagDao {

  private JdbcTemplate jdbcTemplate;

  @Autowired
  public void setDataSource(DataSource ds) {
    jdbcTemplate = new JdbcTemplate(ds);
  }

  /**
   * Добавление тега к топику.
   *
   * @param msgId идентификационный номер топика
   * @param tagId идентификационный номер тега
   */
  public void addTag(int msgId, int tagId) {
    jdbcTemplate.update("INSERT INTO tags VALUES(?,?)", msgId, tagId);
  }

  /**
   * Удаление тега у топика.
   *
   * @param msgId идентификационный номер топика
   * @param tagId идентификационный номер тега
   */
  public void deleteTag(int msgId, int tagId) {
    jdbcTemplate.update("DELETE FROM tags WHERE msgid=? and tagid=?", msgId, tagId);

  }

  /**
   * Получить список тегов топика .
   *
   * @param msgid идентификационный номер топика
   * @return список тегов топика
   */
  @Nonnull
  public ImmutableList<String> getTags(int msgid) {
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

  /**
   * Получение количества тегов, которые будут изменены для топиков (величина прироста использования тега).
   *
   * @param oldTagId идентификационный номер старого тега
   * @param newTagId идентификационный номер нового тега
   * @return величина прироста использования тега
   */
  public int getCountReplacedTags(int oldTagId, int newTagId) {
    List<Integer> res = jdbcTemplate.queryForList(
      "SELECT count (tagid) FROM tags WHERE tagid=? AND msgid NOT IN (SELECT msgid FROM tags WHERE tagid=?)",
      Integer.class,
      oldTagId,
      newTagId
    );

    return res.get(0);
  }

  /**
   * Замена тега в топиках другим тегом.
   *
   * @param oldTagId идентификационный номер старого тега
   * @param newTagId идентификационный номер нового тега
   */
  public void replaceTag(int oldTagId, int newTagId) {
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
  public void deleteTag(int tagId) {
    jdbcTemplate.update("DELETE FROM tags WHERE tagid=?", tagId);
  }

  /**
   * пересчёт счётчиков использования.
   */
public void reCalculateAllCounters() {
    jdbcTemplate.update("update tags_values set counter = (select count(*) from tags join topics on tags.msgid=topics.id where tags.tagid=tags_values.id and not deleted)");
  }

}
