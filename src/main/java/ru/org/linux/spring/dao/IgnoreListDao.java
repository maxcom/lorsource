package ru.org.linux.spring.dao;

import com.google.common.collect.ImmutableSet;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowCallbackHandler;
import org.springframework.stereotype.Repository;
import ru.org.linux.site.AccessViolationException;
import ru.org.linux.site.User;

import javax.sql.DataSource;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Set;

@Repository
public class IgnoreListDao {
  private static final String queryIgnoreList = "SELECT a.ignored FROM ignore_list a WHERE a.userid=?";
  private static final String queryIgnoreStat = "SELECT count(*) as inum FROM ignore_list JOIN users ON  ignore_list.userid = users.id WHERE ignored=? AND not blocked";

  private JdbcTemplate jdbcTemplate;

  @Autowired
  public void setDataSource(DataSource ds) {
    jdbcTemplate = new JdbcTemplate(ds);
  }

  public void addUser(User listOwner, User userToIgnore) throws AccessViolationException {
    if (userToIgnore.isModerator()) {
      throw new AccessViolationException("Нельзя игнорировать модератора");
    }

    jdbcTemplate.update(
            "INSERT INTO ignore_list (userid,ignored) VALUES(?,?)",
            listOwner.getId(),
            userToIgnore.getId()
    );
  }

  public void remove(User listOwner, User userToIgnore) {
    jdbcTemplate.update(
            "DELETE FROM ignore_list WHERE userid=? AND ignored=?",
            listOwner.getId(),
            userToIgnore.getId()
    );
  }

  /**
   * Получить список игнорируемых
   * @param user пользователь который игнорирует
   * @return список игнорируемых
   */
  public Set<Integer> get(User user) {
    final ImmutableSet.Builder<Integer> builder = ImmutableSet.builder();
    jdbcTemplate.query(queryIgnoreList, new RowCallbackHandler() {
      @Override
      public void processRow(ResultSet resultSet) throws SQLException {
        builder.add(resultSet.getInt("ignored"));
      }
    }, user.getId());
    return builder.build();
  }

  public int getIgnoreStat(User ignoredUser) {
    return jdbcTemplate.queryForInt(queryIgnoreStat, ignoredUser.getId());
  }
}
