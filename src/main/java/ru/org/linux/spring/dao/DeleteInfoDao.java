/*
 * Copyright 1998-2024 Linux.org.ru
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

package ru.org.linux.spring.dao;

import com.google.common.base.Preconditions;
import org.springframework.jdbc.core.BatchPreparedStatementSetter;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import ru.org.linux.site.DeleteInfo;
import ru.org.linux.user.User;
import scala.Option;

import javax.annotation.Nonnull;
import javax.sql.DataSource;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

/**
 * Получение информации кем и почему удален топик
 */
@Repository
public class DeleteInfoDao {
  private final JdbcTemplate jdbcTemplate;
  private static final String QUERY_DELETE_INFO =
          "SELECT reason,delby as userid, deldate, bonus FROM del_info WHERE msgid=?";

  private static final String QUERY_DELETE_INFO_FOR_UPDATE =
          "SELECT reason,delby as userid, deldate, bonus FROM del_info WHERE msgid=? FOR UPDATE";

  private static final String INSERT_DELETE_INFO =
          "INSERT INTO del_info (msgid, delby, reason, deldate, bonus) values(?,?,?, CURRENT_TIMESTAMP, ?)";

  public DeleteInfoDao(DataSource dataSource) {
    jdbcTemplate = new JdbcTemplate(dataSource);
  }

  /**
   * Кто, когда и почему удалил сообщение
   * @param id id проверяемого сообщения
   * @return информация о удаленном сообщении
   */
  public Optional<DeleteInfo> getDeleteInfo(int id) {
    return Optional.ofNullable(getDeleteInfo(id, false));
  }

  /**
   * Кто, когда и почему удалил сообщение
   * @param id id проверяемого сообщения
   * @param forUpdate блокировать запись до конца текущей транзакции (SELECT ... FOR UPDATE)
   * @return информация о удаленном сообщении
   */
  public DeleteInfo getDeleteInfo(int id, boolean forUpdate) {
    List<DeleteInfo> list = jdbcTemplate.query(
            forUpdate?QUERY_DELETE_INFO_FOR_UPDATE:QUERY_DELETE_INFO,
            (resultSet, i) -> {
              Integer bonus = resultSet.getInt("bonus");
              if (resultSet.wasNull()) {
                bonus = null;
              }

              return new DeleteInfo(
                      resultSet.getInt("userid"),
                      resultSet.getString("reason"),
                      resultSet.getTimestamp("deldate"),
                      Option.apply(bonus)
              );
            }, id);

    if (list.isEmpty()) {
      return null;
    } else {
      return list.get(0);
    }
  }

  public void insert(int msgid, User deleter, String reason, int scoreBonus) {
    Preconditions.checkArgument(scoreBonus <= 0, "Score bonus on delete must be non-positive");

    jdbcTemplate.update(INSERT_DELETE_INFO, msgid, deleter.getId(), reason, scoreBonus);
  }

  public int getRecentScoreLoss(User user) {
    return Math.abs(jdbcTemplate.queryForObject(
            "select COALESCE(sum(bonus), 0) from del_info where deldate>CURRENT_TIMESTAMP-'2 week'::interval and " +
                    "msgid in (select id from comments where comments.userid = ? union all select id from topics where topics.userid = ?)",
            Integer.class, user.getId(), user.getId()));
  }

  public void insert(final List<InsertDeleteInfo> deleteInfos) {
    if (deleteInfos.isEmpty()) {
      return;
    }

    jdbcTemplate.batchUpdate(INSERT_DELETE_INFO, new BatchPreparedStatementSetter() {
      @Override
      public void setValues(PreparedStatement ps, int i) throws SQLException {
        InsertDeleteInfo info = deleteInfos.get(i);

        ps.setInt(1, info.getMsgid());
        ps.setInt(2, info.getDeleteUser());
        ps.setString(3, info.getReason());
        ps.setInt(4, info.getBonus());
      }

      @Override
      public int getBatchSize() {
        return deleteInfos.size();
      }
    });
  }

  public void delete(int msgid) {
    jdbcTemplate.update("DELETE FROM del_info WHERE msgid=?", msgid);
  }

  public static class InsertDeleteInfo {
    private final int msgid;
    private final String reason;
    private final int bonus;
    private final int deleteUser;

    public InsertDeleteInfo(int msgid, @Nonnull String reason, int bonus, int deleteUser) {
      Preconditions.checkArgument(bonus <= 0, "Score bonus on delete must be non-positive");

      this.msgid = msgid;
      this.reason = reason;
      this.bonus = bonus;
      this.deleteUser = deleteUser;
    }

    public int getMsgid() {
      return msgid;
    }

    public String getReason() {
      return reason;
    }

    public int getBonus() {
      return bonus;
    }

    public int getDeleteUser() {
      return deleteUser;
    }
  }
}
