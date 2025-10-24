/*
 * Copyright 1998-2023 Linux.org.ru
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

import com.typesafe.scalalogging.StrictLogging
import org.springframework.dao.DuplicateKeyException
import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.{RequestMapping, RequestMethod, RequestParam}
import org.springframework.web.servlet.ModelAndView
import org.springframework.web.servlet.view.RedirectView
import ru.org.linux.auth.AuthUtil.AuthorizedOnly
import ru.org.linux.topic.TopicDao
import ru.org.linux.user.UserErrorException

@Controller
class VoteController(pollDao: PollDao, topicDao: TopicDao) extends StrictLogging {
  @RequestMapping(value = Array("/vote.jsp"), method = Array(RequestMethod.POST))
  def vote(@RequestParam(value = "vote", required = false) votes: Array[Int],
           @RequestParam("voteid") voteid: Int): ModelAndView = AuthorizedOnly { currentUser =>
    val poll = pollDao.getPoll(voteid)

    val msg = topicDao.getById(poll.topic)

    if (!msg.commited) {
      throw new BadVoteException("Опрос еще не подтвержден")
    }

    if (msg.expired) {
      throw new BadVoteException("Опрос завершен")
    }

    if (votes == null || votes.length == 0) {
      throw new UserErrorException("ничего не выбрано")
    }

    if (!poll.multiSelect && votes.length != 1) {
      throw new BadVoteException("этот опрос допускает только один вариант ответа")
    }

    try {
      pollDao.updateVotes(voteid, votes, currentUser.user)
    } catch {
      case ex: DuplicateKeyException =>
        logger.debug("Vote already in database", ex)
    }

    new ModelAndView(new RedirectView(msg.getLink))
  }

}