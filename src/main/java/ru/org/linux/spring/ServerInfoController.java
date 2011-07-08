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
import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.ModelAndView;

import ru.org.linux.site.LorDataSource;
import ru.org.linux.site.User;

@Controller
public class ServerInfoController {
  @RequestMapping("/server.jsp")
  public ModelAndView serverInfo() throws Exception {
    Connection db = null;

    try {
      db = LorDataSource.getConnection();

      List<User> moderators = new ArrayList<User>();

      Statement st = db.createStatement();
      ResultSet rs = st.executeQuery("SELECT id FROM users WHERE canmod ORDER BY id");

      while (rs.next()) {
        moderators.add(User.getUserCached(db, rs.getInt("id")));
      }

      rs.close();

      ModelAndView mv = new ModelAndView("server");
      mv.getModel().put("moderators", moderators);

      rs = st.executeQuery("SELECT id FROM users WHERE corrector ORDER BY id");

      List<User> correctors = new ArrayList<User>();

      while (rs.next()) {
        correctors.add(User.getUserCached(db, rs.getInt("id")));
      }

      rs.close();

      mv.getModel().put("correctors", correctors);

      return mv;
    } finally {
      if (db!=null) {
        db.close();
      }
    }
  }
}
