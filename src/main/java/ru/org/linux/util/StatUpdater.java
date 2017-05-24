/*
 * Copyright 1998-2017 Linux.org.ru
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

package ru.org.linux.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.simple.SimpleJdbcCall;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import ru.org.linux.user.UserEventService;

import javax.sql.DataSource;

@Component
public class StatUpdater {
  private static final Logger logger = LoggerFactory.getLogger(StatUpdater.class);
  private static final int MAX_EVENTS = 1000;

  private static final int FIVE_MINS = 5 * 60 * 1000;
  private static final int TEN_MINS = 10 * 60 * 1000;
  private static final int HOUR = 60 * 60 * 1000;

  private SimpleJdbcCall statUpdate;
  private SimpleJdbcCall statUpdate2;
  private SimpleJdbcCall statMonthly;

  @Autowired
  UserEventService userEventService;

  @Autowired
  public void setDataSource(DataSource dataSource) {
    statUpdate = new SimpleJdbcCall(dataSource).withFunctionName("stat_update");
    statUpdate2 = new SimpleJdbcCall(dataSource).withFunctionName("stat_update2");
    statMonthly = new SimpleJdbcCall(dataSource).withFunctionName("update_monthly_stats");
  }

  @Scheduled(fixedDelay=TEN_MINS, initialDelay = FIVE_MINS)
  public void updateStats() {
    logger.debug("Updating statistics");

    statUpdate.execute();
    statMonthly.execute();
  }

  @Scheduled(fixedDelay=HOUR, initialDelay = FIVE_MINS)
  public void updateGroupStats() {
    logger.debug("Updating group statistics");

    statUpdate2.execute();
  }

  @Scheduled(fixedDelay=HOUR, initialDelay = FIVE_MINS)
  public void cleanEvents() {
    userEventService.cleanupOldEvents(MAX_EVENTS);
  }
}
