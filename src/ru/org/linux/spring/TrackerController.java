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

import java.sql.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;import javax.servlet.ServletRequest;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.ModelAndView;

import ru.org.linux.site.*;

@Controller
public class TrackerController {
  @RequestMapping("/tracker.jsp")
  public ModelAndView tracker(
    @RequestParam(value="h", required = false) Integer hourObject,
    @RequestParam(value="filter", required = false) String filter,
    @RequestParam(value="new", required = false) String testRequest,
    ServletRequest request) throws Exception {
    int hour = hourObject!=null?hourObject:3;

    if (hour < 1 || hour > 23) {
      throw new BadInputException("неправильный ввод. hours = " + hour);
    }

    boolean noTalks = filter!=null && filter.equals("notalks");
    boolean mine = filter!=null && filter.equals("mine");

    Map<String, Object> params = new HashMap<String, Object>();

    params.put("hour", hour);
    params.put("notalks", noTalks);
    params.put("mine", mine);

    Template tmpl = Template.getTemplate(request);
    int messages = tmpl.getProf().getInt("messages");

    Connection db = null;

    try {
      db = LorDataSource.getConnection();

      User user = null;
      if (mine) {
        if (!tmpl.isSessionAuthorized()) {
          throw new UserErrorException("Not authorized");
        }

        user = User.getUser(db, tmpl.getNick());
      }

      String sSql;
      if (testRequest!=null) {
        sSql = "SELECT " +
          "t.userid as author, t.id, lastmod, t.stat1 AS stat1, t.stat3 AS stat3, t.stat4 AS stat4, g.id AS gid, g.title AS gtitle, t.title AS title, comments.id as cid, comments.userid AS last_comment_by " +
          "FROM topics AS t, groups AS g, comments " +
          "WHERE not t.deleted AND t.id=comments.topic AND t.groupid=g.id AND comments.id=(SELECT id FROM comments WHERE NOT deleted AND comments.topic=t.id ORDER BY postdate DESC LIMIT 1) AND t.postdate > CURRENT_TIMESTAMP - interval '"+hour+" hours' " +
          "UNION ALL SELECT t.userid as author, t.id, lastmod,  t.stat1 AS stat1, t.stat3 AS stat3, t.stat4 AS stat4, g.id AS gid, g.title AS gtitle, t.title AS title, 0, 0 FROM topics AS t, groups AS g WHERE not t.deleted AND t.postdate > CURRENT_TIMESTAMP - interval '"+hour+" hours' AND t.stat1=0 " +
          "ORDER BY lastmod DESC LIMIT 100;";
      } else {
        sSql = "SELECT " +
        "t.userid as author, t.id, lastmod, t.stat1 AS stat1, t.stat3 AS stat3, t.stat4 AS stat4, g.id AS gid, g.title AS gtitle, t.title AS title, cid, comments.userid AS last_comment_by " +
        "FROM topics AS t, groups AS g, (SELECT topic, max(id) as cid FROM comments WHERE NOT deleted AND postdate > CURRENT_TIMESTAMP - interval '" + hour + " hours' GROUP BY topic " +
        "UNION ALL SELECT id, 0 as cid FROM topics WHERE postdate > CURRENT_TIMESTAMP - interval '" + hour + " hours' AND stat1=0) AS foo LEFT JOIN comments ON foo.cid=comments.id " +
        "WHERE not t.deleted AND t.id=foo.topic AND t.groupid=g.id "
        + (noTalks?" AND not t.groupid=8404":"")
        + (mine?" AND t.userid="+user.getId():"")+
        "ORDER BY lastmod DESC";
      }

      Statement st = db.createStatement();
      ResultSet rs = st.executeQuery(sSql);

      List<Item> msgs = new ArrayList<Item>();

      while (rs.next()) {
        msgs.add(new Item(rs, messages));
      }

      params.put("msgs", msgs);
    } finally {
      if (db!=null) {
        db.close();
      }
    }

    return new ModelAndView("tracker", params);
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
  }
}
