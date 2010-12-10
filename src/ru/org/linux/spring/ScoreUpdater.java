/*
 * Copyright 1998-2010 Linux.org.ru
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

import java.sql.Connection;
import java.sql.Statement;

import ru.org.linux.site.LorDataSource;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.jdbc.support.JdbcUtils;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class ScoreUpdater {
  private static final Log logger = LogFactory.getLog(ScoreUpdater.class);

  @Scheduled(cron="1 0 1 */2 * *")
  public void updateScore() throws Exception {
    logger.info("Updating score");

    Connection db = LorDataSource.getConnection();

    try {
      db.setAutoCommit(false);

      Statement st = db.createStatement();

      st.executeUpdate(
        "update users set score=score+1 " +
          "where id in " +
            "(select distinct comments.userid from comments, topics " +
            "where comments.postdate>CURRENT_TIMESTAMP-'2 days'::interval " +
            "and topics.id=comments.topic and " +
            "groupid!=8404 and groupid!=4068 and groupid!=19390 and " +
            "not comments.deleted and not topics.deleted)"
      );

      st.executeUpdate("update users set max_score=score where score>max_score");

      st.close();

      db.commit();
    } finally {
      JdbcUtils.closeConnection(db);
    }
  }
}
