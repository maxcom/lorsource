package ru.org.linux.spring.dao;

import com.google.common.collect.ImmutableMap;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.simple.SimpleJdbcInsert;
import org.springframework.stereotype.Repository;
import ru.org.linux.site.User;

import javax.sql.DataSource;
import java.util.Map;

@Repository
public class UserEventsDao {
  private SimpleJdbcInsert insert;

  @Autowired
  public void setDataSource(DataSource ds) {
    insert = new SimpleJdbcInsert(ds);

    insert.setTableName("user_events");
    insert.usingColumns("userid", "type", "private", "message_id", "comment_id", "message");
  }

  public void addUserRefEvent(User[] refs, int topic, int comment) {
    if (refs.length==0) {
      return;
    }

    Map<String, Object>[] batch = new Map[refs.length];

    for (int i=0; i<refs.length; i++) {
      User ref = refs[i];

      batch[i] = ImmutableMap.<String, Object>of(
            "userid", ref.getId(),
            "type", "REF",
            "private", false,
            "message_id", topic,
            "comment_id", comment
      );
    }

    insert.executeBatch(batch);
  }

  public void addUserRefEvent(User[] refs, int topic) {
    if (refs.length==0) {
      return;
    }

    Map<String, Object>[] batch = new Map[refs.length];

    for (int i=0; i<refs.length; i++) {
      User ref = refs[i];

      batch[i] = ImmutableMap.<String, Object>of(
            "userid", ref.getId(),
            "type", "REF",
            "private", false,
            "message_id", topic
      );
    }

    insert.executeBatch(batch);
  }

  public void addReplyEvent(User parentAuthor, int topicId, int commentId) {
    insert.execute(ImmutableMap.<String, Object>of(
            "userid", parentAuthor.getId(),
            "type", "REPLY",
            "private", false,
            "message_id", topicId,
            "comment_id", commentId
    ));
  }
}
