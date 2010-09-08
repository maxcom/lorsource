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

import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.ModelAndView;

import ru.org.linux.site.LorDataSource;
import ru.org.linux.site.Message;
import ru.org.linux.site.MessageNotFoundException;
import ru.org.linux.site.Poll;
import ru.org.linux.spring.CacheableController;
import ru.org.linux.spring.commons.CacheProvider;
import ru.org.linux.spring.dao.PollDaoImpl;
import ru.org.linux.spring.dao.VoteDTO;

@Controller
public class PollBoxletImpl extends SpringBoxlet implements CacheableController {
  private CacheProvider cacheProvider;
  private PollDaoImpl pollDao;

  public PollDaoImpl getPollDao() {
    return pollDao;
  }

  @Autowired
  public void setPollDao(PollDaoImpl pollDao) {
    this.pollDao = pollDao;
  }

  @Autowired
  public void setCacheProvider(CacheProvider cacheProvider) {
    this.cacheProvider = cacheProvider;
  }

  @Override
  @RequestMapping("/poll.boxlet")
  protected ModelAndView getData(HttpServletRequest request, HttpServletResponse response) throws Exception {
    final Poll poll = getFromCache(cacheProvider, getCacheKey() + "poll", new GetCommand<Poll>() {
      @Override
      public Poll get() throws Exception {
        Connection db = null;

        try {
          db = LorDataSource.getConnection();

          return Poll.getCurrentPoll(db);
        } finally {
          if (db!=null) {
            db.close();
          }
        }
      }
    });

    List<VoteDTO> votes = getFromCache(cacheProvider, getCacheKey() + "votes", new GetCommand<List<VoteDTO>>() {
      @Override
      public List<VoteDTO> get() {
        return pollDao.getVoteDTO(poll.getId());
      }
    });

    Message msg = getFromCache(cacheProvider, getCacheKey() + "votes", new GetCommand<Message>() {
      @Override
      public Message get() throws SQLException, MessageNotFoundException {
        Connection db = null;

        try {
          db = LorDataSource.getConnection();

          return new Message(db, poll.getTopicId());
        } finally {
          if (db!=null) {
            db.close();
          }
        }
      }
    });

    Integer count = getFromCache(cacheProvider, getCacheKey() + "count", new GetCommand<Integer>() {
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
