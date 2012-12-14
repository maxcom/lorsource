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

package ru.org.linux.topic.stub;

import org.springframework.beans.factory.annotation.Autowired;
import ru.org.linux.topic.Topic;
import ru.org.linux.topic.TopicListDao;
import ru.org.linux.topic.TopicListDto;
import ru.org.linux.user.User;

import java.util.ArrayList;
import java.util.List;

public class TestTopicListDaoImpl implements TopicListDao {
  @Autowired
  TopicListDto topicListDto;

  @Override
  public List<Topic> getTopics(TopicListDto topicListDto) {
    this.topicListDto.copy(topicListDto);
    return new ArrayList<Topic>();
  }

  @Override
  public List<TopicListDto.DeletedTopic> getDeletedTopics(Integer sectionId) {
    return null;
  }

  @Override
  public List<DeletedTopicForUser> getDeletedTopicsForUser(User user, int offset, int limit) {
    return null;
  }

  @Override
  public int getCountDeletedTopicsForUser(User user) {
    return 0;
  }
}
