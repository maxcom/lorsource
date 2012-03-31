package ru.org.linux.marks;

import com.google.common.collect.ImmutableMap;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.jdbc.core.RowCallbackHandler;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.core.simple.SimpleJdbcInsert;
import org.springframework.stereotype.Repository;

import javax.sql.DataSource;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Repository
public class MessageMarksDao {
  private NamedParameterJdbcTemplate jdbcTemplate;
  private SimpleJdbcInsert jdbcInsert;
  
  @Autowired
  private void setDataSoure(DataSource ds) {
    jdbcTemplate = new NamedParameterJdbcTemplate(ds);
    jdbcInsert = new SimpleJdbcInsert(ds);
    
    jdbcInsert.setTableName("msg_marks");
  }

  @Cacheable("MsgMarks")
  public Map<MessageMark, Integer> getMessageMarks(int msgid) {
    final ImmutableMap.Builder<MessageMark, Integer> builder = ImmutableMap.builder();

    jdbcTemplate.query(
            "SELECT mark, count(*) FROM msg_marks WHERE msgid=:msgid GROUP BY mark",
            ImmutableMap.of("msgid", msgid),
            new RowCallbackHandler() {
              @Override
              public void processRow(ResultSet rs) throws SQLException {
                builder.put(MessageMark.getById(rs.getInt("mark")), rs.getInt("count"));
              }
            }
    );

    return builder.build();
  }

  @CacheEvict(value = "MsgMarks", key = "#msgid")
  public void mark(int msgid, int userid, MessageMark mark) {
    try {
      jdbcInsert.execute(
              ImmutableMap.<String, Object>of("msgid", msgid, "userid", userid, "mark", mark.getId())
      );
    } catch (DuplicateKeyException ex) {
      // ignore
    }
  }
  
  public List<MessageMark> getMarks(int userid, int msgid) {
    return jdbcTemplate.query(
            "SELECT mark FROM msg_marks WHERE msgid=:msgid and userid=:userid",
            ImmutableMap.<String, Object>of("msgid", msgid, "userid", userid),
            new RowMapper<MessageMark>() {
              @Override
              public MessageMark mapRow(ResultSet resultSet, int i) throws SQLException {
                return MessageMark.getById(resultSet.getInt("mark"));
              }
            }
    );  
  }
}
