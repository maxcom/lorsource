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

import javax.sql.DataSource;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.simple.SimpleJdbcCall;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class StatUpdater {
  private static final Log logger = LogFactory.getLog(StatUpdater.class);

  private SimpleJdbcCall statUpdate;
  private SimpleJdbcCall statUpdate2;
  private SimpleJdbcCall statMonthly;

  @Autowired
  public void setDataSource(DataSource dataSource) {
    statUpdate = new SimpleJdbcCall(dataSource).withFunctionName("stat_update");
    statUpdate2 = new SimpleJdbcCall(dataSource).withFunctionName("stat_update2");
    statMonthly = new SimpleJdbcCall(dataSource).withFunctionName("update_monthly_stats");
  }

  @Scheduled(fixedDelay=10*60*1000)
  public void updateStats() {
    logger.debug("Updating statistics");

    statUpdate.execute();
    statUpdate2.execute();
    statMonthly.execute();
  }
}
