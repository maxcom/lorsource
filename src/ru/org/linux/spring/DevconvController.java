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
import java.sql.Statement;
import java.sql.ResultSet;
import java.util.Date;

import javax.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.ModelAndView;

import ru.org.linux.site.*;

@Controller
public class DevconvController {
  @RequestMapping(value="/devconf2011", method = RequestMethod.POST)
  public ModelAndView add(HttpServletRequest request, @RequestParam("msg") String msg) throws Exception {
    Connection db = null;

    Template tmpl = Template.getTemplate(request);

    if (!tmpl.isSessionAuthorized()) {
      throw new UserErrorException("Not authorized");
    }

    try {
      db = LorDataSource.getConnection();

      Statement st = db.createStatement();

      User user = tmpl.getCurrentUser();

      UserInfo info = new UserInfo(db, user.getId());

      if (info.getRegistrationDate()!=null && info.getRegistrationDate().after(new Date(111, 4, 15))) {
        throw new UserErrorException("Дата регистрации после 15/05/2010");
      }

      if (!"devconf2011".equals(msg)) {
        throw new UserErrorException("Неправильный код, прочитайте текст новости");
      }

      ResultSet rs = st.executeQuery("SELECT * FROM devconf2011 WHERE userid="+user.getId());

      if (!rs.next()) {
        st.executeUpdate("INSERT INTO devconf2011 VALUES('"+ user.getId()+ "')");
      }

      return new ModelAndView("action-done", "message", "OK");
    } finally {
      if (db!=null) {
        db.close();
      }
    }
  }
}
