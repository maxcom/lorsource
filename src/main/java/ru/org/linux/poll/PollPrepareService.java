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

package ru.org.linux.poll;

import com.google.common.base.Function;
import com.google.common.collect.ImmutableSortedMap;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import ru.org.linux.topic.Topic;
import ru.org.linux.user.User;

import java.util.List;
import java.util.Map;

@Service
public class PollPrepareService {
  @Autowired
  private PollDao pollDao;

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
            pollDao.getCountUsers(poll),
            pollDao.getPollVariants(poll, Poll.ORDER_VOTES, user)
    );
  }

  public PreparedPoll preparePollPreview(Poll newPoll) {
    final Map<Integer,PollVariantResult> currentMap;

    if (newPoll.getId()>0) {
      currentMap = Maps.uniqueIndex(
              pollDao.getPollVariants(newPoll),
              new Function<PollVariantResult, Integer>() {
                @Override
                public Integer apply(PollVariantResult input) {
                  return input.getId();
                }
              }
      );
    } else {
      currentMap = ImmutableSortedMap.of();
    }

    List<PollVariantResult> variants = Lists.transform(
            newPoll.getVariants(),
            new Function<PollVariant, PollVariantResult>() {
              @Override
              public PollVariantResult apply(PollVariant input) {
                PollVariantResult pollVariant = currentMap.get(input.getId());

                if (pollVariant != null) {
                  return new PollVariantResult(input.getId(), input.getLabel(), pollVariant.getVotes(), pollVariant.getUserVoted());
                } else {
                  return new PollVariantResult(input.getId(), input.getLabel(), 0, false);
                }
              }
            }
    );

    return new PreparedPoll(newPoll, 0, variants);
  }
}
