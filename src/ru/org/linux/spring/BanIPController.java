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

import java.net.URLEncoder;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.Timestamp;
import java.util.Calendar;
import java.util.Date;

import javax.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.view.RedirectView;

import ru.org.linux.site.*;
import ru.org.linux.util.ServletParameterParser;

@Controller
public class BanIPController {
  @RequestMapping(value="/banip.jsp", method= RequestMethod.POST)
  public ModelAndView banIP(
    HttpServletRequest request,
    @RequestParam("ip") String ip,
    @RequestParam("reason") String reason,
    @RequestParam("time") String time
  ) throws Exception {
    Template tmpl = Template.getTemplate(request);

    if (!tmpl.isModeratorSession()) {
      throw new IllegalAccessException("Not authorized");
    }

    Calendar calendar = Calendar.getInstance();
    calendar.setTime(new Date());

    if ("hour".equals(time)) {
      calendar.add(Calendar.HOUR_OF_DAY, 1);
    } else if ("day".equals(time)) {
      calendar.add(Calendar.DAY_OF_MONTH, 1);
    } else if ("month".equals(time)) {
      calendar.add(Calendar.MONTH, 1);
    } else if ("3month".equals(time)) {
      calendar.add(Calendar.MONTH, 3);
    } else if ("6month".equals(time)) {
      calendar.add(Calendar.MONTH, 6);
    } else if ("remove".equals(time)) {
    } else if ("custom".equals(time)) {
      int days = new ServletParameterParser(request).getInt("ban_days");

      if (days <= 0 || days > 180) {
        throw new UserErrorException("Invalid days count");
      }

      calendar.add(Calendar.DAY_OF_MONTH, days);
    }

    Timestamp ts;
    if ("unlim".equals(time)) {
      ts = null;
    } else {
      ts = new Timestamp(calendar.getTimeInMillis());
    }

    Connection db = null;
    try {
      db = LorDataSource.getConnection();
      db.setAutoCommit(false);

      User user = User.getUser(db, tmpl.getNick());

      IPBlockInfo blockInfo = IPBlockInfo.getBlockInfo(db, ip);
      user.checkCommit();

      PreparedStatement pst;

      if (blockInfo == null) {
        pst = db.prepareStatement("INSERT INTO b_ips (ip, mod_id, date, reason, ban_date) VALUES (?::inet, ?, CURRENT_TIMESTAMP, ?, ?)");
      } else {
        pst = db.prepareStatement("UPDATE b_ips SET ip=?::inet, mod_id=?,date=CURRENT_TIMESTAMP, reason=?, ban_date=? WHERE ip=?::inet");
        pst.setString(5, ip);
      }

      pst.setString(1, ip);
      pst.setInt(2, user.getId());
      pst.setString(3, reason);
      pst.setTimestamp(4, ts);

      pst.executeUpdate();

      db.commit();

      return new ModelAndView(new RedirectView("sameip.jsp?ip=" + URLEncoder.encode(ip)));
    } finally {
      if (db != null) {
        db.close();
      }
    }
  }
}
