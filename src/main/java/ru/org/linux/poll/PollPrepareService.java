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

package ru.org.linux.poll;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import ru.org.linux.topic.Topic;
import ru.org.linux.user.User;

@Service
public class PollPrepareService {
  @Autowired
  private PollDao pollDao;

  /**
   * Функция подготовки опроса
   * @param poll опрос
   * @return подготовленный опрос
   */
  public PreparedPoll preparePoll(Poll poll) {
    return new PreparedPoll(
            poll,
            pollDao.getMaxVote(poll),
            pollDao.getCountUsers(poll),
            pollDao.getPollVariants(poll, Poll.ORDER_VOTES)
    );
  }

  /**
   * Функция подготовки опроса для пользователя
   * @param topic топик в котором опрос
   * @param user пользователь для которого подготавливается опрос
   * @return подготовленный опрос
   * @throws PollNotFoundException может не существовать опроса для этого топика
   */
  public PreparedPoll preparePoll(Topic topic, User user) throws PollNotFoundException {
    Poll poll = pollDao.getPollByTopicId(topic.getId());

    return new PreparedPoll(
            poll,
            pollDao.getMaxVote(poll),
            pollDao.getCountUsers(poll),
            pollDao.getPollVariants(poll, Poll.ORDER_VOTES, user)
    );
  }

  /**
   * Функция подготовки опроса
   * @param topic топик в котором опрос
   * @return подготовленный опрос
   * @throws PollNotFoundException может не существовать опроса для этого топика
   */
  public PreparedPoll preparePoll(Topic topic) throws PollNotFoundException {
    return preparePoll(topic, null);
  }
}
