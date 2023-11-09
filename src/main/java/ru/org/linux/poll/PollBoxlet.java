/*
 * Copyright 1998-2021 Linux.org.ru
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
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.ModelAndView;
import ru.org.linux.auth.AuthUtil;
import ru.org.linux.boxlets.AbstractBoxlet;
import ru.org.linux.topic.Topic;
import ru.org.linux.topic.TopicDao;
import ru.org.linux.user.User;

import javax.servlet.http.HttpServletRequest;

@Controller
public class PollBoxlet extends AbstractBoxlet {
  private PollDao pollDao;
  private TopicDao messageDao;

  @Autowired
  public void setPollDao(PollDao pollDao) {
    this.pollDao = pollDao;
  }

  @Autowired
  public void setMessageDao(TopicDao messageDao) {
    this.messageDao = messageDao;
  }

  @Override
  @RequestMapping("/poll.boxlet")
  protected ModelAndView getData(HttpServletRequest request) throws Exception {
    User currentUser  =AuthUtil.getCurrentUser();
    final Poll poll = pollDao.getMostRecentPoll(currentUser!=null? currentUser.getId() :0);

    Topic msg = messageDao.getById(poll.getTopic());

    int count = pollDao.getVotersCount(poll.getId());

    int countUsers = pollDao.getCountUsers(poll);

    ModelAndView result = new ModelAndView("boxlets/poll");
    result.addObject("poll", poll);
    result.addObject("count", count);
    result.addObject("message", msg);
    result.addObject("countUsers", countUsers);
    return result;
  }
}
