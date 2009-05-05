/*
 * Copyright 1998-2009 Linux.org.ru
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

import java.util.Map;
import java.util.HashMap;
import java.util.List;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.stereotype.Controller;
import org.springframework.beans.factory.annotation.Autowired;

import ru.org.linux.spring.dao.PollDaoImpl;
import ru.org.linux.site.Poll;
import ru.org.linux.site.PollNotFoundException;

/**
 * User: sreentenko
 * Date: 01.05.2009
 * Time: 23:51:26
 */
@Controller
public class PollBoxletImpl extends SpringBoxlet {

  private PollDaoImpl pollDao;

  public PollDaoImpl getPollDao() {
    return pollDao;
  }
  @Autowired
  public void setPollDao(PollDaoImpl pollDao) {
    this.pollDao = pollDao;
  }

  @RequestMapping("/poll.boxlet")
  protected ModelAndView getData(HttpServletRequest request, HttpServletResponse response) {
    Poll poll;
    try {
      poll = pollDao.getCurrentPoll();
    } catch (PollNotFoundException e) {
      throw new RuntimeException(e);
    }
    final List<PollDaoImpl.VoteDTO> votes = pollDao.getVoteDTO(poll.getId());

    Map<String, Object> model = new HashMap<String, Object>();
    model.put("poll", poll);
    model.put("votes", votes);
    model.put("count", pollDao.getVotersCount(poll.getId()));
    return new ModelAndView("boxlets/poll", model);
  }
}
