/*
 * Copyright 1998-2012 Linux.org.ru
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

package ru.org.linux.group;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.rowset.SqlRowSet;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.view.RedirectView;
import ru.org.linux.ApplicationController;
import ru.org.linux.auth.AccessViolationException;
import ru.org.linux.section.SectionService;
import ru.org.linux.site.Template;
import ru.org.linux.section.Section;
import ru.org.linux.user.IgnoreListDao;
import ru.org.linux.user.UserDao;
import ru.org.linux.user.User;
import ru.org.linux.util.ServletParameterBadValueException;

import javax.servlet.http.HttpServletRequest;
import javax.sql.DataSource;
import java.util.*;

@Controller
public class GroupController extends ApplicationController {
  public static final int MAX_OFFSET = 300;

  @Autowired
  private GroupDao groupDao;

  @Autowired
  private SectionService sectionService;

  @Autowired
  private UserDao userDao;

  @Autowired
  private GroupInfoPrepareService prepareService;

  @Autowired
  private IgnoreListDao ignoreListDao;

  @Autowired
  private GroupPermissionService groupPermissionService;

  private JdbcTemplate jdbcTemplate;

  @Autowired
  public void setDataSource(DataSource ds) {
    jdbcTemplate = new JdbcTemplate(ds);
  }

  @RequestMapping("/group.jsp")
  public ModelAndView topics(
          @RequestParam("group") int groupId,
          @RequestParam(value = "offset", required = false) Integer offsetObject
  ) throws Exception {
    Group group = groupDao.getGroup(groupId);

    if (offsetObject != null) {
      Map<String, String> redirectParams = new HashMap<String, String>();
      redirectParams.put("offset", offsetObject.toString());
      return redirect(group.getUrl(), redirectParams);
    } else {
      return redirect(group.getUrl());
    }
  }

  @RequestMapping("/group-lastmod.jsp")
  public ModelAndView topicsLastmod(
          @RequestParam("group") int groupId,
          @RequestParam(value = "offset", required = false) Integer offsetObject
  ) throws Exception {
    Map<String, String> redirectParams = new HashMap<String, String>();
    redirectParams.put("lastmod", "true");

    if (offsetObject != null) {
      redirectParams.put("offset", offsetObject.toString());
    }

    Group group = groupDao.getGroup(groupId);
    return redirect(group.getUrl(), redirectParams);
  }

  @RequestMapping("/forum/{group}/{year}/{month}")
  public ModelAndView forumArchive(
    @PathVariable("group") String groupName,
    @RequestParam(defaultValue = "0", value="offset") int offset,
    @PathVariable int year,
    @PathVariable int month,
    HttpServletRequest request
  ) throws Exception {
    return forum(groupName, offset, false, request, year, month);
  }

  @RequestMapping("/forum/{group}")
  public ModelAndView forum(
    @PathVariable("group") String groupName,
    @RequestParam(defaultValue = "0", value="offset") int offset,
    @RequestParam(defaultValue = "false") boolean lastmod,
    HttpServletRequest request
  ) throws Exception {
    return forum(groupName, offset, lastmod, request, null, null);
  }

  private ModelAndView forum(
    @PathVariable("group") String groupName,
    @RequestParam(defaultValue = "0", value="offset") int offset,
    @RequestParam(defaultValue = "false") boolean lastmod,
    HttpServletRequest request,
    Integer year,
    Integer month
  ) throws Exception {
    ModelAndView modelAndView = new ModelAndView("group");
    Template tmpl = Template.getTemplate(request);

    boolean showDeleted = request.getParameter("deleted") != null;
    modelAndView.addObject("showDeleted", showDeleted);

    Section section = sectionService.getSection(Section.SECTION_FORUM);
    modelAndView.addObject("groupList", groupDao.getGroups(section));

    Group group = groupDao.getGroup(section, groupName);

    if (showDeleted && !"POST".equals(request.getMethod())) {
      return new ModelAndView(new RedirectView(group.getUrl()));
    }

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

    modelAndView.addObject("firstPage", firstPage);
    modelAndView.addObject("offset", offset);
    modelAndView.addObject("lastmod", lastmod);

    boolean showIgnored = false;
    if (request.getParameter("showignored") != null) {
      showIgnored = "t".equals(request.getParameter("showignored"));
    }

    modelAndView.addObject("showIgnored", showIgnored);

    modelAndView.addObject("group", group);

    modelAndView.addObject("section", section);

    Set<Integer> ignoreList;

    if (tmpl.getCurrentUser()!=null) {
      ignoreList = ignoreListDao.get(tmpl.getCurrentUser());
    } else {
      ignoreList = Collections.emptySet();
    }

    modelAndView.addObject("groupInfo", prepareService.prepareGroupInfo(group, request.isSecure()));

    String ignq = "";

    if (!showIgnored && tmpl.isSessionAuthorized()) {
      if (firstPage && !ignoreList.isEmpty()) {
        ignq = " AND topics.userid NOT IN (SELECT ignored FROM ignore_list WHERE userid=" + tmpl.getCurrentUser().getId() + ')';
      }
    }

    String delq = showDeleted ? "" : " AND NOT deleted ";
    int topics = tmpl.getProf().getTopics();

    String q = "SELECT topics.title as subj, lastmod, userid, topics.id as msgid, deleted, topics.stat1, topics.stat3, topics.stat4, topics.sticky, topics.resolved FROM topics WHERE topics.groupid=" + group.getId() + delq;

    if (year!=null) {
      if (year<1990 || year > 3000) {
        throw new ServletParameterBadValueException("year", "указан некорректный год");
      }

      if (month<1 || month > 12) {
        throw new ServletParameterBadValueException("month", "указан некорректный месяц");
      }

      q+=" AND postdate>='" + year + '-' + month + "-01'::timestamp AND (postdate<'" + year + '-' + month + "-01'::timestamp+'1 month'::interval)";
      modelAndView.addObject("year", year);
      modelAndView.addObject("month", month);
      modelAndView.addObject("url", group.getUrl()+year+ '/' +month+ '/');
    } else {
      modelAndView.addObject("url", group.getUrl());
    }

    SqlRowSet rs;

    if (!lastmod) {
      if (year==null) {
        if (offset==0) {
          q += " AND (sticky or postdate>CURRENT_TIMESTAMP-'3 month'::interval) ";
        }

        rs = jdbcTemplate.queryForRowSet(q + ignq + " ORDER BY sticky DESC, msgid DESC LIMIT " + topics + " OFFSET " + offset);
      } else {
        rs = jdbcTemplate.queryForRowSet(q + " ORDER BY msgid DESC LIMIT " + topics + " OFFSET " + offset);
      }
    } else {
      if (firstPage) {
        rs = jdbcTemplate.queryForRowSet(q + ignq + " ORDER BY sticky DESC,lastmod DESC LIMIT " + topics + " OFFSET " + offset);
      } else {
        rs = jdbcTemplate.queryForRowSet(q + " ORDER BY lastmod DESC LIMIT " + topics + " OFFSET " + offset);
      }
    }

    List<TopicsListItem> topicsList = new ArrayList<TopicsListItem>();
    int messages = tmpl.getProf().getMessages();

    while (rs.next()) {
      TopicsListItem topic = new TopicsListItem(userDao, rs, messages);

      // TODO: надо проверять просто ID в списке игнорирования
      User author = topic.getAuthor();

      if (!firstPage && !ignoreList.isEmpty() && ignoreList.contains(author.getId())) {
        continue;
      }

      topicsList.add(topic);
    }

    modelAndView.addObject("topicsList", topicsList);

    if (year == null) {
      modelAndView.addObject("count", groupDao.calcTopicsCount(group, showDeleted));
    } else {
      modelAndView.addObject("count", getArchiveCount(group.getId(), year, month));
    }

    modelAndView.addObject("addable", groupPermissionService.isTopicPostingAllowed(group, tmpl.getCurrentUser()));

    return render(modelAndView);
  }

  private int getArchiveCount(int groupid, int year, int month) {
    List<Integer> res = jdbcTemplate.queryForList("SELECT c FROM monthly_stats WHERE groupid=? AND year=? AND month=?", Integer.class, groupid, year, month);

    if (!res.isEmpty()) {
      return res.get(0);
    } else {
      return 0;
    }
  }
}
