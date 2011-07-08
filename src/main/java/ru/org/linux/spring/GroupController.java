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
import java.sql.SQLException;
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
  public static final int MAX_OFFSET = 300;

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
  @RequestMapping(value = {"/forum/{group}/{year}/{month}"})
  public ModelAndView forumArchive(
    @PathVariable("group") String groupName,
    @RequestParam(defaultValue = "0", value="offset") int offset,
    @PathVariable int year,
    @PathVariable int month,
    HttpServletRequest request
  ) throws Exception {
    return forum(groupName, offset, false, request, year, month);
  }

  @RequestMapping(value = "/forum/{group}")
  public ModelAndView forum(
    @PathVariable("group") String groupName,
    @RequestParam(defaultValue = "0", value="offset") int offset,
    @RequestParam(defaultValue = "false") boolean lastmod,
    HttpServletRequest request
  ) throws Exception {
    return forum(groupName, offset, lastmod, request, null, null);
  }

  private static ModelAndView forum(
    @PathVariable("group") String groupName,
    @RequestParam(defaultValue = "0", value="offset") int offset,
    @RequestParam(defaultValue = "false") boolean lastmod,
    HttpServletRequest request,
    Integer year,
    Integer month
  ) throws Exception {
    Map<String, Object> params = new HashMap<String, Object>();
    Template tmpl = Template.getTemplate(request);

    boolean showDeleted = request.getParameter("deleted") != null;
    params.put("showDeleted", showDeleted);

    Connection db = null;
    try {
      db = LorDataSource.getConnection();
      db.setAutoCommit(false);

      tmpl.updateCurrentUser(db);

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

      if (offset != 0) {
        firstPage = false;

        if (offset < 0) {
          throw new ServletParameterBadValueException("offset", "offset не может быть отрицательным");
        }

        if (year == null && offset>MAX_OFFSET) {
          return new ModelAndView(new RedirectView(group.getUrl()+"archive"));
        }
      } else {
        firstPage = true;
      }

      params.put("firstPage", firstPage);
      params.put("offset", offset);
      params.put("lastmod", lastmod);

      boolean showIgnored = false;
      if (request.getParameter("showignored") != null) {
        showIgnored = "t".equals(request.getParameter("showignored"));
      }

      params.put("showIgnored", showIgnored);

      params.put("group", group);
      params.put("groupInfo", new PreparedGroupInfo(db, group));

      params.put("section", new Section(db, group.getSectionId()));

      Map<Integer, String> ignoreList;

      if (tmpl.getCurrentUser()!=null) {
        ignoreList = IgnoreList.getIgnoreList(db, tmpl.getCurrentUser().getId());
      } else {
        ignoreList = Collections.emptyMap();
      }

      String ignq = "";

      if (!showIgnored && tmpl.isSessionAuthorized()) {
        if (firstPage && !ignoreList.isEmpty()) {
          ignq = " AND topics.userid NOT IN (SELECT ignored FROM ignore_list, users WHERE userid=users.id and nick='" + tmpl.getNick() + "')";
        }
      }

      params.put("ignoreList", ignoreList);

      Statement st = db.createStatement();
      String delq = showDeleted ? "" : " AND NOT deleted ";
      int topics = tmpl.getProf().getTopics();

      String q = "SELECT topics.title as subj, lastmod, userid, topics.id as msgid, deleted, topics.stat1, topics.stat3, topics.stat4, topics.sticky, topics.resolved FROM topics,groups, sections WHERE sections.id=groups.section AND (topics.moderate OR NOT sections.moderate) AND topics.groupid=groups.id AND groups.id=" + groupId + delq;

      if (year!=null) {
        if (year<1990 || year > 3000) {
          throw new ServletParameterBadValueException("year", "указан некорректный год");
        }

        if (month<1 || month > 12) {
          throw new ServletParameterBadValueException("month", "указан некорректный месяц");
        }

        q+=" AND postdate>='" + year + '-' + month + "-01'::timestamp AND (postdate<'" + year + '-' + month + "-01'::timestamp+'1 month'::interval)";
        params.put("year", year);
        params.put("month", month);
        params.put("url", group.getUrl()+year+ '/' +month+ '/');
      } else {
        params.put("url", group.getUrl());
      }

      ResultSet rs;

      if (!lastmod) {
        if (year==null) {
          if (offset==0) {
            q += " AND (sticky or postdate>CURRENT_TIMESTAMP-'3 month'::interval) ";
          }

          rs = st.executeQuery(q + ignq + " ORDER BY sticky DESC, msgid DESC LIMIT " + topics + " OFFSET " + offset);
        } else {
          rs = st.executeQuery(q + " ORDER BY msgid DESC LIMIT " + topics + " OFFSET " + offset);
        }
      } else {
        if (firstPage) {
          rs = st.executeQuery(q + ignq + " ORDER BY sticky DESC,lastmod DESC LIMIT " + topics + " OFFSET " + offset);
        } else {
          rs = st.executeQuery(q + " ORDER BY lastmod DESC LIMIT " + topics + " OFFSET " + offset);
        }
      }

      List<TopicsListItem> topicsList = new ArrayList<TopicsListItem>();
      int messages = tmpl.getProf().getMessages();

      while (rs.next()) {
        TopicsListItem topic = new TopicsListItem(db, rs, messages);

        // TODO: надо проверять просто ID в списке игнорирования
        User author = topic.getAuthor();

        if (!firstPage && !ignoreList.isEmpty() && ignoreList.containsValue(author.getNick())) {
          continue;
        }

        topicsList.add(topic);
      }

      params.put("topicsList", topicsList);

      if (year==null) {
        params.put("count", group.calcTopicsCount(db, showDeleted));
      } else {
        params.put("count", getArchiveCount(db, groupId, year, month));
      }

      return new ModelAndView("group", params);
    } finally {
      if (db != null) {
        db.close();
      }
    }
  }

  private static int getArchiveCount(Connection db, int groupid, int year, int month) throws SQLException {
    Statement st = db.createStatement();
    ResultSet rs = st.executeQuery("SELECT c FROM monthly_stats WHERE groupid="+groupid+" AND year="+year+" AND month="+month);
    if (!rs.next()) {
      return 0;
    } else {
      return rs.getInt(1);
    }
  }
}
