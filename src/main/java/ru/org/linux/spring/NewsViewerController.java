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
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.ConnectionCallback;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.View;
import org.springframework.web.servlet.view.RedirectView;
import ru.org.linux.group.BadGroupException;
import ru.org.linux.group.Group;
import ru.org.linux.group.GroupDao;
import ru.org.linux.message.Message;
import ru.org.linux.message.MessagePrepareService;
import ru.org.linux.section.Section;
import ru.org.linux.section.SectionDao;
import ru.org.linux.site.*;
import ru.org.linux.tagcloud.TagCloudDao;
import ru.org.linux.spring.dao.UserDao;
import ru.org.linux.util.DateUtil;
import ru.org.linux.util.ServletParameterException;
import ru.org.linux.util.ServletParameterMissingException;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.sql.DataSource;
import java.net.URLEncoder;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;

@Controller
public class NewsViewerController {
  private static final String[] filterValues = { "all", "notalks", "tech"};
  private static final Set<String> filterValuesSet = new HashSet<String>(Arrays.asList(filterValues));

  @Autowired
  private SectionDao sectionDao;

  @Autowired
  private GroupDao groupDao;

  @Autowired
  private TagCloudDao tagCloudDao;

  @Autowired
  private MessagePrepareService prepareService;

  @Autowired
  private UserDao userDao;

  private JdbcTemplate jdbcTemplate;

  @Autowired
  public void setDataSource(DataSource ds) {
    jdbcTemplate = new JdbcTemplate(ds);
  }

