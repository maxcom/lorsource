package ru.org.linux.spring.dao;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import ru.org.linux.site.MemoriesListItem;
import ru.org.linux.site.Message;
import ru.org.linux.site.User;

import javax.sql.DataSource;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

@Repository
public class MemoriesDao {
  private JdbcTemplate jdbcTemplate;

  @Autowired
  public void setDataSource(DataSource ds) {
    jdbcTemplate = new JdbcTemplate(ds);
  }

  @Transactional(rollbackFor = Exception.class, propagation = Propagation.REQUIRED)
  public void addToMemories(int userid, int topic) {
    List<Integer> res = jdbcTemplate.queryForList(
            "SELECT id FROM memories WHERE userid=? AND topic=? FOR UPDATE",
            Integer.class,
            userid,
            topic
    );

    if (res.isEmpty()) {
      jdbcTemplate.update(
              "INSERT INTO memories (userid, topic) values (?,?)",
              userid,
              topic
      );
    }
  }

  /**
   * Get memories id or 0 if not in memories
   *
   * @param user
   * @param topic
   * @return
   */
  public int getId(User user, Message topic) {
    List<Integer> res = jdbcTemplate.queryForList(
            "SELECT id FROM memories WHERE userid=? AND topic=?",
            Integer.class,
            user.getId(),
            topic.getId()
    );

    if (res.isEmpty()) {
      return 0;
    } else {
      return res.get(0);
    }
  }

  public MemoriesListItem getMemoriesListItem(int id) {
    List<MemoriesListItem> res = jdbcTemplate.query(
            "SELECT * FROM memories WHERE id=?",
            new RowMapper<MemoriesListItem>() {
              @Override
              public MemoriesListItem mapRow(ResultSet rs, int rowNum) throws SQLException {
                return new MemoriesListItem(rs);
              }
            },
            id
    );

    if (res.isEmpty()) {
      return null;
    } else {
      return res.get(0);
    }
  }

  public void delete(int id) {
    jdbcTemplate.update("DELETE FROM memories WHERE id=?", id);
  }
}
