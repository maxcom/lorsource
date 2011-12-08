package ru.org.linux.dao;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import ru.org.linux.dto.FavoritesListItemDto;
import ru.org.linux.dto.MessageDto;
import ru.org.linux.dto.UserDto;

import javax.sql.DataSource;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

@Repository
public class FavoritesDao {
  private JdbcTemplate jdbcTemplate;

  @Autowired
  public void setDataSource(DataSource ds) {
    jdbcTemplate = new JdbcTemplate(ds);
  }

  /**
   * Добавить в избранное.
   *
   * @param userid идентификатор пользователя
   * @param topic  идентификатор сообщения
   */
  @Transactional(rollbackFor = Exception.class, propagation = Propagation.REQUIRED)
  public void add(int userid, int topic) {
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
   * Get memories id or 0 if not in memories.
   *
   * @param user
   * @param topic
   * @return
   */
  public int getId(UserDto user, MessageDto topic) {
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

  /**
   * Получить один объект списка избранного.
   *
   * @param id идентификатор избранного
   * @return объект списка избранного
   */
  public FavoritesListItemDto getListItem(int id) {
    List<FavoritesListItemDto> res = jdbcTemplate.query(
      "SELECT * FROM memories WHERE id=?",
      new RowMapper<FavoritesListItemDto>() {
        @Override
        public FavoritesListItemDto mapRow(ResultSet rs, int rowNum) throws SQLException {
          return new FavoritesListItemDto(rs);
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

  /**
   * Удалить запись из избранного.
   *
   * @param id идентификатор избранного
   */
  public void delete(int id) {
    jdbcTemplate.update("DELETE FROM memories WHERE id=?", id);
  }
}
