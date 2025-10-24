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

package ru.org.linux.poll;

import com.google.common.collect.ImmutableList;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.ModelAndView;
import ru.org.linux.auth.AuthUtil;
import ru.org.linux.boxlets.AbstractBoxlet;
import ru.org.linux.topic.Topic;
import ru.org.linux.topic.TopicService;

@Controller
public class PollBoxlet extends AbstractBoxlet {
  private final PollDao pollDao;
  private final TopicService topicService;

  public PollBoxlet(PollDao pollDao, TopicService topicService) {
    this.pollDao = pollDao;
    this.topicService = topicService;
  }

  @Override
  @RequestMapping("/poll.boxlet")
  protected ModelAndView getData(HttpServletRequest request) {
    final Poll poll = pollDao.getMostRecentPoll();
    ImmutableList<PollVariantResult> results = pollDao.getPollResults(poll, Poll.OrderId(), AuthUtil.getCurrentUser());

    boolean userVoted = results.stream().anyMatch(PollVariantResult::isUserVoted);

    Topic msg = topicService.getById(poll.getTopic());
    int count = pollDao.getVotersCount(poll.getId());
    int countUsers = pollDao.getCountUsers(poll);


    ModelAndView result = new ModelAndView("boxlets/poll");

    result.addObject("poll", poll);
    result.addObject("count", count);
    result.addObject("message", msg);
    result.addObject("countUsers", countUsers);
    result.addObject("userVoted", userVoted);
    result.addObject("votedVariants", results);

    return result;
  }
}
