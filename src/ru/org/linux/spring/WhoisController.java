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
import java.util.Collections;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.springframework.web.servlet.mvc.AbstractController;
import org.springframework.web.servlet.ModelAndView;

import ru.org.linux.util.ServletParameterParser;
import ru.org.linux.site.User;
import ru.org.linux.site.LorDataSource;

public class WhoisController extends AbstractController {
  @Override
  protected ModelAndView handleRequestInternal(HttpServletRequest request, HttpServletResponse response) throws Exception {
    String nick = new ServletParameterParser(request).getString("nick");

    Connection db = null;
    try {
      db = LorDataSource.getConnection();
      
      User user = User.getUser(db, nick);

      return new ModelAndView("whois", Collections.singletonMap("user", user));
    } finally {
      if (db!=null) {
        db.close();
      }
    }
  }
}
