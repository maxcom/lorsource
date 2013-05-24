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

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.ModelAndView;
import ru.org.linux.site.Template;
import ru.org.linux.user.User;
import ru.org.linux.user.UserDao;
import ru.org.linux.user.UserErrorException;
import ru.org.linux.user.UserInfo;

import javax.servlet.http.HttpServletRequest;
import javax.sql.DataSource;
import java.util.Date;
import java.util.List;

@Controller
public class DevconfController {
  @Autowired
  private UserDao userDao;

  private JdbcTemplate jdbcTemplate;

  @Autowired
  public void setDataSource(DataSource ds) {
    jdbcTemplate = new JdbcTemplate(ds);
  }

  @RequestMapping(value = "/devconf2013", method = RequestMethod.POST)
  public ModelAndView add(HttpServletRequest request, @RequestParam("msg") String msg) throws Exception {
    Template tmpl = Template.getTemplate(request);

    if (!tmpl.isSessionAuthorized()) {
      throw new UserErrorException("Not authorized");
    }

    User user = tmpl.getCurrentUser();

    UserInfo info = userDao.getUserInfoClass(user);

    if (!"devconf2013".equals(msg)) {
      throw new UserErrorException("Неправильный код, прочитайте текст новости");
    }

    List<Integer> found = jdbcTemplate.queryForList("SELECT * FROM devconf2011 WHERE userid=?", Integer.class, user.getId());

    if (found.isEmpty()) {
      jdbcTemplate.update("INSERT INTO devconf2011 VALUES(?)", user.getId());
    }

    return new ModelAndView("action-done", "message", "OK");
  }
}