  @RequestMapping(value = "/view-news.jsp", method = {RequestMethod.GET, RequestMethod.HEAD})
  public ModelAndView showNews(
          @RequestParam(value = "month", required = false) Integer month,
          @RequestParam(value = "year", required = false) Integer year,
          @RequestParam(value = "section", required = false) Integer sectionid,
          @RequestParam(value = "group", required = false) Integer groupid,
          @RequestParam(value = "tagcloud", required = false) String tag,
          @RequestParam(value = "offset", required = false) Integer offset,
          HttpServletRequest request,
          HttpServletResponse response
  ) throws Exception {
    Map<String, Object> params = new HashMap<String, Object>();

    params.put("url", "view-news.jsp");
    StringBuilder urlParams = new StringBuilder();

    Section section = null;

    if (sectionid != null) {
      urlParams.append("section=").append(Integer.toString(sectionid));

      section = sectionDao.getSection(sectionid);

      params.put("section", section);
      params.put("archiveLink", section.getArchiveLink());
    }

    if (tag != null) {
      if (urlParams.length() > 0) {
        urlParams.append('&');
      }

      urlParams.append("tagcloud=").append(URLEncoder.encode(tag));
    }

    if (groupid != null) {
      if (urlParams.length() > 0) {
        urlParams.append('&');
      }

      urlParams.append("group=").append(Integer.toString(groupid));
    }

    params.put("params", urlParams);

    if (month == null) {
      response.setDateHeader("Expires", System.currentTimeMillis() + 60 * 1000);
      response.setDateHeader("Last-Modified", System.currentTimeMillis());
    } else {
      long expires = System.currentTimeMillis() + 30 * 24 * 60 * 60 * 1000L;

      if (year == null) {
        throw new ServletParameterMissingException("year");
      }

      params.put("year", year);
      params.put("month", month);

      Calendar calendar = Calendar.getInstance();
      calendar.set(year, month - 1, 1);
      calendar.add(Calendar.MONTH, 1);

      long lastmod = calendar.getTimeInMillis();

      if (lastmod < System.currentTimeMillis()) {
        response.setDateHeader("Expires", expires);
        response.setDateHeader("Last-Modified", lastmod);
      } else {
        response.setDateHeader("Expires", System.currentTimeMillis() + 60 * 1000);
        response.setDateHeader("Last-Modified", System.currentTimeMillis());
      }
    }

    Group group = null;

    if (groupid != null) {
      group = groupDao.getGroup(groupid);

      if (group.getSectionId() != sectionid) {
        throw new ScriptErrorException("группа #" + groupid + " не принадлежит разделу #" + sectionid);
      }

      params.put("group", group);
    }

    if (tag != null) {
      TagCloudDao.checkTag(tag);
      params.put("tagcloud", tag);
    }

    if (section == null && tag == null) {
      throw new ServletParameterException("section or tagcloud required");
    }

    String navtitle;
    if (section != null) {
      navtitle = section.getName();
    } else {
      navtitle = tag;
    }

    if (group != null) {
      navtitle = "<a href=\"" + Section.getNewsViewerLink(group.getSectionId()) + "\">" + section.getName() + "</a> - <strong>" + group.getTitle() + "</strong>";
    }

    String ptitle;

    if (month == null) {
      if (section != null) {
        ptitle = section.getName();
        if (group != null) {
          ptitle += " - " + group.getTitle();
        }

        if (tag != null) {
          ptitle += " - " + tag;
        }
      } else {
        ptitle = tag;
      }
    } else {
      ptitle = "Архив: " + section.getName();

      if (group != null) {
        ptitle += " - " + group.getTitle();
      }

      if (tag != null) {
        ptitle += " - " + tag;
      }

      ptitle += ", " + year + ", " + DateUtil.getMonth(month);
      navtitle += " - Архив " + year + ", " + DateUtil.getMonth(month);
    }

    params.put("ptitle", ptitle);
    params.put("navtitle", navtitle);

    final NewsViewer newsViewer = new NewsViewer();

    if (section != null) {
      newsViewer.addSection(sectionid);
      if (section.isPremoderated()) {
        newsViewer.setCommitMode(NewsViewer.CommitMode.COMMITED_ONLY);
      } else {
        newsViewer.setCommitMode(NewsViewer.CommitMode.POSTMODERATED_ONLY);
      }
    }

    if (group != null) {
      newsViewer.setGroup(group.getId());
    }

    if (tag != null) {
      newsViewer.setTag(tagCloudDao.getTagId(tag));
    }

    offset = fixOffset(offset);

    if (month != null) {
      newsViewer.setDatelimit("postdate>='" + year + '-' + month + "-01'::timestamp AND (postdate<'" + year + '-' + month + "-01'::timestamp+'1 month'::interval)");
    } else if (tag == null && group == null) {
      if (!section.isPremoderated()) {
        newsViewer.setDatelimit("(postdate>(CURRENT_TIMESTAMP-'6 month'::interval))");
      }

      newsViewer.setLimit("LIMIT 20" + (offset > 0 ? (" OFFSET " + offset) : ""));
    } else {
      newsViewer.setLimit("LIMIT 20" + (offset > 0 ? (" OFFSET " + offset) : ""));
    }

    List<Message> messages = jdbcTemplate.execute(new ConnectionCallback<List<Message>>() {
      @Override
      public List<Message> doInConnection(Connection con) throws SQLException, DataAccessException {
        return newsViewer.getMessagesCached(con);
      }
    });

    params.put("messages", prepareService.prepareMessagesFeed(messages, request.isSecure()));

    params.put("offsetNavigation", month == null);
    params.put("offset", offset);

    if (section != null) {
      String rssLink = "/section-rss.jsp?section=" + section.getId();
      if (group != null) {
        rssLink += "&group=" + group.getId();
      }

      params.put("rssLink", rssLink);
    }

    return new ModelAndView("view-news", params);
  }

