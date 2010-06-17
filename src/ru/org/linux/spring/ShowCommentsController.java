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
import java.text.DateFormat;

import javax.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.ModelAndView;

import ru.org.linux.site.*;
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

      StringBuilder out = new StringBuilder();

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
          out.append("<tr><td>").append(rs.getString("ptitle")).append("</td>");
          out.append("<td>").append(rs.getString("gtitle")).append("</td>");
          out.append("<td><a href=\"jump-message.jsp?msgid=").append(rs.getInt("topicid")).append("&amp;cid=").append(rs.getInt("msgid")).append("\" rev=contents>").append(StringUtil.makeTitle(rs.getString("title"))).append("</a></td>");
          out.append("<td>").append(dateFormat.format(rs.getTimestamp("postdate"))).append("</td></tr>");
        }

        rs.close();
      } finally {
        if (pst != null) {
          pst.close();
        }
      }

      mv.getModel().put("list", out.toString());

      return mv;
    } finally {
      if (db!=null) {
        db.close();
      }
    }
  }
}
