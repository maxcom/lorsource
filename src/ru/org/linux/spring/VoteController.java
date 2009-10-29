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
import java.sql.Statement;
import java.sql.ResultSet;

import javax.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.view.RedirectView;

import ru.org.linux.site.*;

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
}