  @RequestMapping("/people/{nick}")
  public ModelAndView showUserTopicsNew(
    @PathVariable String nick,
    @RequestParam(value="offset", required=false) Integer offset,
    @RequestParam(value="output", required=false) String output,
    HttpServletRequest request,
    HttpServletResponse response
  ) throws Exception {
    Map<String, Object> params = new HashMap<String, Object>();

    params.put("url", "/people/" + nick + '/');
    params.put("whoisLink", "/people/" + nick + '/' + "profile");
// TODO    params.put("archiveLink", "/people/"+nick+"/archive/");

    response.setDateHeader("Expires", System.currentTimeMillis() + 60 * 1000);
    response.setDateHeader("Last-Modified", System.currentTimeMillis());

    User user = userDao.getUser(nick);
    UserInfo userInfo = userDao.getUserInfoClass(user);
    params.put("meLink", userInfo.getUrl());

    params.put("ptitle", "Сообщения " + user.getNick());
    params.put("navtitle", "Сообщения " + user.getNick());

    params.put("user", user);

    final NewsViewer newsViewer = new NewsViewer();

    offset = fixOffset(offset);

    newsViewer.setLimit("LIMIT 20" + (offset > 0 ? (" OFFSET " + offset) : ""));

    newsViewer.setCommitMode(NewsViewer.CommitMode.ALL);

    if (user.getId() == 2) {
      throw new UserErrorException("Лента для пользователя anonymous не доступна");
    }

    newsViewer.setUserid(user.getId());

    List<Message> messages = jdbcTemplate.execute(new ConnectionCallback<List<Message>>() {
      @Override
      public List<Message> doInConnection(Connection con) throws SQLException, DataAccessException {
        return newsViewer.getMessagesCached(con);
      }
    });

    boolean rss = output != null && "rss".equals(output);

    params.put("messages", prepareService.prepareMessagesFeed(messages, request.isSecure()));

    params.put("offsetNavigation", true);
    params.put("offset", offset);

    params.put("rssLink", "/people/" + nick + "/?output=rss");

    if (rss) {
      return new ModelAndView("section-rss", params);
    } else {
      return new ModelAndView("view-news", params);
    }
  }

  @RequestMapping("/people/{nick}/favs")
  public ModelAndView showUserFavs(
    @PathVariable String nick,
    @RequestParam(value="offset", required=false) Integer offset,
    @RequestParam(value="output", required=false) String output,
    HttpServletRequest request,
    HttpServletResponse response
  ) throws Exception {
    Map<String, Object> params = new HashMap<String, Object>();

    params.put("url", "/people/"+nick+ "/favs");
    params.put("whoisLink", "/people/"+nick+ '/' +"profile");

    response.setDateHeader("Expires", System.currentTimeMillis() + 60 * 1000);
    response.setDateHeader("Last-Modified", System.currentTimeMillis());

    User user = userDao.getUser(nick);
    UserInfo userInfo = userDao.getUserInfoClass(user);
    params.put("meLink", userInfo.getUrl());

    params.put("ptitle", "Избранные сообщения " + user.getNick());
    params.put("navtitle", "Избранные сообщения " + user.getNick());

    params.put("user", user);

    final NewsViewer newsViewer = new NewsViewer();

    offset = fixOffset(offset);

    newsViewer.setLimit("LIMIT 20" + (offset > 0 ? (" OFFSET " + offset) : ""));

    newsViewer.setCommitMode(NewsViewer.CommitMode.ALL);

    if (user.getId() == 2) {
      throw new UserErrorException("Лента для пользователя anonymous не доступна");
    }

    newsViewer.setUserid(user.getId());
    newsViewer.setUserFavs(true);

    List<Message> messages = jdbcTemplate.execute(new ConnectionCallback<List<Message>>() {
      @Override
      public List<Message> doInConnection(Connection con) throws SQLException, DataAccessException {
        return newsViewer.getMessagesCached(con);
      }
    });

    boolean rss = output != null && "rss".equals(output);

    params.put("messages", prepareService.prepareMessagesFeed(messages, request.isSecure()));

    params.put("offsetNavigation", true);
    params.put("offset", offset);

    params.put("rssLink", "/people/" + nick + "/favs?output=rss");

    if (rss) {
      return new ModelAndView("section-rss", params);
    } else {
      return new ModelAndView("view-news", params);
    }
  }

