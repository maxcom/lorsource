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

package ru.org.linux.spring;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.view.RedirectView;

import ru.org.linux.site.*;
import ru.org.linux.spring.dao.VoteDTO;

@Controller
public class VoteController {
  @RequestMapping(value="/vote.jsp", method= RequestMethod.POST)
  public ModelAndView vote(
    HttpServletRequest request,
    @RequestParam("vote") int vote,
    @RequestParam("voteid") int voteid
  ) throws Exception {
    Template tmpl = Template.getTemplate(request);

    if (!tmpl.isSessionAuthorized()) {
      throw new AccessViolationException("Not authorized");
    }

    Connection db = null;

    try {
      db = LorDataSource.getConnection();
      db.setAutoCommit(false);

      User user = User.getUser(db, tmpl.getNick());

      Poll poll = Poll.getCurrentPoll(db);

      if (voteid != poll.getId()) {
        throw new BadVoteException("голосовать можно только в текущий опрос");
      }

      Statement st = db.createStatement();

      ResultSet rs = st.executeQuery("SELECT * FROM vote_users WHERE vote="+voteid+" AND userid="+user.getId());

      if (!rs.next()) {
        if (st.executeUpdate("UPDATE votes SET votes=votes+1 WHERE id=" + vote + " AND vote=" + voteid) == 0) {
          throw new BadVoteException(vote, voteid);
        }

        st.executeUpdate("INSERT INTO vote_users VALUES("+voteid+", "+user.getId()+")");
      }

      rs.close();
      st.close();
      db.commit();

      return new ModelAndView(new RedirectView("view-message.jsp?msgid=" + poll.getTopicId() + "&highlight=" + vote));
    } finally {
      if (db != null) {
        db.close();
      }
    }
  }

  @RequestMapping(value="/vote-vote.jsp", method=RequestMethod.GET)
  public ModelAndView showForm(
    @RequestParam("msgid") int msgid,
    HttpServletRequest request
  ) throws Exception {
    if (!Template.isSessionAuthorized(request.getSession())) {
      throw new AccessViolationException("Not authorized");
    }

    Map<String, Object> params = new HashMap<String, Object>();

    params.put("msgid", msgid);

    Connection db = null;
    try {
      db = LorDataSource.getConnection();

      Poll poll = Poll.getPollByTopic(db, msgid);

      if (!poll.isCurrent()) {
        throw new BadVoteException("голосовать можно только в текущий опрос");
      }

      params.put("poll", poll);

      Statement st = db.createStatement();
      ResultSet rs = st.executeQuery("SELECT id, label FROM votes WHERE vote=" + poll.getId() + " ORDER BY id");

      List<VoteDTO> votes = new ArrayList<VoteDTO>();

      while (rs.next()) {
        VoteDTO dto = new VoteDTO();
        dto.setId(rs.getInt("id"));
        dto.setLabel(rs.getString("label"));
        dto.setPollId(poll.getId());
        votes.add(dto);
      }

      params.put("votes", votes);

      return new ModelAndView("vote-vote", params);
    } finally {
      if (db!=null) {
        db.close();
      }
    }
  }

  @RequestMapping("/view-vote.jsp")
  public ModelAndView viewVote(@RequestParam("vote") int voteid) throws Exception {
    Connection db = null;

    try {
      db = LorDataSource.getConnection();

      Poll poll = new Poll(db, voteid);

      return new ModelAndView(new RedirectView("/jump-message.jsp?msgid=" + poll.getTopicId()));
    } finally {
      if (db != null) {
        db.close();
      }
    }
  }
}