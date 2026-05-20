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

import jakarta.servlet.http.HttpServletRequest
import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.servlet.ModelAndView
import ru.org.linux.auth.AuthUtil
import ru.org.linux.boxlets.AbstractBoxlet
import ru.org.linux.topic.{Topic, TopicService}

import scala.jdk.CollectionConverters.*

@Controller
class PollBoxlet(pollDao: PollDao, topicService: TopicService) extends AbstractBoxlet:

  @RequestMapping(Array("/poll.boxlet"))
  override protected def getData(request: HttpServletRequest): ModelAndView =
    val poll = pollDao.getMostRecentPoll()
    val results = pollDao.getPollResults(poll, Poll.OrderId, AuthUtil.getCurrentUser)
    val userVoted = results.exists(_.userVoted)

    val msg = topicService.getById(poll.topic)
    val count = pollDao.getVotersCount(poll.id)
    val countUsers = pollDao.getCountUsers(poll)

    val result = new ModelAndView("boxlets/poll")
    result.addObject("poll", poll)
    result.addObject("count", count)
    result.addObject("message", msg)
    result.addObject("countUsers", countUsers)
    result.addObject("userVoted", userVoted)
    result.addObject("votedVariants", results.asJava)
    result
