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

import java.sql.*;
import java.util.*;

import javax.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.ModelAndView;

import ru.org.linux.site.*;

@Controller
public class TrackerController {
  private static final String[] filterValues = { "all", "notalks", "tech", "mine" };
  private static final Set<String> filterValuesSet = new HashSet<String>(Arrays.asList(filterValues));

  @RequestMapping("/tracker.jsp")
  public ModelAndView tracker(
    @RequestParam(value="filter", required = false) String filter,
    @RequestParam(value="offset", required = false) Integer offset,
    HttpServletRequest request) throws Exception {

    if (filter!=null && !filterValuesSet.contains(filter)) {
      throw new UserErrorException("Некорректное значение filter");
    }

    if (offset==null) {
      offset = 0;
    } else {
      if (offset<0 || offset>300) {
        throw new UserErrorException("Некорректное значение offset");
      }
    }

    boolean noTalks = filter!=null && filter.equals("notalks");
    boolean tech = filter!=null && filter.equals("tech");
    boolean mine = filter!=null && filter.equals("mine");

    Map<String, Object> params = new HashMap<String, Object>();

    params.put("mine", mine);
    params.put("offset", offset);

    params.put("filter", filter);

    String dateLimit = mine?"6 month":"24 hours";

    Template tmpl = Template.getTemplate(request);
    int messages = tmpl.getProf().getInt("messages");
    int topics = tmpl.getProf().getInt("topics");

    params.put("topics", topics);

    if (filter!=null) {
      params.put("query", "&filter="+filter);
    } else {
      params.put("query", "");      
    }

    Connection db = null;

    try {
      db = LorDataSource.getConnection();

      User user = Template.getCurrentUser(db, request.getSession());

      if (mine) {
        if (!tmpl.isSessionAuthorized()) {
          throw new UserErrorException("Not authorized");
        }
      }

      String sSql = "SELECT " +
        "t.userid as author, t.id, lastmod, t.stat1 AS stat1, t.stat3 AS stat3, t.stat4 AS stat4, g.id AS gid, g.title AS gtitle, t.title AS title, comments.id as cid, comments.userid AS last_comment_by, t.resolved as resolved,section,urlname,comments.postdate, sections.moderate as smod, t.moderate " +
        "FROM topics AS t, groups AS g, comments, sections " +
        "WHERE g.section=sections.id AND not t.deleted AND t.id=comments.topic AND t.groupid=g.id " +
        "AND comments.id=(SELECT id FROM comments WHERE NOT deleted AND comments.topic=t.id ORDER BY postdate DESC LIMIT 1) " +
        "AND t.lastmod > CURRENT_TIMESTAMP - interval '" + dateLimit + "' " +
        (user!=null?"AND t.userid NOT IN (select ignored from ignore_list where userid="+user.getId()+") ":"")
        + (noTalks ? " AND not t.groupid=8404" : "")
        + (tech ? " AND not t.groupid=8404 AND not t.groupid=4068 AND section=2" : "")
        + (mine ? " AND t.userid=" + user.getId() : "") +
        "UNION ALL SELECT t.userid as author, t.id, lastmod,  t.stat1 AS stat1, t.stat3 AS stat3, t.stat4 AS stat4, g.id AS gid, g.title AS gtitle, t.title AS title, 0, 0, t.resolved as resolved,section,urlname,postdate, sections.moderate as smod, t.moderate " +
        "FROM topics AS t, groups AS g, sections " +
        "WHERE sections.id=g.section AND not t.deleted AND t.postdate > CURRENT_TIMESTAMP - interval '" + dateLimit + "' " +
        "AND t.stat1=0 AND g.id=t.groupid " +
        (user!=null?"AND t.userid NOT IN (select ignored from ignore_list where userid="+user.getId()+") ":"")
        + (noTalks ? " AND not t.groupid=8404" : "")
        + (tech ? " AND not t.groupid=8404 AND not t.groupid=4068 AND section=2" : "")
        + (mine ? " AND t.userid=" + user.getId() : "") +
        "ORDER BY lastmod DESC LIMIT "+ topics +" OFFSET "+offset;

      Statement st = db.createStatement();
      ResultSet rs = st.executeQuery(sSql);

      List<Item> msgs = new ArrayList<Item>();

      while (rs.next()) {
        msgs.add(new Item(rs, messages));
      }

      params.put("msgs", msgs);

      if (tmpl.isModeratorSession() && !mine) {
        params.put("newUsers", getNewUsers(db));
      }

      return new ModelAndView("tracker", params);
    } finally {
      if (db!=null) {
        db.close();
      }
    }
  }

