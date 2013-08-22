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

package ru.org.linux.search;

import java.io.IOException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.solr.client.solrj.SolrServer;
import org.apache.solr.client.solrj.SolrServerException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class SolrOptimizer {
  private static final Log logger = LogFactory.getLog(SolrOptimizer.class);

  private SolrServer solrServer;

  @Autowired
  public void setSolrServer(SolrServer solrServer) {
    this.solrServer = solrServer;
  }

  @Scheduled(cron="0 0 1 * * *")
  public void optimize() throws IOException, SolrServerException {
    logger.info("Optimizing solr index");
    solrServer.optimize(true, true, 5);
    logger.info("Finished solr index optimization");
  }
}
