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
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.ModelAndView;
import ru.org.linux.site.*;
import ru.org.linux.spring.dao.IgnoreListDao;
import ru.org.linux.spring.dao.UserDao;

import javax.servlet.ServletRequest;
import javax.servlet.http.HttpServletRequest;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

@Controller
public class IgnoreListController {
  @Autowired
  private UserDao userDao;

  @Autowired
  private IgnoreListDao ignoreListDao;

  @RequestMapping(value="/ignore-list.jsp", method={RequestMethod.GET, RequestMethod.HEAD})
  public ModelAndView showList(HttpServletRequest request) throws Exception {
    Template tmpl = Template.getTemplate(request);

    if (!tmpl.isSessionAuthorized()) {
      throw new AccessViolationException("Not authorized");
    }

    User user = tmpl.getCurrentUser();
    user.checkAnonymous();

    Map<Integer, User> ignoreMap = createIgnoreMap(ignoreListDao.get(user));

    return new ModelAndView("ignore-list", "ignoreList", ignoreMap);
  }

  private Map<Integer,User> createIgnoreMap(Set<Integer> ignoreList) throws UserNotFoundException {
    Map<Integer, User> ignoreMap = new HashMap<Integer, User>(ignoreList.size());

    for (int id : ignoreList) {
      ignoreMap.put(id, userDao.getUserCached(id));
    }

    return ignoreMap;
  }

  @RequestMapping(value="/ignore-list.jsp", method= RequestMethod.POST, params = "add")
  public ModelAndView listAdd(
    HttpServletRequest request,
    @RequestParam String nick
  ) throws Exception {
    Template tmpl = Template.getTemplate(request);

    if (!tmpl.isSessionAuthorized()) {
      throw new AccessViolationException("Not authorized");
    }

    User user = tmpl.getCurrentUser();
    user.checkAnonymous();

    User addUser = userDao.getUser(nick);

    // Add nick to ignore list
    if (nick.equals(user.getNick())) {
      throw new BadInputException("нельзя игнорировать самого себя");
    }

    Set<Integer> ignoreSet = ignoreListDao.get(user);

    if (!ignoreSet.contains(addUser.getId())) {
      ignoreListDao.addUser(user, addUser);
    }

    Map<Integer, User> ignoreMap = createIgnoreMap(ignoreListDao.get(user));
    return new ModelAndView("ignore-list", "ignoreList", ignoreMap);
  }

  @RequestMapping(value="/ignore-list.jsp", method= RequestMethod.POST, params = "del")
  public ModelAndView listDel(
    ServletRequest request,
    @RequestParam int id
  ) throws Exception {
    Template tmpl = Template.getTemplate(request);

    if (!tmpl.isSessionAuthorized()) {
      throw new AccessViolationException("Not authorized");
    }

    User user = tmpl.getCurrentUser();
    user.checkAnonymous();

    User delUser = userDao.getUser(id);

    ignoreListDao.remove(user, delUser);

    Map<Integer, User> ignoreMap = createIgnoreMap(ignoreListDao.get(user));
    return new ModelAndView("ignore-list", "ignoreList", ignoreMap);
  }
}
