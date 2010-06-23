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
import java.sql.ResultSet;
import java.sql.Timestamp;
import java.text.DateFormat;
import java.util.ArrayList;
import java.util.List;

import javax.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.ModelAndView;

import ru.org.linux.site.DateFormats;
import ru.org.linux.site.LorDataSource;
import ru.org.linux.site.User;
import ru.org.linux.site.UserErrorException;
import ru.org.linux.util.ServletParameterException;
import ru.org.linux.util.StringUtil;

@Controller
public class ShowCommentsController {
  @RequestMapping("/show-comments.jsp")
  public ModelAndView showComments(
    @RequestParam String nick,
    @RequestParam(defaultValue="0") int offset,
    HttpServletResponse response
  ) throws Exception {
    ModelAndView mv = new ModelAndView("show-comments");

    int topics = 50;
    mv.getModel().put("topics", topics);

    if (offset<0) {
      throw new ServletParameterException("offset<0!?");
    }

    mv.getModel().put("offset", offset);

    boolean firstPage = offset==0;

    if (firstPage) {
      response.setDateHeader("Expires", System.currentTimeMillis() + 90 * 1000);
    } else {
      response.setDateHeader("Expires", System.currentTimeMillis() + 60 * 60 * 1000L);
    }

    mv.getModel().put("firstPage", firstPage);

    Connection db = null;

    try {
      db = LorDataSource.getConnection();

      User user = User.getUser(db, nick);

      mv.getModel().put("user", user);

      if (user.isAnonymous()) {
        throw new UserErrorException("Функция только для зарегистрированных пользователей");
      }

      DateFormat dateFormat = DateFormats.createDefault();

      List<CommentsListItem> out = new ArrayList<CommentsListItem>(topics);

      PreparedStatement pst=null;

      try {
        pst = db.prepareStatement(
          "SELECT sections.name as ptitle, groups.title as gtitle, topics.title, " +
            "topics.id as topicid, comments.id as msgid, comments.postdate " +
            "FROM sections, groups, topics, comments " +
            "WHERE sections.id=groups.section AND groups.id=topics.groupid " +
            "AND comments.topic=topics.id " +
            "AND comments.userid=? AND NOT comments.deleted ORDER BY postdate DESC LIMIT " + topics + " OFFSET " + offset
        );

        pst.setInt(1, user.getId());
        ResultSet rs = pst.executeQuery();

        while (rs.next()) {
          CommentsListItem item = new CommentsListItem();

          item.setSectionTitle(rs.getString("ptitle"));
          item.setGroupTitle(rs.getString("gtitle"));
          item.setTopicId(rs.getInt("topicid"));
          item.setCommentId(rs.getInt("msgid"));
          item.setTitle(StringUtil.makeTitle(rs.getString("title")));
          item.setPostdate(rs.getTimestamp("postdate"));

          out.add(item);
        }

        rs.close();
      } finally {
        if (pst != null) {
          pst.close();
        }
      }

      mv.getModel().put("list", out);

      return mv;
    } finally {
      if (db!=null) {
        db.close();
      }
    }
  }

  public class CommentsListItem {
    private String sectionTitle;
    private String groupTitle;
    private int topicId;
    private int commentId;
    private String title;
    private Timestamp postdate;

    public String getSectionTitle() {
      return sectionTitle;
    }

    public void setSectionTitle(String sectionTitle) {
      this.sectionTitle = sectionTitle;
    }

    public String getGroupTitle() {
      return groupTitle;
    }

    public void setGroupTitle(String groupTitle) {
      this.groupTitle = groupTitle;
    }

    public int getTopicId() {
      return topicId;
    }

    public void setTopicId(int topicId) {
      this.topicId = topicId;
    }

    public int getCommentId() {
      return commentId;
    }

    public void setCommentId(int commentId) {
      this.commentId = commentId;
    }

    public String getTitle() {
      return title;
    }

    public void setTitle(String title) {
      this.title = title;
    }

    public Timestamp getPostdate() {
      return postdate;
    }

    public void setPostdate(Timestamp postdate) {
      this.postdate = postdate;
    }
  }
}
