/*
 * Copyright 1998-2016 Linux.org.ru
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

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.rowset.SqlRowSet;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.view.RedirectView;
import ru.org.linux.auth.AccessViolationException;
import ru.org.linux.section.Section;
import ru.org.linux.section.SectionService;
import ru.org.linux.site.Template;
import ru.org.linux.spring.SiteConfig;
import ru.org.linux.topic.ArchiveDao;
import ru.org.linux.topic.TopicTagService;
import ru.org.linux.user.IgnoreListDao;
import ru.org.linux.user.User;
import ru.org.linux.user.UserDao;
import ru.org.linux.util.BadImageException;
import ru.org.linux.util.ServletParameterBadValueException;
import ru.org.linux.util.image.ImageInfo;

import javax.annotation.Nullable;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.sql.DataSource;
import java.util.*;

@Controller
public class GroupController {
  public static final int MAX_OFFSET = 300;

  @Autowired
  private GroupDao groupDao;

  @Autowired
  private ArchiveDao archiveDao;

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

  @Autowired
  private TopicTagService topicTagService;

  @Autowired
  private SiteConfig siteConfig;

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
      return new ModelAndView(new RedirectView(group.getUrl() + "?offset=" + offsetObject.toString()));
    } else {
      return new ModelAndView(new RedirectView(group.getUrl()));
    }
  }

  @RequestMapping("/group-lastmod.jsp")
  public ModelAndView topicsLastmod(
          @RequestParam("group") int groupId,
          @RequestParam(value = "offset", required = false) Integer offsetObject
  ) throws Exception {
    Group group = groupDao.getGroup(groupId);

    if (offsetObject != null) {
      return new ModelAndView(new RedirectView(group.getUrl() + "?offset=" + offsetObject.toString() + "&lastmod=true"));
    } else {
      return new ModelAndView(new RedirectView(group.getUrl() + "?lastmod=true"));
    }
  }

  @RequestMapping("/forum/{group}/{year:\\d+}/{month:\\d+}")
  public ModelAndView forumArchive(
    @PathVariable("group") String groupName,
    @RequestParam(defaultValue = "0", value="offset") int offset,
    @PathVariable int year,
    @PathVariable int month,
    HttpServletRequest request,
    HttpServletResponse response
  ) throws Exception {
    return forum(groupName, offset, false, request, response, year, month);
  }

  @RequestMapping("/forum/{group}")
  public ModelAndView forum(
    @PathVariable("group") String groupName,
    @RequestParam(defaultValue = "0", value="offset") int offset,
    @RequestParam(defaultValue = "false") boolean lastmod,
    HttpServletRequest request,
    HttpServletResponse response
  ) throws Exception {
    return forum(groupName, offset, lastmod, request, response, null, null);
  }

  private List<TopicsListItem> getStickyTopics(
          Group group,
          int messagesInPage
  ) {
    String q =
            "SELECT topics.title as subj, lastmod, userid, topics.id as msgid, deleted, topics.stat1, topics.stat3, topics.sticky, topics.resolved " +
            "FROM topics WHERE sticky AND NOT deleted AND topics.groupid=? ORDER BY postdate DESC";

    SqlRowSet rs = jdbcTemplate.queryForRowSet(q, group.getId());

    return prepareTopic(rs, messagesInPage);
  }

  // TODO: move to dao/service
  private List<TopicsListItem> getTopics(
          Group group,
          int messagesInPage,
          boolean lastmod,
          Integer year,
          Integer month,
          int topics,
          int offset,
          boolean showDeleted,
          boolean showIgnored,
          @Nullable User currentUser
  ) {
    Set<Integer> ignoreList;

    if (currentUser!=null) {
      ignoreList = ignoreListDao.get(currentUser);
    } else {
      ignoreList = Collections.emptySet();
    }

   String delq = showDeleted ? "" : " AND NOT deleted ";

    String q = "SELECT topics.title as subj, lastmod, userid, topics.id as msgid, deleted, topics.stat1, topics.stat3, topics.sticky, topics.resolved " +
            "FROM topics WHERE NOT draft AND NOT sticky AND topics.groupid=" + group.getId() + delq;

    if (year!=null) {
      q+=" AND postdate>='" + year + '-' + month + "-01'::timestamp AND (postdate<'" + year + '-' + month + "-01'::timestamp+'1 month'::interval)";
    }

    String ignq = "";

    if (!showIgnored && currentUser!=null) {
      int currentUserId = currentUser.getId();
      if (!ignoreList.isEmpty()) {
        ignq = " AND topics.userid NOT IN (SELECT ignored FROM ignore_list WHERE userid=" + currentUserId + ')';
      }

      if (!currentUser.isModerator()) {
        ignq += " AND topics.id NOT IN (select distinct tags.msgid from tags, user_tags "
          + "where tags.tagid=user_tags.tag_id and user_tags.is_favorite = false and user_id=" + currentUserId + ") ";
      }
    }

    SqlRowSet rs;

    if (!lastmod) {
      if (year==null) {
        if (offset==0) {
          q += " AND postdate>CURRENT_TIMESTAMP-'3 month'::interval ";
        }

        rs = jdbcTemplate.queryForRowSet(q + ignq + " ORDER BY postdate DESC LIMIT " + topics + " OFFSET " + offset);
      } else {
        rs = jdbcTemplate.queryForRowSet(q + ignq + " ORDER BY postdate DESC LIMIT " + topics + " OFFSET " + offset);
      }
    } else {
      rs = jdbcTemplate.queryForRowSet(q + ignq + " ORDER BY lastmod DESC LIMIT " + topics + " OFFSET " + offset);
    }

    return prepareTopic(rs, messagesInPage);
  }

  private List<TopicsListItem> prepareTopic(
          SqlRowSet rs,
          int messagesInPage
  ) {
    List<TopicsListItem> topicsList = new ArrayList<>();

    while (rs.next()) {
      User author;

      author = userDao.getUserCached(rs.getInt("userid"));

      ImmutableList<String> tags = topicTagService.getTagsForTitle(rs.getInt("msgid"));

      TopicsListItem topic = new TopicsListItem(author, rs, messagesInPage, tags);

      topicsList.add(topic);
    }

    return topicsList;
  }

  private ModelAndView forum(
    @PathVariable("group") String groupName,
    @RequestParam(defaultValue = "0", value="offset") int offset,
    @RequestParam(defaultValue = "false") boolean lastmod,
    HttpServletRequest request,
    HttpServletResponse response,
    Integer year,
    Integer month
  ) throws Exception {
    Map<String, Object> params = new HashMap<>();
    Template tmpl = Template.getTemplate(request);

    boolean showDeleted = request.getParameter("deleted") != null;
    params.put("showDeleted", showDeleted);

    Section section = sectionService.getSection(Section.SECTION_FORUM);
    params.put("groupList", groupDao.getGroups(section));

    Group group = groupDao.getGroup(section, groupName);

    if (showDeleted && !"POST".equals(request.getMethod())) {
      return new ModelAndView(new RedirectView(group.getUrl()));
    }

    if (showDeleted && !tmpl.isSessionAuthorized()) {
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
    params.put("prevPage", offset - tmpl.getProf().getTopics());
    params.put("nextPage", offset + tmpl.getProf().getTopics());
    params.put("lastmod", lastmod);

    boolean showIgnored = false;
    if (request.getParameter("showignored") != null) {
      showIgnored = "t".equals(request.getParameter("showignored"));
    }

    params.put("showIgnored", showIgnored);

    params.put("group", group);

    if(group.getImage() != null) {
      try {
        params.put("groupImagePath", '/' + "tango" + group.getImage());
        ImageInfo info = new ImageInfo(siteConfig.getHTMLPathPrefix() + "tango" + group.getImage());
        params.put("groupImageInfo", info);
      } catch (BadImageException ex) {
        params.put("groupImagePath", null);
        params.put("groupImageInfo", null);
      }
    } else {
      params.put("groupImagePath", null);
      params.put("groupImageInfo", null);
    }

    params.put("section", section);

    params.put("groupInfo", prepareService.prepareGroupInfo(group, request.isSecure()));

    if (year!=null) {
      if (year<1990 || year > 3000) {
        throw new ServletParameterBadValueException("year", "указан некорректный год");
      }

      if (month<1 || month > 12) {
        throw new ServletParameterBadValueException("month", "указан некорректный месяц");
      }

      params.put("year", year);
      params.put("month", month);
      params.put("url", group.getUrl()+year+ '/' +month+ '/');
    } else {
      params.put("url", group.getUrl());
    }

    List<TopicsListItem> mainTopics = getTopics(
            group,
            tmpl.getProf().getMessages(),
            lastmod,
            year,
            month,
            tmpl.getProf().getTopics(),
            offset,
            showDeleted,
            showIgnored,
            tmpl.getCurrentUser()
    );

    if (year==null && offset==0 && !lastmod) {
      List<TopicsListItem> stickyTopics = getStickyTopics(group, tmpl.getProf().getMessages());

      params.put("topicsList",  Lists.newArrayList(Iterables.concat(stickyTopics, mainTopics)));
    } else {
      params.put("topicsList", mainTopics);
    }

    if (year != null) {
      params.put("hasNext", offset + tmpl.getProf().getTopics() < archiveDao.getArchiveCount(group.getId(), year, month));
    } else {
      params.put("hasNext", offset<MAX_OFFSET && mainTopics.size()==tmpl.getProf().getTopics());
    }

    params.put("addable", groupPermissionService.isTopicPostingAllowed(group, tmpl.getCurrentUser()));

    response.setDateHeader("Expires", System.currentTimeMillis() + 90 * 1000);

    return new ModelAndView("group", params);
  }

  @ExceptionHandler(GroupNotFoundException.class)
  @ResponseStatus(HttpStatus.NOT_FOUND)
  public ModelAndView handleNotFoundException() {
    return new ModelAndView("errors/code404");
  }
}
