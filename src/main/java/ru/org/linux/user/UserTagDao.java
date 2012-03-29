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
import org.springframework.jdbc.core.RowCallbackHandler;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import javax.sql.DataSource;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Repository
public class UserTagDao {

  private NamedParameterJdbcTemplate jdbcTemplate;

  @Autowired
  public void setDataSource(DataSource ds) {
    jdbcTemplate = new NamedParameterJdbcTemplate(ds);
  }

  /**
   * Добавление тега к пользователю.
   *
   * @param userId     идентификационный номер пользователя
   * @param tagId      идентификационный номер тега
   * @param isFavorite выбирать фаворитные теги (true) или игнорируемые (false)
   */
  public void addTag(int userId, int tagId, boolean isFavorite) {
    MapSqlParameterSource parameters = new MapSqlParameterSource();
    parameters.addValue("user_id", userId);
    parameters.addValue("tag_id", tagId);
    parameters.addValue("is_favorite", isFavorite);
    jdbcTemplate.update(
      "INSERT INTO user_tags (user_id, tag_id, is_favorite) VALUES(:user_id, :tag_id, :is_favorite)",
      parameters
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
    MapSqlParameterSource parameters = new MapSqlParameterSource();
    parameters.addValue("user_id", userId);
    parameters.addValue("tag_id", tagId);
    parameters.addValue("is_favorite", isFavorite);

    jdbcTemplate.update(
      "DELETE FROM user_tags WHERE user_id=:user_id and tag_id=:tag_id and is_favorite=:is_favorite",
      parameters
    );
  }

  /**
   * Удаление тега у всех пользователей.
   *
   * @param tagId идентификационный номер тега
   */
  public void deleteTags(int tagId) {
    MapSqlParameterSource parameters = new MapSqlParameterSource();
    parameters.addValue("tag_id", tagId);
    jdbcTemplate.update("DELETE FROM user_tags WHERE tag_id=:tag_id", parameters);
  }

  /**
   * Получить список всех тегов для пользователя.
   *
   * @param userId     идентификационный номер пользователя
   * @param isFavorite выбирать фаворитные теги (true) или игнорируемые (false)
   * @return список тегов пользователя
   */
  public ImmutableList<String> getTags(int userId, boolean isFavorite) {
    MapSqlParameterSource parameters = new MapSqlParameterSource();
    parameters.addValue("user_id", userId);
    parameters.addValue("is_favorite", isFavorite);

    final ImmutableList.Builder<String> tags = ImmutableList.builder();

    jdbcTemplate.query(
      "SELECT tags_values.value FROM user_tags, tags_values WHERE " +
        "user_tags.user_id=:user_id AND tags_values.id=user_tags.tag_id AND user_tags.is_favorite=:is_favorite " +
        "ORDER BY value",
      parameters,
      new RowCallbackHandler() {
        @Override
        public void processRow(ResultSet rs) throws SQLException {
          tags.add(rs.getString("value"));
        }
      }
    );

    return tags.build();
  }

  /**
   * Получить список ID пользователей, у которых в профиле есть перечисленные фаворитные теги.
   *
   * @param tags  список фаворитных тегов
   * @return список ID пользователей
   */
  public List<Integer> getUserIdListByTags(List<String> tags) {
    if (tags.isEmpty()) {
      return ImmutableList.of();
    }

    return jdbcTemplate.queryForList(
      "select distinct user_id from user_tags where tag_id in (select id from tags_values where value in ( :values )) "
        + "AND is_favorite = true",
      Collections.singletonMap("values", tags),
      Integer.class
    );
  }

  /**
   * Замена тега у пользователей другим тегом.
   *
   * @param oldTagId идентификационный номер старого тега
   * @param newTagId идентификационный номер нового тега
   */
  public void replaceTag(int oldTagId, int newTagId) {
    MapSqlParameterSource parameters = new MapSqlParameterSource();
    parameters.addValue("new_tag_id", newTagId);
    parameters.addValue("old_tag_id", oldTagId);
    jdbcTemplate.update(
      "UPDATE user_tags SET tag_id=:new_tag_id WHERE tag_id=:old_tag_id " +
        "AND user_id NOT IN (SELECT user_id FROM user_tags WHERE tag_id=:new_tag_id)",
      parameters
    );
  }

}
