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
import javax.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.ModelAndView;

import ru.org.linux.site.LorDataSource;
import ru.org.linux.site.Template;
import ru.org.linux.site.User;

@Controller
public class ShowTopicsController {
  @RequestMapping("/show-topics.jsp")
  public ModelAndView showTopics(
    HttpServletRequest request, HttpServletResponse response,
    @RequestParam("nick") String nick,
    @RequestParam(value="offset", required=false) Integer offset
  ) throws Exception {
    Template tmpl = Template.getTemplate(request);

    Map<String, Object> params = new HashMap<String, Object>();

    params.put("nick", nick);

    boolean firstPage=offset==null;

    if (firstPage) {
      offset = 0;
    }

    params.put("offset", offset);
    params.put("firstPage", firstPage);

    int topicsInPage = tmpl.getProf().getInt("topics");
    params.put("topicsInPage", topicsInPage);

    Connection db = null;

    try {
      db = LorDataSource.getConnection();

      User user = User.getUser(db, nick);

      Statement st = db.createStatement();
      ResultSet rs = st.executeQuery("SELECT count(topics.id) FROM topics, users WHERE users.id=topics.userid AND users.id=" + user.getId() + " AND NOT deleted");

      int count = 0;
      int pages = 0;

      if (rs.next()) {
        count = rs.getInt("count");
        pages = count / topicsInPage;
        if (count % topicsInPage != 0) {
          count = (pages + 1) * topicsInPage;
        }
      }
      rs.close();

      params.put("count", count);
      params.put("pages", pages);

      if (firstPage || offset >= pages * topicsInPage) {
        response.setDateHeader("Expires", System.currentTimeMillis() + 90 * 1000);
      } else {
        response.setDateHeader("Expires", System.currentTimeMillis() + 30 * 24 * 60 * 60 * 1000L);
      }

      boolean showDeleted = false;

      if (Template.isSessionAuthorized(request.getSession())) {
        if (tmpl.isModeratorSession() || tmpl.getNick().equals(nick)) {
          showDeleted = true;
        }
      }

      if (firstPage) {
        rs = st.executeQuery("SELECT sections.name, groups.title as gtitle, topics.title as subj, topics.id as msgid, lastmod, userid, deleted, topics.stat1, topics.stat3, topics.stat4, topics.sticky FROM topics, groups, sections, users WHERE topics.groupid=groups.id AND sections.id=groups.section AND users.id=topics.userid AND users.id=" + user.getId() +(showDeleted?"":" AND NOT deleted")+" ORDER BY msgid DESC LIMIT " + topicsInPage);
      } else {
        rs = st.executeQuery("SELECT sections.name, groups.title as gtitle, topics.title as subj, topics.id as msgid, lastmod, userid, deleted, topics.stat1, topics.stat3, topics.stat4, topics.sticky FROM topics, groups, sections, users WHERE topics.groupid=groups.id AND sections.id=groups.section AND users.id=topics.userid AND users.id=" + user.getId() +(showDeleted?"":" AND NOT deleted")+" ORDER BY msgid ASC LIMIT " + topicsInPage + " OFFSET " + offset);
      }

      List<TopicsListItem> topicsList = new ArrayList<TopicsListItem>();
      int messagesInPage = tmpl.getProf().getInt("messages");

      while (rs.next()) {
        TopicsListItem item = new TopicsListItem(rs, messagesInPage);
        topicsList.add(item);
      }

      rs.close();
      st.close();

      params.put("topicsList", topicsList);
    } finally {
      if (db!=null) {
        db.close();
      }
    }

    return new ModelAndView("show-topics", params);
  }
}
