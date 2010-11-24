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

import java.io.Serializable;

import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.Queue;
import javax.jms.Session;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Required;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.jms.core.MessageCreator;
import org.springframework.stereotype.Component;

@Component
public class SearchQueueSender {
  private JmsTemplate jmsTemplate;
  private Queue queue;

  @Autowired
  @Required
  public void setJmsTemplate(JmsTemplate jmsTemplate) {
    this.jmsTemplate = jmsTemplate;
  }

  @Autowired
  @Required
  public void setQueue(Queue queue) {
    this.queue = queue;
  }

  public void updateMessageOnly(final int msgid) {
    jmsTemplate.send(queue, new MessageCreator() {
      @Override
      public Message createMessage(Session session) throws JMSException {
        return session.createObjectMessage(new UpdateMessage(msgid));
      }
    });
  }

  public static class UpdateMessage implements Serializable {
    private final int msgid;

    private static final long serialVersionUID = 5080317225175809364L;

    public UpdateMessage(int msgid) {
      this.msgid = msgid;
    }

    public int getMsgid() {
      return msgid;
    }
  }
}
