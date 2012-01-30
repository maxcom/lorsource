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

package ru.org.linux.spring;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowCallbackHandler;
import org.springframework.jdbc.core.simple.SimpleJdbcCall;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

@Component
public class StatUpdater {
  private static final Log logger = LogFactory.getLog(StatUpdater.class);
  private static final int MAX_EVENTS = 1000;

  private SimpleJdbcCall statUpdate;
  private SimpleJdbcCall statUpdate2;
  private SimpleJdbcCall statMonthly;

  private JdbcTemplate jdbcTemplate;

  @Autowired
  public void setDataSource(DataSource dataSource) {
    statUpdate = new SimpleJdbcCall(dataSource).withFunctionName("stat_update");
    statUpdate2 = new SimpleJdbcCall(dataSource).withFunctionName("stat_update2");
    statMonthly = new SimpleJdbcCall(dataSource).withFunctionName("update_monthly_stats");
    jdbcTemplate = new JdbcTemplate(dataSource);
  }

  @Scheduled(fixedDelay=10*60*1000)
  public void updateStats() {
    logger.debug("Updating statistics");

    statUpdate.execute();
    statUpdate2.execute();
    statMonthly.execute();
  }

  @Scheduled(fixedDelay = 60*60*1000)
  public void creanEvents() {
    final List<Integer> deleteList = new ArrayList<Integer>();

    jdbcTemplate.query(
            "select userid, count(user_events.id) from user_events group by userid order by count desc limit 10",
            new RowCallbackHandler() {
              @Override
              public void processRow(ResultSet rs) throws SQLException {
                if (rs.getInt("count")>MAX_EVENTS) {
                  deleteList.add(rs.getInt("userid"));
                }
              }
            }
    );

    for (int id : deleteList) {
      logger.info("Cleaning messages for userid="+id);

      jdbcTemplate.update(
              "DELETE FROM user_events WHERE user_events.id IN (SELECT id FROM user_events WHERE userid=? ORDER BY event_date DESC OFFSET ?)",
              id,
              MAX_EVENTS
      );
    }
  }
}
