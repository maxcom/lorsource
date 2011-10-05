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

package ru.org.linux.spring.boxlets;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.ModelAndView;
import ru.org.linux.site.Message;
import ru.org.linux.site.MessageNotFoundException;
import ru.org.linux.site.Poll;
import ru.org.linux.spring.commons.CacheProvider;
import ru.org.linux.spring.dao.MessageDao;
import ru.org.linux.spring.dao.PollDao;
import ru.org.linux.spring.dao.VoteDTO;

import javax.servlet.http.HttpServletRequest;
import java.util.List;

@Controller
public class PollBoxletImpl extends SpringBoxlet  {
  private CacheProvider cacheProvider;
  private PollDao pollDao;
  private MessageDao messageDao;

  @Autowired
  public void setPollDao(PollDao pollDao) {
    this.pollDao = pollDao;
  }

  @Autowired
  public void setCacheProvider(CacheProvider cacheProvider) {
    this.cacheProvider = cacheProvider;
  }

  @Autowired
  public void setMessageDao(MessageDao messageDao) {
    this.messageDao = messageDao;
  }

  @Override
  @RequestMapping("/poll.boxlet")
  protected ModelAndView getData(HttpServletRequest request) throws Exception {
    final Poll poll = getFromCache(cacheProvider, getCacheKey() + "poll", new GetCommand<Poll>() {
      @Override
      public Poll get() throws Exception {
        return pollDao.getCurrentPoll();
      }
    });

    List<VoteDTO> votes = getFromCache(cacheProvider, getCacheKey() + "votes"+poll.getId(), new GetCommand<List<VoteDTO>>() {
      @Override
      public List<VoteDTO> get() {
        return pollDao.getVoteDTO(poll.getId());
      }
    });

    Message msg = getFromCache(cacheProvider, getCacheKey() + "topic"+poll.getId(), new GetCommand<Message>() {
      @Override
      public Message get() throws MessageNotFoundException {
        return messageDao.getById(poll.getTopicId());
      }
    });

    Integer count = getFromCache(cacheProvider, getCacheKey() + "count"+poll.getId(), new GetCommand<Integer>() {
      @Override
      public Integer get() {
        return pollDao.getVotersCount(poll.getId());
      }
    });

    ModelAndView result = new ModelAndView("boxlets/poll");
    result.addObject("poll", poll);
    result.addObject("votes", votes);
    result.addObject("count", count);
    result.addObject("message", msg);
    return result;
  }

  @Override
  public int getExpiryTime() {
    return super.getExpiryTime() * 2;
  }
}