  private static int fixOffset(Integer offset) {
    if (offset!=null) {
      if (offset<0) {
          return 0;
      }

      if (offset>200) {
        return 200;
      }

      return offset;
    } else {
      return 0;
    }
  }

  @RequestMapping(value="/view-all.jsp", method={RequestMethod.GET, RequestMethod.HEAD})
  public ModelAndView viewAll(
    @RequestParam(value="section", required = false, defaultValue = "0") int sectionId,
    HttpServletRequest request
  ) throws Exception {

    ModelAndView modelAndView = new ModelAndView("view-all");

    Section section = null;

    if (sectionId != 0) {
      section = sectionDao.getSection(sectionId);
      modelAndView.getModel().put("section", section);
    }

    final NewsViewer newsViewer = new NewsViewer();
    newsViewer.setCommitMode(NewsViewer.CommitMode.UNCOMMITED_ONLY);
    newsViewer.setDatelimit("postdate>(CURRENT_TIMESTAMP-'1 month'::interval)");
    if (section != null) {
      newsViewer.addSection(section.getId());
    }

    List<Message> messages = jdbcTemplate.execute(new ConnectionCallback<List<Message>>() {
      @Override
      public List<Message> doInConnection(Connection con) throws SQLException, DataAccessException {
        return newsViewer.getMessages(con);
      }
    });

    modelAndView.getModel().put("messages", prepareService.prepareMessagesFeed(messages, request.isSecure()));

    RowMapper<DeletedTopic> mapper = new RowMapper<DeletedTopic>() {
      @Override
      public DeletedTopic mapRow(ResultSet rs, int rowNum) throws SQLException {
        return new DeletedTopic(rs);
      }
    };

    List<DeletedTopic> deleted;

    if (sectionId == 0) {
      deleted = jdbcTemplate.query(
              "SELECT topics.title as subj, nick, groups.section, groups.title as gtitle, topics.id as msgid, groups.id as guid, sections.name as ptitle, reason FROM topics,groups,users,sections,del_info WHERE sections.id=groups.section AND topics.userid=users.id AND topics.groupid=groups.id AND sections.moderate AND deleted AND del_info.msgid=topics.id AND topics.userid!=del_info.delby AND delDate is not null ORDER BY del_info.delDate DESC LIMIT 20",
              mapper
      );
    } else {
      deleted = jdbcTemplate.query(
              "SELECT topics.title as subj, nick, groups.section, groups.title as gtitle, topics.id as msgid, groups.id as guid, sections.name as ptitle, reason FROM topics,groups,users,sections,del_info WHERE sections.id=groups.section AND topics.userid=users.id AND topics.groupid=groups.id AND sections.moderate AND deleted AND del_info.msgid=topics.id AND topics.userid!=del_info.delby AND delDate is not null AND section=? ORDER BY del_info.delDate DESC LIMIT 20",
              mapper,
              sectionId
      );
    }

    modelAndView.getModel().put("deletedTopics", deleted);

    modelAndView.getModel().put("sections", sectionDao.getSectionsList());

    return modelAndView;
  }

  public static class DeletedTopic {
    private final String nick;
    private final int id;
    private final int groupId;
    private final String ptitle;
    private final String gtitle;
    private final String title;
    private final String reason;

    public DeletedTopic(ResultSet rs) throws SQLException {
      nick = rs.getString("nick");
      id = rs.getInt("msgid");
      groupId = rs.getInt("guid");
      ptitle = rs.getString("ptitle");
      gtitle = rs.getString("gtitle");
      title = rs.getString("subj");
      reason = rs.getString("reason");
    }

    public String getNick() {
      return nick;
    }

    public int getId() {
      return id;
    }

    public int getGroupId() {
      return groupId;
    }

    public String getPtitle() {
      return ptitle;
    }

    public String getGtitle() {
      return gtitle;
    }

