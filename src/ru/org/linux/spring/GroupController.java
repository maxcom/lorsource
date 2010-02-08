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
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.*;

import javax.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.view.RedirectView;

import ru.org.linux.site.*;
import ru.org.linux.util.ServletParameterBadValueException;

@Controller
public class GroupController {
  @RequestMapping("/group.jsp")
  public ModelAndView topics(
    @RequestParam("group") int groupId,
    @RequestParam(value = "offset", required = false) Integer offsetObject
  ) throws Exception {
    Connection db = null;

    try {
      db = LorDataSource.getConnection();

      Group group = new Group(db, groupId);

      if (offsetObject != null) {
        return new ModelAndView(new RedirectView(group.getUrl() + "?offset=" + offsetObject.toString()));
      } else {
        return new ModelAndView(new RedirectView(group.getUrl()));
      }
    } finally {
      if (db != null) {
        db.close();
      }
    }
  }

  @RequestMapping("/group-lastmod.jsp")
  public ModelAndView topicsLastmod(
    @RequestParam("group") int groupId,
    @RequestParam(value = "offset", required = false) Integer offsetObject
  ) throws Exception {
    Connection db = null;

    try {
      db = LorDataSource.getConnection();

      Group group = new Group(db, groupId);

      if (offsetObject != null) {
        return new ModelAndView(new RedirectView(group.getUrl() + "?offset=" + offsetObject.toString() + "&lastmod=true"));
      } else {
        return new ModelAndView(new RedirectView(group.getUrl() + "?lastmod=true"));
      }
    } finally {
      if (db != null) {
        db.close();
      }
    }
  }

  @RequestMapping(value = "/forum/{group}")
  public ModelAndView forum(
    @PathVariable("group") String groupName,
    @RequestParam(required = false, value="offset") Integer offsetObject,
    @RequestParam(defaultValue = "false") boolean lastmod,
    HttpServletRequest request
  ) throws Exception {
    Map<String, Object> params = new HashMap<String, Object>();
    Template tmpl = Template.getTemplate(request);

    boolean showDeleted = request.getParameter("deleted") != null;
    params.put("showDeleted", showDeleted);

    Connection db = null;
    try {
      db = LorDataSource.getConnection();
      db.setAutoCommit(false);

      Section section = new Section(db, Section.SECTION_FORUM);
      Group group = section.getGroup(db, groupName);

      params.put("groupList", Group.getGroups(db, section));

      if (showDeleted && !"POST".equals(request.getMethod())) {
        return new ModelAndView(new RedirectView(group.getUrl()));
      }

      int groupId = group.getId();

      if (showDeleted && !Template.isSessionAuthorized(request.getSession())) {
        throw new AccessViolationException("Вы не авторизованы");
      }

      boolean firstPage;
      int offset;

      if (offsetObject != null) {
        offset = offsetObject;
        firstPage = false;

        if (offset < 0) {
          throw new ServletParameterBadValueException("offset", "offset не может быть отрицательным");
        }
      } else {
        firstPage = true;
        offset = 0;
      }

      params.put("firstPage", firstPage);
      params.put("offset", offset);
      params.put("lastmod", lastmod);

      boolean showIgnored = false;
      if (request.getParameter("showignored") != null) {
        showIgnored = "t".equals(request.getParameter("showignored"));
      }

      params.put("showIgnored", showIgnored);

      int messages = tmpl.getProf().getInt("messages");

      params.put("group", group);

      params.put("section", new Section(db, group.getSectionId()));

      String ignq = "";

      Map<Integer, String> ignoreList = IgnoreList.getIgnoreList(db, (String) request.getSession().getValue("nick"));

      if (!showIgnored && Template.isSessionAuthorized(request.getSession())) {
        if (firstPage && ignoreList != null && !ignoreList.isEmpty()) {
          ignq = " AND topics.userid NOT IN (SELECT ignored FROM ignore_list, users WHERE userid=users.id and nick='" + request.getSession().getValue("nick") + "')";
        }
      }

      params.put("ignoreList", ignoreList);

      Statement st = db.createStatement();
      ResultSet rs;
      String delq = showDeleted ? "" : " AND NOT deleted ";
      int topics = tmpl.getProf().getInt("topics");

      if (!lastmod) {
        if (firstPage) {
          rs = st.executeQuery("SELECT topics.title as subj, sections.name, lastmod, userid, topics.id as msgid, deleted, topics.stat1, topics.stat3, topics.stat4, topics.sticky, topics.resolved FROM topics,groups, sections WHERE sections.id=groups.section AND (topics.moderate OR NOT sections.moderate) AND topics.groupid=groups.id AND groups.id=" + groupId + delq + ignq + " AND (postdate>(CURRENT_TIMESTAMP-'3 month'::interval) or sticky) ORDER BY sticky desc,msgid DESC LIMIT " + topics);
        } else {
          rs = st.executeQuery("SELECT topics.title as subj, sections.name, lastmod, userid, topics.id as msgid, deleted, topics.stat1, topics.stat3, topics.stat4, topics.sticky,topics.resolved FROM topics,groups, sections WHERE sections.id=groups.section AND (topics.moderate OR NOT sections.moderate) AND topics.groupid=groups.id AND groups.id=" + groupId + delq + " ORDER BY sticky,msgid ASC LIMIT " + topics + " OFFSET " + offset);
        }
      } else {
        if (firstPage) {
          rs = st.executeQuery("SELECT topics.title as subj, sections.name, lastmod, userid, topics.id as msgid, deleted, topics.stat1, topics.stat3, topics.stat4, topics.sticky, topics.resolved FROM topics,groups, sections WHERE sections.id=groups.section AND (topics.moderate OR NOT sections.moderate) AND topics.groupid=groups.id AND groups.id=" + groupId + " AND NOT deleted " + ignq + " ORDER BY sticky DESC,lastmod DESC LIMIT " + topics + " OFFSET " + offset);
        } else {
          rs = st.executeQuery("SELECT topics.title as subj, sections.name, lastmod, userid, topics.id as msgid, deleted, topics.stat1, topics.stat3, topics.stat4, topics.sticky, topics.resolved FROM topics,groups, sections WHERE sections.id=groups.section AND (topics.moderate OR NOT sections.moderate) AND topics.groupid=groups.id AND groups.id=" + groupId + " AND NOT deleted ORDER BY sticky DESC,lastmod DESC LIMIT " + topics + " OFFSET " + offset);
        }
      }

      List<TopicsListItem> topicsList = new ArrayList<TopicsListItem>();

      while (rs.next()) {
        TopicsListItem topic = new TopicsListItem(rs, messages);

        // TODO: надо проверять просто ID в списке игнорирования
        User author = User.getUserCached(db, topic.getAuthor());

        if (!firstPage && ignoreList != null && !ignoreList.isEmpty() && ignoreList.containsValue(author.getNick())) {
          continue;
        }

        topicsList.add(topic);
      }

      if (!firstPage && !lastmod) {
        Collections.reverse(topicsList);
      }

      params.put("topicsList", topicsList);

      return new ModelAndView("group", params);
    } finally {
      if (db != null) {
        db.close();
      }
    }
  }
}
