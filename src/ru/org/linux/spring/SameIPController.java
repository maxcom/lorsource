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

import javax.servlet.http.HttpServletRequest;

import org.springframework.jdbc.support.JdbcUtils;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.ModelAndView;

import ru.org.linux.site.*;
import ru.org.linux.util.ServletParameterParser;

@Controller
public class SameIPController {
  @RequestMapping("/sameip.jsp")
  public ModelAndView sameIP(HttpServletRequest request) throws Exception {
    Template tmpl = Template.getTemplate(request);

    if (!tmpl.isModeratorSession()) {
      throw new AccessViolationException("Not moderator");
    }

    Connection db = null;

    try {
      db = LorDataSource.getConnection();

      int userAgentId = 0;
      String ip;
      if (request.getParameter("msgid") != null) {
        Statement ipst = db.createStatement();
        int msgid = new ServletParameterParser(request).getInt("msgid");

        ResultSet rs = ipst.executeQuery("SELECT postip, ua_id FROM topics WHERE id=" + msgid);

        if (!rs.next()) {
          rs.close();
          rs = ipst.executeQuery("SELECT postip, ua_id FROM comments WHERE id=" + msgid);
          if (!rs.next()) {
            throw new MessageNotFoundException(msgid);
          }
        }

        ip = rs.getString("postip");
        userAgentId = rs.getInt("ua_id");

        if (ip == null) {
          throw new ScriptErrorException("No IP data for #" + msgid);
        }

        rs.close();
        ipst.close();
      } else {
        ip = new ServletParameterParser(request).getIP("ip");
      }


      ModelAndView mv = new ModelAndView("sameip");

      mv.getModel().put("ip", ip);
      mv.getModel().put("uaId", userAgentId);

      mv.getModel().put("blockInfo", IPBlockInfo.getBlockInfo(db, ip));

      return mv;
    } finally {
      JdbcUtils.closeConnection(db);
    }
  }
}