    public String getTitle() {
      return title;
    }

    public String getReason() {
      return reason;
    }
  }

  @RequestMapping(value = "/show-topics.jsp", method = RequestMethod.GET)
  public View showUserTopics(
    @RequestParam("nick") String nick,
    @RequestParam(value="output", required=false) String output
  ) {
    if (output!=null) {
      return new RedirectView("/people/"+nick+"/?output=rss");
    }
    
    return new RedirectView("/people/"+nick+ '/');
  }

  @RequestMapping("/gallery/")
  public ModelAndView gallery(
    @RequestParam(required=false) Integer offset,
    HttpServletRequest request,
    HttpServletResponse response
  ) throws Exception {
    ModelAndView mv = showNews(null, null, Section.SECTION_GALLERY, null, null, offset, request, response);

    mv.getModel().put("url", "/gallery/");
    mv.getModel().put("params", null);

    return mv;
  }

  @RequestMapping("/forum/lenta")
  public ModelAndView forum(
    @RequestParam(required=false) Integer offset,
    HttpServletRequest request,
    HttpServletResponse response
  ) throws Exception {
    ModelAndView mv = showNews(null, null, Section.SECTION_FORUM, null, null, offset, request, response);

    mv.getModel().put("url", "/forum/lenta");
    mv.getModel().put("params", null);

    return mv;
  }

  @RequestMapping("/polls/")
  public ModelAndView polls(
    @RequestParam(required=false) Integer offset,
    HttpServletRequest request,
    HttpServletResponse response
  ) throws Exception {
    ModelAndView mv = showNews(null, null, Section.SECTION_POLLS, null, null, offset, request, response);

    mv.getModel().put("url", "/polls/");
    mv.getModel().put("params", null);

    return mv;
  }

  @RequestMapping("/news/")
  public ModelAndView news(
    @RequestParam(required=false) Integer offset,
    HttpServletRequest request,
    HttpServletResponse response
  ) throws Exception {
    ModelAndView mv = showNews(null, null, Section.SECTION_NEWS, null, null, offset, request, response);

    mv.getModel().put("url", "/news/");
    mv.getModel().put("params", null);

    return mv;
  }

  @RequestMapping("/gallery/{group}")
  public ModelAndView galleryGroup(
    @RequestParam(required=false) Integer offset,
    @PathVariable("group") String groupName,
    HttpServletRequest request,
    HttpServletResponse response
  ) throws Exception {
    return group(Section.SECTION_GALLERY, offset, groupName, request, response);
  }

  @RequestMapping("/news/{group}")
  public ModelAndView newsGroup(
    @RequestParam(required=false) Integer offset,
    @PathVariable("group") String groupName,
    HttpServletRequest request,
    HttpServletResponse response
  ) throws Exception {
    return group(Section.SECTION_NEWS, offset, groupName, request, response);
  }

  @RequestMapping("/polls/{group}")
  public ModelAndView pollsGroup(
    @RequestParam(required=false) Integer offset,
    @PathVariable("group") String groupName,
    HttpServletRequest request,
    HttpServletResponse response
  ) throws Exception {
    return group(Section.SECTION_POLLS, offset, groupName, request, response);
  }

  public ModelAndView group(
    int sectionId,
    Integer offset,
    String groupName,
    HttpServletRequest request,
    HttpServletResponse response
  ) throws Exception {
    Section section = sectionDao.getSection(sectionId);

    Group group = groupDao.getGroup(section, groupName);

    ModelAndView mv = showNews(null, null, group.getSectionId(), group.getId(), null, offset, request, response);

    mv.getModel().put("url", group.getUrl());
    mv.getModel().put("params", null);

    return mv;
  }