  public List<User> getNewUsers(Connection db) throws SQLException, UserNotFoundException {
    Statement st = db.createStatement();

    ResultSet rs = st.executeQuery("SELECT id FROM users where regdate IS NOT null " +
          "AND regdate > CURRENT_TIMESTAMP - interval '3 days' ORDER BY regdate");

    List<User> list = new ArrayList<User>();

    while (rs.next()) {
      list.add(User.getUser(db, rs.getInt("id")));
    }

    st.close();

    return list;
  }

  public class Item {
    private final int author;
    private final int msgid;
    private final Timestamp lastmod;
    private final int stat1;
    private final int stat3;
    private final int stat4;
    private final int groupId;
    private final String groupTitle;
    private final String title;
    private final int pages;
    private final int cid;
    private final int lastCommentBy;
    private final boolean resolved;
    private final int section;
    private final String groupUrlName;
    private final Timestamp postdate;
    private final boolean uncommited;

    public Item(ResultSet rs, int messagesInPage) throws SQLException {
      author = rs.getInt("author");
      msgid = rs.getInt("id");
      lastmod = rs.getTimestamp("lastmod");
      stat1 = rs.getInt("stat1");
      stat3 = rs.getInt("stat3");
      stat4 = rs.getInt("stat4");
      groupId = rs.getInt("gid");
      groupTitle = rs.getString("gtitle");
      title = rs.getString("title");
      cid = rs.getInt("cid");
      lastCommentBy = rs.getInt("last_comment_by");
      resolved = rs.getBoolean("resolved");
      section = rs.getInt("section");
      groupUrlName = rs.getString("urlname");
      postdate = rs.getTimestamp("postdate");
      uncommited = rs.getBoolean("smod") && ! rs.getBoolean("moderate");

      pages = Message.getPageCount(stat1, messagesInPage);
    }

    public int getAuthor() {
      return author;
    }

    public int getMsgid() {
      return msgid;
    }

    public int getStat1() {
      return stat1;
    }

    public int getStat3() {
      return stat3;
    }

    public int getStat4() {
      return stat4;
    }

    public int getGroupId() {
      return groupId;
    }

    public String getGroupTitle() {
      return groupTitle;
    }

    public String getTitle() {
      return title;
    }

    public Timestamp getLastmod() {
      return lastmod;
    }

    public int getPages() {
      return pages;
    }

    public int getCommentId() {
      return cid;
    }

    public int getLastCommentBy() {
      return lastCommentBy;
    }

    public boolean isResolved(){
      return resolved;
    }

    public String getUrl() {
      if (pages>1) {
        return getGroupUrl()+msgid+"/page"+Integer.toString(pages-1)+"?lastmod="+lastmod.getTime();
      } else {
        return getGroupUrl()+msgid+"?lastmod="+lastmod.getTime();
      }
    }

    public String getUrlReverse() {
      return getGroupUrl()+ '/' +msgid+"?lastmod="+lastmod.getTime();
    }

    public Timestamp getPostdate() {
      return postdate;
    }

    public boolean isUncommited() {
      return uncommited;
    }

    public String getGroupUrl() {
      return Section.getSectionLink(section)+groupUrlName+ '/';
    }
  }
}
