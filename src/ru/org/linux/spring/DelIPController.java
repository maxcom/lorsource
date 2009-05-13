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

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Timestamp;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.servlet.ModelAndView;

import ru.org.linux.site.*;
import ru.org.linux.util.ServletParameterParser;

@Controller
public class DelIPController {
  @RequestMapping(value="/delip.jsp", method= RequestMethod.POST)
  public ModelAndView delIp(HttpServletRequest request) throws Exception {
    Map<String, Object> params = new HashMap<String, Object>();
    Map<Integer, String> deleted = new HashMap<Integer, String>();
    
    Template tmpl = Template.getTemplate(request);
    HttpSession session = request.getSession();
    
    if (!tmpl.isModeratorSession()) {
      throw new AccessViolationException("Not moderator");
    }

    Connection db = null;
    try {
      db = LorDataSource.getConnection();

      String reason = new ServletParameterParser(request).getString("reason");
      String ip = new ServletParameterParser(request).getString("ip");
      String time = new ServletParameterParser(request).getString("time");

      Calendar calendar = Calendar.getInstance();
      calendar.setTime(new Date());

      if ("hour".equals(time)) {
        calendar.add(Calendar.HOUR_OF_DAY, -1);
      } else if ("day".equals(time)) {
        calendar.add(Calendar.DAY_OF_MONTH, -1);
      } else if ("3day".equals(time)) {
        calendar.add(Calendar.DAY_OF_MONTH, -3);
      } else {
        throw new UserErrorException("Invalid count");
      }

      Timestamp ts = new Timestamp(calendar.getTimeInMillis());
      params.put("message", "Удаляем темы и сообщения после "+ts.toString()+" с IP "+ip+"<br>");

      db.setAutoCommit(false);
    
      User moderator = User.getUser(db, (String) session.getValue("nick"));
    
      PreparedStatement st = null;
      ResultSet rs = null;
      CommentDeleter deleter = null;
      
      try {
        // Delete IP topics
        PreparedStatement lock = db.prepareStatement("SELECT id FROM topics WHERE postip=?::inet AND not deleted AND postdate>? FOR UPDATE");
        PreparedStatement st1 = db.prepareStatement("UPDATE topics SET deleted='t',sticky='f' WHERE id=?");
        PreparedStatement st2 = db.prepareStatement("INSERT INTO del_info (msgid, delby, reason) values(?,?,?)");
        lock.setString(1, ip);
        lock.setTimestamp(2, ts);
        st2.setInt(2, moderator.getId());
        st2.setString(3, reason);
       
        int topicCounter = 0;
        ResultSet lockResult = lock.executeQuery(); // lock another delete on this row
        while (lockResult.next()) {
          int mid = lockResult.getInt("id");
          st1.setInt(1,mid);
          st2.setInt(1,mid);
          st1.executeUpdate();
          st2.executeUpdate();
          topicCounter++;
        }
        st1.close();
        st2.close();
        lockResult.close();
        lock.close(); 
        params.put("topics", topicCounter);
    
        // Delete user comments
        deleter = new CommentDeleter(db);
    
        st = db.prepareStatement("SELECT id FROM comments WHERE postip=?::inet AND not deleted AND postdate>? ORDER BY id DESC FOR update");
        st.setString(1,ip);
        st.setTimestamp(2, ts);

        rs = st.executeQuery();
    
        while (rs.next()) {
          int msgid = rs.getInt("id");

          if (!deleter.getReplys(msgid).isEmpty()) {
            deleted.put(msgid, "пропущен");
            continue;
          }

          deleted.put(msgid, deleter.deleteComment(msgid, reason, moderator, -20));
        }

        params.put("deleted", deleted);        
      } finally {
        if (deleter!=null) {
          deleter.close();
        }
      
        if (rs!=null) {
          rs.close();
        }
      
        if (st!=null) {
          st.close();
        }
      }
      
      db.commit();
      
      return new ModelAndView("delip", params);
    } finally {
      if (db!=null) {
        db.close();
      }
    }
  }
}