  @RequestMapping(value="/view-news.jsp", params={ "section" })
  public View oldLink(
    @RequestParam int section,
    @RequestParam(required=false) Integer offset,
    @RequestParam(value="month", required=false) Integer month,
    @RequestParam(value="year", required=false) Integer year,
    @RequestParam(value="group", required=false) Integer groupId
  ) throws Exception {
    if (offset!=null) {
      return new RedirectView(Section.getNewsViewerLink(section)+"?offset="+Integer.toString(offset));
    }

    if (year!=null && month!=null) {
      return new RedirectView(Section.getArchiveLink(section)+Integer.toString(year)+ '/' +Integer.toString(month));
    }

    if (groupId != null) {
      Group group = groupDao.getGroup(groupId);

      return new RedirectView(Section.getNewsViewerLink(section) + group.getUrlName() + '/');
    }

    return new RedirectView(Section.getNewsViewerLink(section));
  }

  @RequestMapping("/{section}/archive/{year}/{month}")
  public ModelAndView galleryArchive(
    @PathVariable String section,
    @PathVariable int year,
    @PathVariable int month,
    HttpServletRequest request,
    HttpServletResponse response
  ) throws Exception {
    ModelAndView mv = showNews(month, year, Section.getSection(section), null, null, null, request, response);

    mv.getModel().put("url", "/gallery/archive/"+year+ '/' +month+ '/');
    mv.getModel().put("params", null);

    return mv;
  }

  @RequestMapping("/section-rss.jsp")
  public ModelAndView showRSS(
    @RequestParam(value="filter", required = false) String filter,
    @RequestParam(value="section", required = false) Integer sectionId,
    @RequestParam(value="group", required = false) Integer groupId,
    HttpServletRequest request
  ) throws Exception {

    if (filter!=null && !filterValuesSet.contains(filter)) {
      throw new UserErrorException("Некорректное значение filter");
    }

    Map<String, Object> params = new HashMap<String, Object>();

    if (sectionId==null) {
      sectionId = 1;
    }

    if (groupId==null) {
      groupId = 0;
    }

    boolean notalks = filter!=null && "notalks".equals(filter);
    boolean tech = filter!=null && "tech".equals(filter);

    String userAgent = request.getHeader("User-Agent");
    final boolean feedBurner = userAgent!=null && userAgent.contains("FeedBurner");

    if (sectionId==1 && groupId==0 && !notalks && !tech && !feedBurner && request.getParameter("noredirect")==null) {
      return new ModelAndView(new RedirectView("http://feeds.feedburner.com/org/LOR"));
    }

    final NewsViewer nv = new NewsViewer();
    nv.addSection(sectionId);
    nv.setDatelimit(" postdate>(CURRENT_TIMESTAMP-'3 month'::interval) ");

    if (groupId !=0) {
      nv.setGroup(groupId);
    }

    nv.setNotalks(notalks);
    nv.setTech(tech);

    nv.setLimit("LIMIT 20");

    Section section = sectionDao.getSection(sectionId);
    params.put("section", section);

    if (section.isPremoderated()) {
      nv.setCommitMode(NewsViewer.CommitMode.COMMITED_ONLY);
    } else {
      nv.setCommitMode(NewsViewer.CommitMode.POSTMODERATED_ONLY);
    }

    Group group = null;
    if (groupId != 0) {
      group = groupDao.getGroup(groupId);

      if (group.getSectionId() != sectionId) {
        throw new BadGroupException("группа #" + groupId + " не принадлежит разделу #" + sectionId);
      }

      params.put("group", group);
    }

    String ptitle = section.getName();
    if (group != null) {
      ptitle += " - " + group.getTitle();
    }

    params.put("ptitle", ptitle);

    List<Message> messages = jdbcTemplate.execute(new ConnectionCallback<List<Message>>() {
      @Override
      public List<Message> doInConnection(Connection con) throws SQLException, DataAccessException {
        return feedBurner ? nv.getMessages(con) : nv.getMessagesCached(con);
      }
    });

    params.put("messages", prepareService.prepareMessagesFeed(messages, request.isSecure()));

    return new ModelAndView("section-rss", params);
  }
}
