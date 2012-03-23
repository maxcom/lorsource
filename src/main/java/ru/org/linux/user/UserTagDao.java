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

package ru.org.linux.user;

import com.google.common.collect.ImmutableList;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowCallbackHandler;
import org.springframework.stereotype.Repository;

import javax.sql.DataSource;
import java.sql.ResultSet;
import java.sql.SQLException;

@Repository
public class UserTagDao {

  private JdbcTemplate jdbcTemplate;

  @Autowired
  public void setDataSource(DataSource ds) {
    jdbcTemplate = new JdbcTemplate(ds);
  }

  /**
   * Добавление тега к пользователю.
   *
   * @param userId     идентификационный номер пользователя
   * @param tagId      идентификационный номер тега
   * @param isFavorite выбирать фаворитные теги (true) или игнорируемые (false)
   */
  public void addTag(int userId, int tagId, boolean isFavorite) {
    jdbcTemplate.update(
      "INSERT INTO user_tags (user_id, tag_id, is_favorite) VALUES(?,?,?)",
      userId,
      tagId,
      isFavorite
    );
  }

  /**
   * Удаление тега у пользователя.
   *
   * @param userId     идентификационный номер пользователя
   * @param tagId      идентификационный номер тега
   * @param isFavorite выбирать фаворитные теги (true) или игнорируемые (false)
   */
  public void deleteTag(int userId, int tagId, boolean isFavorite) {
    jdbcTemplate.update(
      "DELETE FROM user_tags WHERE user_id=? and tag_id=? and is_favorite=?",
      userId,
      tagId,
      isFavorite
    );
  }

  /**
   * Удаление тега у всех пользователей.
   *
   * @param tagId идентификационный номер тега
   */
  public void deleteTags(int tagId) {
    jdbcTemplate.update("DELETE FROM user_tags WHERE tag_id=?", tagId);
  }

  /**
   * Получить список всех тегов для пользователя.
   *
   * @param userId     идентификационный номер пользователя
   * @param isFavorite выбирать фаворитные теги (true) или игнорируемые (false)
   * @return список тегов пользователя
   */
  public ImmutableList<String> getTags(int userId, boolean isFavorite) {
    final ImmutableList.Builder<String> tags = ImmutableList.builder();

    jdbcTemplate.query(
      "SELECT tags_values.value FROM user_tags, tags_values WHERE " +
        "user_tags.user_id=? AND tags_values.id=user_tags.tag_id AND user_tags.is_favorite=?" +
        "ORDER BY value",
      new RowCallbackHandler() {
        @Override
        public void processRow(ResultSet rs) throws SQLException {
          tags.add(rs.getString("value"));
        }
      },
      userId,
      isFavorite
    );

    return tags.build();
  }

}
