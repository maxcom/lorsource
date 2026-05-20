/*
 * Copyright 1998-2026 Linux.org.ru
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
package ru.org.linux.poll

import org.springframework.stereotype.Service
import ru.org.linux.topic.Topic
import ru.org.linux.user.User

@Service
class PollPrepareService(pollDao: PollDao):

  @throws[PollNotFoundException]
  def preparePoll(topic: Topic, user: User): PreparedPoll =
    val poll = pollDao.getPollByTopicId(topic.id)
    PreparedPoll(poll, pollDao.getCountUsers(poll), pollDao.getPollResults(poll, Poll.OrderVotes, user))

  def preparePollPreview(newPoll: Poll): PreparedPoll =
    val currentMap: Map[Int, PollVariantResult] =
      if newPoll.id > 0 then
        pollDao.getPollResults(newPoll).map(v => v.id -> v).toMap
      else
        Map.empty

    val variants = newPoll
      .variants
      .map { input =>
        currentMap.get(input.id) match
          case Some(pollVariant) =>
            PollVariantResult(input.id, input.label, pollVariant.votes, pollVariant.userVoted)
          case None =>
            PollVariantResult(input.id, input.label, 0, false)
      }

    PreparedPoll(newPoll, 0, variants)
