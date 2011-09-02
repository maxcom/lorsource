package ru.org.linux.spring.dao;

import com.google.common.collect.ImmutableList;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.ConnectionCallback;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;
import ru.org.linux.site.BadGroupException;
import ru.org.linux.site.Group;
import ru.org.linux.site.Section;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;

@Repository
public class GroupDao {
  private final JdbcTemplate jdbcTemplate;

  @Autowired
  public GroupDao(DataSource ds) {
    jdbcTemplate = new JdbcTemplate(ds);
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

  public List<Group> getGroups(final Section section) {
    return jdbcTemplate.execute(new ConnectionCallback<List<Group>>() {
      @Override
      public List<Group> doInConnection(Connection con) throws SQLException, DataAccessException {
        return getGroupsInternal(con, section);
      }
    });
  }

  @Deprecated
  public static List<Group> getGroups(Connection db, Section section) throws SQLException {
    return getGroupsInternal(db, section);
  }

  private static List<Group> getGroupsInternal(Connection db, Section section) throws SQLException {
    Statement st = db.createStatement();

    ResultSet rs = st.executeQuery("SELECT sections.moderate, sections.preformat, imagepost, vote, section, havelink, linktext, sections.name as sname, title, urlname, image, restrict_topics, restrict_comments, stat1,stat2,stat3,groups.id,groups.info,groups.longinfo,groups.resolvable FROM groups, sections WHERE sections.id=" + section.getId() + " AND groups.section=sections.id ORDER BY id");

    ImmutableList.Builder<Group> list = ImmutableList.builder();

    while(rs.next()) {
      Group group = new Group(rs);

      list.add(group);
    }

    return list.build();
  }
}
