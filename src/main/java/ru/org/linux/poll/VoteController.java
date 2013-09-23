/*
 * Copyright 1998-2013 Linux.org.ru
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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.view.RedirectView;
import ru.org.linux.auth.AccessViolationException;
import ru.org.linux.site.Template;
import ru.org.linux.topic.Topic;
import ru.org.linux.topic.TopicDao;
import ru.org.linux.user.User;
import ru.org.linux.user.UserErrorException;

import javax.servlet.ServletRequest;
import javax.servlet.http.HttpServletRequest;
import java.util.HashMap;
import java.util.Map;

@Controller
public class VoteController {
  private static final Logger logger = LoggerFactory.getLogger(VoteController.class);

  @Autowired
  private PollDao pollDao;

  @Autowired
  private TopicDao messageDao;

  @RequestMapping(value="/vote.jsp", method= RequestMethod.POST)
  public ModelAndView vote(
    ServletRequest request,
    @RequestParam(value="vote", required = false) int[] votes,
    @RequestParam("voteid") int voteid
  ) throws Exception {
    Template tmpl = Template.getTemplate(request);

    if (!tmpl.isSessionAuthorized()) {
      throw new AccessViolationException("Not authorized");
    }

    User user = tmpl.getCurrentUser();

    Poll poll = pollDao.getCurrentPoll();
    Topic msg = messageDao.getById(poll.getTopicId());

    if (voteid != poll.getId()) {
      throw new BadVoteException("голосовать можно только в текущий опрос");
    }

    if (votes==null || votes.length==0) {
      throw new UserErrorException("ничего не выбрано");
    }

    if (!poll.isMultiSelect() && votes.length!=1) {
      throw new BadVoteException("этот опрос допускает только один вариант ответа");
    }

    try {
      pollDao.updateVotes(voteid, votes, user);
    } catch (DuplicateKeyException ex) {
      logger.info("Failed to vote", ex);
    }

    return new ModelAndView(new RedirectView(msg.getLink()));
  }

  @RequestMapping(value="/vote-vote.jsp", method=RequestMethod.GET)
  public ModelAndView showForm(
    @RequestParam("msgid") int msgid,
    HttpServletRequest request
  ) throws Exception {
    Template tmpl = Template.getTemplate(request);

    if (!tmpl.isSessionAuthorized()) {
      throw new AccessViolationException("Not authorized");
    }

    Map<String, Object> params = new HashMap<>();

    Topic msg = messageDao.getById(msgid);
    params.put("message", msg);

    Poll poll = pollDao.getPollByTopicId(msgid);
    if (!poll.isCurrent()) {
      throw new BadVoteException("голосовать можно только в текущий опрос");
    }
    params.put("poll", poll);

    return new ModelAndView("vote-vote", params);
  }

  @RequestMapping("/view-vote.jsp")
  public ModelAndView viewVote(@RequestParam("vote") int voteid) throws Exception {
    Poll poll = pollDao.getPoll(voteid);
    return new ModelAndView(new RedirectView("/jump-message.jsp?msgid=" + poll.getTopicId()));
  }
}