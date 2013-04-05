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

package ru.org.linux.group;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
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
  private JdbcTemplate jdbcTemplate;

  @Autowired
  public void setDateSource(DataSource ds) {
    jdbcTemplate = new JdbcTemplate(ds);
  }

  /**
   * Получить объект группы по идентификатору.
   *
   * @param id идентификатор группы
   * @return объект группы
   * @throws GroupNotFoundException если группа не существует
   */
  @Cacheable("Groups")
  public Group getGroup(int id) throws GroupNotFoundException {
    try {
      return jdbcTemplate.queryForObject(
        "SELECT sections.moderate, vote, section, havelink, linktext, title, urlname, image, groups.restrict_topics, restrict_comments,stat1,stat3,groups.id, groups.info, groups.longinfo, groups.resolvable FROM groups, sections WHERE groups.id=? AND groups.section=sections.id",
        new RowMapper<Group>() {
          @Override
          public Group mapRow(ResultSet resultSet, int i) throws SQLException {
            return Group.buildGroup(resultSet);
          }
        },
        id
      );
    } catch (EmptyResultDataAccessException ex) {
      throw new GroupNotFoundException("Группа " + id + " не существует", ex);
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
      "SELECT sections.moderate, vote, section, havelink, linktext, title, urlname, image, groups.restrict_topics, restrict_comments, stat1,stat3,groups.id,groups.info,groups.longinfo,groups.resolvable FROM groups, sections WHERE sections.id=? AND groups.section=sections.id ORDER BY id",
      new RowMapper<Group>() {
        @Override
        public Group mapRow(ResultSet rs, int rowNum) throws SQLException {
          return Group.buildGroup(rs);
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
   * @throws GroupNotFoundException если группа не существует
   */
  public Group getGroup(Section section, String name) throws GroupNotFoundException {
    try {
      int id = jdbcTemplate.queryForInt("SELECT id FROM groups WHERE section=? AND urlname=?", section.getId(), name);

      return getGroup(id);
    } catch (EmptyResultDataAccessException ex) {
      throw new GroupNotFoundException("group not found");
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
  @CacheEvict(value="Groups", key="#group.id")
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
