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

package ru.org.linux.spring;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import javax.servlet.http.HttpServletRequest;
import org.springframework.context.support.ApplicationObjectSupport;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.ServletRequestUtils;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.view.RedirectView;

import ru.org.linux.site.*;
import ru.org.linux.util.HTMLFormatter;

@Controller
public class EditVoteController extends ApplicationObjectSupport {
  @RequestMapping(value="/edit-vote.jsp", method= RequestMethod.GET)
  public ModelAndView showForm(
    HttpServletRequest request,
    @RequestParam("msgid") int msgid
  ) throws Exception {
    Template tmpl = Template.getTemplate(request);

    if (!tmpl.isModeratorSession()) {
      throw new AccessViolationException("Not authorized");
    }

    Map<String, Object> params = new HashMap<String, Object>();
    params.put("msgid", msgid);

    Connection db = null;

    try {
      db = LorDataSource.getConnection();

      Poll poll = Poll.getPollByTopic(db, msgid);
      params.put("poll", poll);

      Message msg = Message.getMessage(db, msgid);
      params.put("msg", msg);

      List<PollVariant> variants = poll.getPollVariants(db, Poll.ORDER_ID);
      params.put("variants", variants);

      return new ModelAndView("edit-vote", params);
    } finally {
      if (db != null) {
        db.close();
      }
    }
  }

  @RequestMapping(value="/edit-vote.jsp", method= RequestMethod.POST)
  public ModelAndView editVote(
    HttpServletRequest request,
    @RequestParam int msgid,
    @RequestParam int id,
    @RequestParam String title,
    @RequestParam(defaultValue="false") boolean multiSelect
  ) throws Exception {
    Template tmpl = Template.getTemplate(request);

    if (!tmpl.isModeratorSession()) {
      throw new AccessViolationException("Not authorized");
    }

    Connection db = null;

    try {
      db = LorDataSource.getConnection();
      db.setAutoCommit(false);

      User user = tmpl.getCurrentUser();
      user.checkCommit();

      Poll poll = new Poll(db, id);

      PreparedStatement pstTopic = db.prepareStatement("UPDATE topics SET title=?,lastmod=CURRENT_TIMESTAMP WHERE id=?");
      pstTopic.setInt(2, msgid);
      pstTopic.setString(1, HTMLFormatter.htmlSpecialChars(title));

      pstTopic.executeUpdate();

      PreparedStatement pstPoll = db.prepareStatement("UPDATE votenames SET multiselect=? WHERE id=?");
      pstPoll.setBoolean(1, multiSelect);
      pstPoll.setInt(2, poll.getId());

      pstPoll.executeUpdate();

      List<PollVariant> variants = poll.getPollVariants(db, Poll.ORDER_ID);
      for (PollVariant var : variants) {
        String label = ServletRequestUtils.getRequiredStringParameter(request, "var" + var.getId());

        if (label == null || label.trim().length() == 0) {
          var.remove(db);
        } else {
          var.updateLabel(db, label);
        }
      }

      for (int i = 1; i <= 3; i++) {
        String label = ServletRequestUtils.getRequiredStringParameter(request, "new" + i);

        if (label != null && label.trim().length() > 0) {
          poll.addNewVariant(db, label);
        }
      }

      logger.info("Отредактирован опрос" + id + " пользователем " + user.getNick());

      db.commit();

      Random random = new Random();

      return new ModelAndView(new RedirectView("view-message.jsp?msgid=" + msgid + "&nocache=" + random.nextInt()));
    } finally {
      if (db != null) {
        db.close();
      }
    }
  }  
}
