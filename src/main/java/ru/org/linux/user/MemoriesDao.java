/*
 * Copyright 1998-2014 Linux.org.ru
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

package ru.org.linux.user;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowCallbackHandler;
import org.springframework.jdbc.core.simple.SimpleJdbcInsert;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import ru.org.linux.topic.Topic;
import scala.Tuple2;

import javax.sql.DataSource;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

@Repository
public class MemoriesDao {
  private JdbcTemplate jdbcTemplate;
  private SimpleJdbcInsert insertTemplate;

  @Autowired
  public void setDataSource(DataSource ds) {
    jdbcTemplate = new JdbcTemplate(ds);
    insertTemplate = new SimpleJdbcInsert(ds).withTableName("memories").usingGeneratedKeyColumns("id").usingColumns("userid", "topic", "watch");
  }

  public int addToMemories(User user, Topic topic, boolean watch) {
    try {
      return doAddToMemories(user, topic, watch);
    } catch (DuplicateKeyException ignored) {
      return getId(user, topic.getId(), watch);
    }
  }

  @Transactional(rollbackFor = Exception.class, propagation = Propagation.REQUIRED)
  private int doAddToMemories(User user, Topic topic, boolean watch) {
    int id = getId(user, topic.getId(), watch);

    if (id==0) {
      return insertTemplate.executeAndReturnKey(ImmutableMap.<String, Object>of(
              "userid", user.getId(),
              "topic", topic.getId(),
              "watch", watch
      )).intValue();
    } else {
      return id;
    }
  }

  /**
   * Get memories id or 0 if not in memories
   */
  private int getId(User user, int topic, boolean watch) {
    List<Integer> res = jdbcTemplate.queryForList(
            "SELECT id FROM memories WHERE userid=? AND topic=? AND watch=?",
            Integer.class,
            user.getId(),
            topic,
            watch
    );

    if (res.isEmpty()) {
      return 0;
    } else {
      return res.get(0);
    }
  }

  /**
   * get number of memories/favs for topic
   */
  public MemoriesInfo getTopicInfo(int topic, User currentUser) {
    final List<Integer> res = Lists.newArrayList(0, 0);

    jdbcTemplate.query(
            "SELECT watch, count(*) FROM memories WHERE topic=? GROUP BY watch",
            new RowCallbackHandler() {
              @Override
              public void processRow(ResultSet rs) throws SQLException {
                if (rs.getBoolean("watch")) {
                  res.set(0, rs.getInt("count"));
                } else {
                  res.set(1, rs.getInt("count"));
                }
              }
            },
            topic
    );
    
    if (currentUser!=null) {
      List<Tuple2<Integer, Boolean>> ids = jdbcTemplate.query(
              "SELECT id, watch FROM memories WHERE userid=? AND topic=?",
              (rs, rowNum) -> Tuple2.apply(rs.getInt("id"), rs.getBoolean("watch")),
              currentUser.getId(),
              topic
      );

      int watchId = 0;
      int favsId = 0;

      for (Tuple2<Integer, Boolean> p : ids) {
        if (p._2()) {
          watchId = p._1();
        } else {
          favsId = p._1();
        }
      }

      return new MemoriesInfo(res.get(0), res.get(1), watchId, favsId);
    } else {
      return new MemoriesInfo(res.get(0), res.get(1), 0, 0);
    }
  }

  public MemoriesListItem getMemoriesListItem(int id) {
    List<MemoriesListItem> res = jdbcTemplate.query(
            "SELECT * FROM memories WHERE id=?",
            (rs, rowNum) -> new MemoriesListItem(rs),
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

  public boolean isWatchPresetForUser(User user) {
    return checkMemoriesPresent(user, true);
  }

  public boolean isFavPresetForUser(User user) {
    return checkMemoriesPresent(user, false);
  }

  private boolean checkMemoriesPresent(User user, boolean watch) {
    List<Integer> present = jdbcTemplate.queryForList(
            "select memories.id from memories join topics on memories.topic=topics.id where memories.userid=? and watch=? and not deleted limit 1;",
            Integer.class,
            user.getId(),
            watch
    );

    return !present.isEmpty();
  }

  /**
   * get number of watch memories for user
   * @param user user
   * @return count memories
   */
  public int getWatchCountForUser(User user) {
    List<Integer> ret = jdbcTemplate.queryForList("select count(id) from memories where userid=? and watch='t'", Integer.class, user.getId());
    if(ret.isEmpty()) {
      return 0;
    } else {
      return ret.get(0);
    }
  }
}
