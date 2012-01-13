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

package ru.org.linux.group;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.PreparedStatementCallback;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;
import ru.org.linux.section.Section;

import javax.sql.DataSource;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

@Repository
public class GroupDao {
  private final JdbcTemplate jdbcTemplate;

  @Autowired
  public GroupDao(DataSource ds) {
    jdbcTemplate = new JdbcTemplate(ds);
  }

  /**
   * Получить объект группы по идентификатору.
   *
   * @param id идентификатор группы
   * @return объект группы
   * @throws BadGroupException если группа не существует
   */
  public Group getGroup(int id) throws BadGroupException {
    try {
      return jdbcTemplate.queryForObject(
        "SELECT sections.moderate, sections.preformat, imagepost, vote, section, havelink, linktext, title, urlname, image, restrict_topics, restrict_comments,stat1,stat2,stat3,groups.id, groups.info, groups.longinfo, groups.resolvable FROM groups, sections WHERE groups.id=? AND groups.section=sections.id",
        new RowMapper<Group>() {
          @Override
          public Group mapRow(ResultSet resultSet, int i) throws SQLException {
            return new Group(resultSet);
          }
        },
        id
      );
    } catch (EmptyResultDataAccessException ex) {
      throw new BadGroupException("Группа " + id + " не существует", ex);
    }
  }

  /**
   * Получить спусок групп в указанной секции.
   *
   * @param section объект секции.
   * @return спусок групп
   */
  public List<Group> getGroups(Section section) {
    return jdbcTemplate.query(
      "SELECT sections.moderate, sections.preformat, imagepost, vote, section, havelink, linktext, title, urlname, image, restrict_topics, restrict_comments, stat1,stat2,stat3,groups.id,groups.info,groups.longinfo,groups.resolvable FROM groups, sections WHERE sections.id=? AND groups.section=sections.id ORDER BY id",
      new RowMapper<Group>() {
        @Override
        public Group mapRow(ResultSet rs, int rowNum) throws SQLException {
          return new Group(rs);
        }
      },
      section.getId()
    );
  }


  /**
   * Получить объект группы в указанной секции по имени группы.
   *
   * @param section объект секции.
   * @param name    имя группы
   * @return объект группы
   * @throws BadGroupException если группа не существует
   */
  public Group getGroup(Section section, String name) throws BadGroupException {
    try {
      int id = jdbcTemplate.queryForInt("SELECT id FROM groups WHERE section=? AND urlname=?", section.getId(), name);

      return getGroup(id);
    } catch (EmptyResultDataAccessException ex) {
      throw new BadGroupException("group not found");
    }
  }

  /**
   * Подсчитать количество тем в группе.
   *
   * @param group       объект группы
   * @param showDeleted - учитывать ли удалённые темы
   * @return количество тем в группе
   */
  public int calcTopicsCount(Group group, boolean showDeleted) {
    String query = "SELECT count(topics.id) " +
      "FROM topics WHERE " +
      (group.isModerated() ? "moderate AND " : "") +
      "groupid=?";

    if (!showDeleted) {
      query += " AND NOT topics.deleted";
    }

    List<Integer> res = jdbcTemplate.queryForList(query, Integer.class, group.getId());

    if (!res.isEmpty()) {
      return res.get(0);
    } else {
      return 0;
    }
  }

  /**
   * Изменить настройки группы.
   *
   * @param group      объект группы
   * @param title      Заголовок группы
   * @param info       дополнительная информация
   * @param longInfo   расширенная дополнительная информация
   * @param resolvable м ожно ли ставить темам признак "тема решена"
   * @param urlName
   */
  public void setParams(final Group group, final String title, final String info, final String longInfo, final boolean resolvable, final String urlName) {
    jdbcTemplate.execute(
      "UPDATE groups SET title=?, info=?, longinfo=?,resolvable=?,urlname=? WHERE id=?",
      new PreparedStatementCallback<String>() {
        @Override
        public String doInPreparedStatement(PreparedStatement pst) throws SQLException, DataAccessException {
          pst.setString(1, title);

          if (!info.isEmpty()) {
            pst.setString(2, info);
          } else {
            pst.setString(2, null);
          }

          if (!longInfo.isEmpty()) {
            pst.setString(3, longInfo);
          } else {
            pst.setString(3, null);
          }

          pst.setBoolean(4, resolvable);
          pst.setString(5, urlName);
          pst.setInt(6, group.getId());

          pst.executeUpdate();

          return null;
        }
      }
    );
  }
}
