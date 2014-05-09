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

package ru.org.linux.site;

import ru.org.linux.topic.Topic;

public class MessageNotFoundException extends ScriptErrorException {

  private final Topic topic;
  private final int id;

  public MessageNotFoundException(Topic topic, int commentId, String info) {
    this.topic = topic;
    id =commentId;
  }
  
  public MessageNotFoundException(int topicId, String info) {
    super(info);
    id = topicId;
    topic = null;
  }

  public MessageNotFoundException(int id) {
    super("Сообщение #" + id + " не существует");
    this.id = id;
    topic = null;
  }

  public Topic getTopic() {
    return topic;
  }

  public int getId() {
    return id;
  }
}