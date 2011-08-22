package ru.org.linux.spring.dao;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;
import ru.org.linux.site.BadGroupException;
import ru.org.linux.site.Group;

import javax.sql.DataSource;
import java.sql.ResultSet;
import java.sql.SQLException;

@Repository
public class GroupDao {
  private final JdbcTemplate jdbcTemplate;

  @Autowired
  public GroupDao(DataSource ds) {
    this.jdbcTemplate = new JdbcTemplate(ds);
  }

  public Group getGroup(int id) throws BadGroupException {
    return getGroupInternal(jdbcTemplate, id);
  }

  @Deprecated
  public static Group getGroup(JdbcTemplate jdbcTemplate, int id) throws BadGroupException {
    return getGroupInternal(jdbcTemplate, id);
  }

  private static Group getGroupInternal(JdbcTemplate jdbcTemplate, int id) throws BadGroupException {
    try {
      return jdbcTemplate.queryForObject(
              "SELECT sections.moderate, sections.preformat, imagepost, vote, section, havelink, linktext, sections.name as sname, title, urlname, image, restrict_topics, restrict_comments,stat1,stat2,stat3,groups.id, groups.info, groups.longinfo, groups.resolvable FROM groups, sections WHERE groups.id=? AND groups.section=sections.id",
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
}
