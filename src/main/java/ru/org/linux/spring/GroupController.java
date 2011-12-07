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

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.rowset.SqlRowSet;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.view.RedirectView;
import ru.org.linux.dao.GroupDao;
import ru.org.linux.dao.IgnoreListDao;
import ru.org.linux.dao.SectionDao;
import ru.org.linux.dao.UserDao;
import ru.org.linux.dto.GroupDto;
import ru.org.linux.dto.SectionDto;
import ru.org.linux.dto.UserDto;
import ru.org.linux.exception.AccessViolationException;
import ru.org.linux.exception.util.ServletParameterBadValueException;
import ru.org.linux.site.*;

import javax.servlet.http.HttpServletRequest;
import javax.sql.DataSource;
import java.util.*;

@Controller
public class GroupController {
  public static final int MAX_OFFSET = 300;

  @Autowired
  private GroupDao groupDao;

  @Autowired
  private SectionDao sectionDao;

  @Autowired
  private UserDao userDao;

  @Autowired
  private PrepareService prepareService;

  @Autowired
  private IgnoreListDao ignoreListDao;

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
    GroupDto groupDto = groupDao.getGroup(groupId);

    if (offsetObject != null) {
      return new ModelAndView(new RedirectView(groupDto.getUrl() + "?offset=" + offsetObject.toString()));
    } else {
      return new ModelAndView(new RedirectView(groupDto.getUrl()));
    }
  }

  @RequestMapping("/group-lastmod.jsp")
  public ModelAndView topicsLastmod(
      @RequestParam("group") int groupId,
      @RequestParam(value = "offset", required = false) Integer offsetObject
  ) throws Exception {
    GroupDto groupDto = groupDao.getGroup(groupId);

    if (offsetObject != null) {
      return new ModelAndView(new RedirectView(groupDto.getUrl() + "?offset=" + offsetObject.toString() + "&lastmod=true"));
    } else {
      return new ModelAndView(new RedirectView(groupDto.getUrl() + "?lastmod=true"));
    }
  }

  @RequestMapping(value = {"/forum/{group}/{year}/{month}"})
  public ModelAndView forumArchive(
      @PathVariable("group") String groupName,
      @RequestParam(defaultValue = "0", value = "offset") int offset,
      @PathVariable int year,
      @PathVariable int month,
      HttpServletRequest request
  ) throws Exception {
    return forum(groupName, offset, false, request, year, month);
  }

  @RequestMapping(value = "/forum/{group}")
  public ModelAndView forum(
      @PathVariable("group") String groupName,
      @RequestParam(defaultValue = "0", value = "offset") int offset,
      @RequestParam(defaultValue = "false") boolean lastmod,
      HttpServletRequest request
  ) throws Exception {
    return forum(groupName, offset, lastmod, request, null, null);
  }

  private ModelAndView forum(
      @PathVariable("group") String groupName,
      @RequestParam(defaultValue = "0", value = "offset") int offset,
      @RequestParam(defaultValue = "false") boolean lastmod,
      HttpServletRequest request,
      Integer year,
      Integer month
  ) throws Exception {
    Map<String, Object> params = new HashMap<String, Object>();
    Template tmpl = Template.getTemplate(request);

    boolean showDeleted = request.getParameter("deleted") != null;
    params.put("showDeleted", showDeleted);

    SectionDto sectionDto = sectionDao.getSection(SectionDto.SECTION_FORUM);
    params.put("groupList", groupDao.getGroups(sectionDto));

    GroupDto groupDto = groupDao.getGroup(sectionDto, groupName);

    if (showDeleted && !"POST".equals(request.getMethod())) {
      return new ModelAndView(new RedirectView(groupDto.getUrl()));
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

      if (year == null && offset > MAX_OFFSET) {
        return new ModelAndView(new RedirectView(groupDto.getUrl() + "archive"));
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

    params.put("group", groupDto);

    params.put("section", sectionDto);

    Set<Integer> ignoreList;

    if (tmpl.getCurrentUser() != null) {
      ignoreList = ignoreListDao.get(tmpl.getCurrentUser());
    } else {
      ignoreList = Collections.emptySet();
    }

    params.put("groupInfo", prepareService.prepareGroupInfo(groupDto, request.isSecure()));

    String ignq = "";

    if (!showIgnored && tmpl.isSessionAuthorized()) {
      if (firstPage && !ignoreList.isEmpty()) {
        ignq = " AND topics.userid NOT IN (SELECT ignored FROM ignore_list WHERE userid=" + tmpl.getCurrentUser().getId() + ')';
      }
    }

    String delq = showDeleted ? "" : " AND NOT deleted ";
    int topics = tmpl.getProf().getTopics();

    String q = "SELECT topics.title as subj, lastmod, userid, topics.id as msgid, deleted, topics.stat1, topics.stat3, topics.stat4, topics.sticky, topics.resolved FROM topics,groups WHERE topics.groupid=groups.id AND groups.id=" + groupDto.getId() + delq;

    if (year != null) {
      if (year < 1990 || year > 3000) {
        throw new ServletParameterBadValueException("year", "указан некорректный год");
      }

      if (month < 1 || month > 12) {
        throw new ServletParameterBadValueException("month", "указан некорректный месяц");
      }

      q += " AND postdate>='" + year + '-' + month + "-01'::timestamp AND (postdate<'" + year + '-' + month + "-01'::timestamp+'1 month'::interval)";
      params.put("year", year);
      params.put("month", month);
      params.put("url", groupDto.getUrl() + year + '/' + month + '/');
    } else {
      params.put("url", groupDto.getUrl());
    }

    SqlRowSet rs;

    if (!lastmod) {
      if (year == null) {
        if (offset == 0) {
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
      UserDto author = topic.getAuthor();

      if (!firstPage && !ignoreList.isEmpty() && ignoreList.contains(author.getId())) {
        continue;
      }

      topicsList.add(topic);
    }

    params.put("topicsList", topicsList);

    if (year == null) {
      params.put("count", groupDao.calcTopicsCount(groupDto, showDeleted));
    } else {
      params.put("count", getArchiveCount(groupDto.getId(), year, month));
    }

    return new ModelAndView("group", params);
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
