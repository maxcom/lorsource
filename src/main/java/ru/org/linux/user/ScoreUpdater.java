/*
 * Copyright 1998-2013 Linux.org.ru
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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import javax.sql.DataSource;

@Component
public class ScoreUpdater {
  private static final Log logger = LogFactory.getLog(ScoreUpdater.class);

  private JdbcTemplate jdbcTemplate;

  @Autowired
  public void setDataSource(DataSource dataSource) {
    jdbcTemplate = new JdbcTemplate(dataSource);
  }

  @Scheduled(cron="1 0 1 */2 * *")
  @Transactional
  public void updateScore() {
    logger.info("Updating score");

    jdbcTemplate.update("update users set score=score+1 " +
          "where id in " +
            "(select distinct comments.userid from comments, topics " +
            "where comments.postdate>CURRENT_TIMESTAMP-'2 days'::interval " +
            "and topics.id=comments.topic and " +
            "groupid!=8404 and groupid!=4068 and groupid!=19390 and " +
            "not comments.deleted and not topics.deleted)");

    updateMaxScore();
  }

  @Scheduled(cron="1 15 * * * *")
  public void updateMaxScore() {
    jdbcTemplate.update("update users set max_score=score where score>max_score");
  }

  @Scheduled(cron="0 1 * * * *")
  public void block() {
    jdbcTemplate.update("update users set blocked='t' where id in (select id from users where score<-50 and nick!='anonymous' and max_score<150 and not blocked)");
    jdbcTemplate.update("update users set blocked='t' where id in (select id from users where score<-50 and nick!='anonymous' and max_score<150 and blocked is null)");
  }

  @Scheduled(cron="0 1 2 * * *")
  public void deleteInactivated() {
    jdbcTemplate.update("delete from users where not activated and not blocked and regdate<CURRENT_TIMESTAMP-'1 week'::interval");
  }
}
