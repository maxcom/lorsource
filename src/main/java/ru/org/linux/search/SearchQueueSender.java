/*
 * Copyright 1998-2024 Linux.org.ru
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

import com.google.common.base.Preconditions;
import com.google.common.collect.Lists;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.stereotype.Component;

import javax.jms.ConnectionFactory;
import javax.jms.Queue;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Component
public class SearchQueueSender {
  private final JmsTemplate jmsTemplate;
  private final JmsTemplate lowPriorityJmsTemplate;
  private final Queue queue;
  private static final Logger logger = LoggerFactory.getLogger(SearchQueueSender.class);

  public SearchQueueSender(@Qualifier("jmsConnectionFactory") ConnectionFactory jmsConnectionFactory, Queue queue) {
    jmsTemplate = new JmsTemplate(jmsConnectionFactory);
    jmsTemplate.setExplicitQosEnabled(true);
    lowPriorityJmsTemplate = new JmsTemplate(jmsConnectionFactory);
    lowPriorityJmsTemplate.setPriority(1);
    lowPriorityJmsTemplate.setExplicitQosEnabled(true);

    this.queue = queue;
  }

  public void updateMessageOnly(int msgid) {
    updateMessage(msgid, false);
  }

  public void updateMessage(final int msgid, final boolean withComments) {
    logger.info("Scheduling reindex #"+msgid+" withComments="+withComments);

    jmsTemplate.send(queue, session -> session.createObjectMessage(new UpdateMessage(msgid, withComments)));
  }

  public void updateMonth(final int year, final int month) {
    logger.info("Scheduling reindex by date "+year+ '/' +month);

    lowPriorityJmsTemplate.send(queue, session -> session.createObjectMessage(new UpdateMonth(year, month)));
  }

  public void updateComment(final int msgid) {
    Preconditions.checkArgument(msgid!=0, "msgid==0!?");

    jmsTemplate.send(queue, session -> session.createObjectMessage(new UpdateComments(Lists.newArrayList(msgid))));
  }

  public void updateComment(final List<Integer> msgids) {
    jmsTemplate.send(queue, session -> session.createObjectMessage(new UpdateComments(new ArrayList<>(msgids))));
  }

  public static class UpdateMessage implements Serializable {
    private final int msgid;
    private final boolean withComments;

    private static final long serialVersionUID = 5080317225175809364L;

    public UpdateMessage(int msgid, boolean withComments) {
      this.msgid = msgid;
      this.withComments = withComments;
    }

    public int getMsgid() {
      return msgid;
    }

    public boolean isWithComments() {
      return withComments;
    }
  }

  public static class UpdateComments implements Serializable {
    private final List<Integer> msgids;
    private static final long serialVersionUID = 8277563519169476453L;

    // force ArrayList to fix Scala serialization problems
    public UpdateComments(ArrayList<Integer> msgids) {
      this.msgids = msgids;
    }

    public List<Integer> getMsgids() {
      return Collections.unmodifiableList(msgids);
    }
  }

  public static class UpdateMonth implements Serializable {
    private final int year;
    private final int month;
    private static final long serialVersionUID = -7803422618174957487L;

    public UpdateMonth(int year, int month) {
      this.year = year;
      this.month = month;
    }

    public int getYear() {
      return year;
    }

    public int getMonth() {
      return month;
    }
  }
}
