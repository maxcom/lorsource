/*
 * Copyright 1998-2012 Linux.org.ru
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

package ru.org.linux.user;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.view.RedirectView;
import ru.org.linux.auth.AccessViolationException;
import ru.org.linux.site.BadInputException;
import ru.org.linux.site.Template;
import ru.org.linux.tag.TagNotFoundException;

import javax.servlet.ServletRequest;
import javax.servlet.http.HttpServletRequest;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

@Controller
public class UserFilterController {
  @Autowired
  private UserDao userDao;

  @Autowired
  private IgnoreListDao ignoreListDao;

  @Autowired
  private UserTagService userTagService;

  @RequestMapping(value = "/user-filter", method = {RequestMethod.GET, RequestMethod.HEAD})
  public ModelAndView showList(HttpServletRequest request)
    throws AccessViolationException, UserNotFoundException {
    Template tmpl = Template.getTemplate(request);

    if (!tmpl.isSessionAuthorized()) {
      throw new AccessViolationException("Not authorized");
    }

    User user = tmpl.getCurrentUser();
    user.checkAnonymous();

    ModelAndView modelAndView = new ModelAndView("user-filter-list");

    Map<Integer, User> ignoreMap = createIgnoreMap(ignoreListDao.get(user));
    modelAndView.addObject("ignoreList", ignoreMap);
    modelAndView.addObject("favoriteTags", userTagService.favoritesGet(user));
    if (!tmpl.isModeratorSession()) {
      modelAndView.addObject("ignoreTags", userTagService.ignoresGet(user));
    } else {
      modelAndView.addObject("isModerator", true);
    }
    return modelAndView;
  }

  private Map<Integer, User> createIgnoreMap(Set<Integer> ignoreList) throws UserNotFoundException {
    Map<Integer, User> ignoreMap = new HashMap<Integer, User>(ignoreList.size());

    for (int id : ignoreList) {
      ignoreMap.put(id, userDao.getUserCached(id));
    }

    return ignoreMap;
  }

  @RequestMapping(value = "/user-filter/ignore-user", method = RequestMethod.POST, params = "add")
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

    return new ModelAndView(new RedirectView("/user-filter"));
  }

  @RequestMapping(value = "/user-filter/ignore-user", method = RequestMethod.POST, params = "del")
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

    return new ModelAndView(new RedirectView("/user-filter"));
  }

  /**
   * Добавление тега к пользователю.
   *
   * @param request данные запроса от web-клиента
   * @param tagName название тега
   * @return объект web-модели
   * @throws AccessViolationException нарушение прав доступа
   * @throws UserNotFoundException    пользователь не найден
   */
  @RequestMapping(value = "/user-filter/favorite-tag", method = RequestMethod.POST, params = "add")
  public ModelAndView favoriteTagAdd(
    HttpServletRequest request,
    @RequestParam String tagName
  ) throws AccessViolationException, UserNotFoundException {
    Template tmpl = Template.getTemplate(request);

    if (!tmpl.isSessionAuthorized()) {
      throw new AccessViolationException("Not authorized");
    }

    User user = tmpl.getCurrentUser();
    user.checkAnonymous();
    String errorMessage = null;
    try {
      userTagService.favoriteAdd(user, tagName);
    } catch (TagNotFoundException e) {
      errorMessage = e.getMessage();
    } catch (DuplicateKeyException e) {
      errorMessage = "Тег уже добавлен";
    }
    if (errorMessage != null) {
      ModelAndView modelAndView = showList(request);
      modelAndView.addObject("favoriteTagAddError", tagName + ": " + errorMessage);
      return modelAndView;
    }

    return new ModelAndView(new RedirectView("/user-filter"));
  }

  /**
   * Удаление тега у пользователя.
   *
   * @param request данные запроса от web-клиента
   * @param tagName название тега
   * @return объект web-модели
   * @throws TagNotFoundException     тег не найден
   * @throws AccessViolationException нарушение прав доступа
   */
  @RequestMapping(value = "/user-filter/favorite-tag", method = RequestMethod.POST, params = "del")
  public ModelAndView favoriteTagDel(
    ServletRequest request,
    @RequestParam String tagName
  ) throws TagNotFoundException, AccessViolationException {
    Template tmpl = Template.getTemplate(request);

    if (!tmpl.isSessionAuthorized()) {
      throw new AccessViolationException("Not authorized");
    }

    User user = tmpl.getCurrentUser();
    user.checkAnonymous();

    userTagService.favoriteDel(user, tagName);

    return new ModelAndView(new RedirectView("/user-filter"));
  }

  /**
   * Добавление игнорированного тега к пользователю.
   *
   * @param request данные запроса от web-клиента
   * @param tagName название тега
   * @return объект web-модели
   * @throws AccessViolationException нарушение прав доступа
   * @throws UserNotFoundException    пользователь не найден
   */
  @RequestMapping(value = "/user-filter/ignore-tag", method = RequestMethod.POST, params = "add")
  public ModelAndView ignoreTagAdd(
    HttpServletRequest request,
    @RequestParam String tagName
  ) throws AccessViolationException, UserNotFoundException {
    Template tmpl = Template.getTemplate(request);

    if (!tmpl.isSessionAuthorized()) {
      throw new AccessViolationException("Not authorized");
    }
    if (tmpl.isModeratorSession()) {
      throw new AccessViolationException("Модераторам нельзя игнорировать теги");
    }

    User user = tmpl.getCurrentUser();
    user.checkAnonymous();

    String errorMessage = null;
    try {
      userTagService.ignoreAdd(user, tagName);
    } catch (TagNotFoundException e) {
      errorMessage = e.getMessage();
    } catch (DuplicateKeyException e) {
      errorMessage = "Тег уже добавлен";
    }
    if (errorMessage != null) {
      ModelAndView modelAndView = showList(request);
      modelAndView.addObject("ignoreTagAddError", tagName + ": " + errorMessage);
      return modelAndView;
    }

    return new ModelAndView(new RedirectView("/user-filter"));
  }

  /**
   * Удаление игнорированного тега у пользователя.
   *
   * @param request данные запроса от web-клиента
   * @param tagName название тега
   * @return объект web-модели
   * @throws TagNotFoundException     тег не найден
   * @throws AccessViolationException нарушение прав доступа
   */
  @RequestMapping(value = "/user-filter/ignore-tag", method = RequestMethod.POST, params = "del")
  public ModelAndView ignoreTagDel(
    ServletRequest request,
    @RequestParam String tagName
  ) throws TagNotFoundException, AccessViolationException {
    Template tmpl = Template.getTemplate(request);

    if (!tmpl.isSessionAuthorized()) {
      throw new AccessViolationException("Not authorized");
    }

    if (tmpl.isModeratorSession()) {
      throw new AccessViolationException("Модераторам нельзя игнорировать теги");
    }

    User user = tmpl.getCurrentUser();
    user.checkAnonymous();

    userTagService.ignoreDel(user, tagName);

    return new ModelAndView(new RedirectView("/user-filter"));
  }
}
