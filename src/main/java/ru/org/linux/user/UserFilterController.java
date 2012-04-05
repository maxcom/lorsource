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

import com.google.common.collect.ImmutableMap;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.view.RedirectView;
import ru.org.linux.auth.AccessViolationException;
import ru.org.linux.site.BadInputException;
import ru.org.linux.site.Template;
import ru.org.linux.tag.TagNotFoundException;
import ru.org.linux.tag.TagService;

import javax.servlet.ServletRequest;
import javax.servlet.http.HttpServletRequest;
import java.util.HashMap;
import java.util.List;
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
  public ModelAndView showList(
    HttpServletRequest request,
    @RequestParam(value = "newFavoriteTagName", required = false) String newFavoriteTagName,
    @RequestParam(value = "newIgnoredTagName", required = false) String newIgnoredTagName
  ) throws AccessViolationException {
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

    if (newFavoriteTagName != null && TagService.isGoodTag(newFavoriteTagName)) {
      modelAndView.addObject("newFavoriteTagName", newFavoriteTagName);
    }

    if (newIgnoredTagName != null && TagService.isGoodTag(newIgnoredTagName)) {
      modelAndView.addObject("newIgnoredTagName", newIgnoredTagName);
    }

    return modelAndView;
  }

  private Map<Integer, User> createIgnoreMap(Set<Integer> ignoreList) {
    Map<Integer, User> ignoreMap = new HashMap<Integer, User>(ignoreList.size());

    for (int id : ignoreList) {
      try {
        ignoreMap.put(id, userDao.getUserCached(id));
      } catch (UserNotFoundException e) {
        throw new RuntimeException(e);
      }
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
   */
  @RequestMapping(value = "/user-filter/favorite-tag", method = RequestMethod.POST, params = "add")
  public ModelAndView favoriteTagAddHTML(
    HttpServletRequest request,
    @RequestParam String tagName
  ) throws AccessViolationException {
    List<String> r = favoriteTagAdd(request, tagName);

    if (r.size() != 0) {
      ModelAndView modelAndView = showList(request, tagName, null);
      modelAndView.addObject("favoriteTagAddError", r);
      return modelAndView;
    }

    return new ModelAndView(new RedirectView("/user-filter"));
  }

  /**
   * Добавление тега к пользователю.
   *
   * @param request данные запроса от web-клиента
   * @param tagName название тега
   * @return объект web-модели
   * @throws AccessViolationException нарушение прав доступа
   */
  @RequestMapping(value = "/user-filter/favorite-tag", method = RequestMethod.POST, params = "add", headers = "Accept=application/json")
  @ResponseBody
  public Map<String, String> favoriteTagAddJSON(
    HttpServletRequest request,
    @RequestParam String tagName
  ) throws AccessViolationException {
    List<String> r = favoriteTagAdd(request, tagName);
    if (r.size() != 0) {
      return ImmutableMap.of("error", StringUtils.join(r, "; "));
    }

    return ImmutableMap.of();
  }

  /**
   * @return null if ok, error otherwise
   */
  private List<String> favoriteTagAdd(
    HttpServletRequest request,
    @RequestParam String tagName
  ) throws AccessViolationException {
    Template tmpl = Template.getTemplate(request);

    if (!tmpl.isSessionAuthorized()) {
      throw new AccessViolationException("Not authorized");
    }

    User user = tmpl.getCurrentUser();
    user.checkAnonymous();

    return userTagService.addMultiplyTags(user, tagName, true);
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
   * Удаление тега у пользователя.
   *
   * @param request данные запроса от web-клиента
   * @param tagName название тега
   * @return объект web-модели
   * @throws TagNotFoundException     тег не найден
   * @throws AccessViolationException нарушение прав доступа
   */
  @RequestMapping(value = "/user-filter/favorite-tag", method = RequestMethod.POST, params = "del", headers = "Accept=application/json")
  public
  @ResponseBody
  Map<String, String> favoriteTagDelJSON(
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

    return ImmutableMap.of();
  }

  /**
   * Добавление игнорированного тега к пользователю.
   *
   * @param request данные запроса от web-клиента
   * @param tagName название тега
   * @return объект web-модели
   * @throws AccessViolationException нарушение прав доступа
   */
  @RequestMapping(value = "/user-filter/ignore-tag", method = RequestMethod.POST, params = "add")
  public ModelAndView ignoreTagAdd(
    HttpServletRequest request,
    @RequestParam String tagName
  ) throws AccessViolationException {
    Template tmpl = Template.getTemplate(request);

    if (!tmpl.isSessionAuthorized()) {
      throw new AccessViolationException("Not authorized");
    }
    if (tmpl.isModeratorSession()) {
      throw new AccessViolationException("Модераторам нельзя игнорировать теги");
    }

    User user = tmpl.getCurrentUser();
    user.checkAnonymous();

    List<String> errorMessage = userTagService.addMultiplyTags(user, tagName, false);

    if (errorMessage.size() != 0) {
      ModelAndView modelAndView = showList(request, null, tagName);
      modelAndView.addObject("ignoreTagAddError", errorMessage);
      return modelAndView;
    }

    return new ModelAndView(new RedirectView("/user-filter"));
  }

  /**
   * Добавление игнорированного тега к пользователю.
   *
   * @param request данные запроса от web-клиента
   * @param tagName название тега
   * @return объект web-модели
   * @throws AccessViolationException нарушение прав доступа
   */
  @RequestMapping(value = "/user-filter/ignore-tag", method = RequestMethod.POST, params = "add", headers = "Accept=application/json")
  public
  @ResponseBody
  Map<String, String> ignoreTagAddJSON(
    HttpServletRequest request,
    @RequestParam String tagName
  ) throws AccessViolationException {
    Template tmpl = Template.getTemplate(request);

    if (!tmpl.isSessionAuthorized()) {
      throw new AccessViolationException("Not authorized");
    }
    if (tmpl.isModeratorSession()) {
      throw new AccessViolationException("Модераторам нельзя игнорировать теги");
    }

    User user = tmpl.getCurrentUser();
    user.checkAnonymous();

    List<String> errorMessage = userTagService.addMultiplyTags(user, tagName, false);
    if (errorMessage.size() != 0) {
      return ImmutableMap.of("error", StringUtils.join(errorMessage,"; "));
    }

    return ImmutableMap.of();
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

  /**
   * Удаление игнорированного тега у пользователя.
   *
   * @param request данные запроса от web-клиента
   * @param tagName название тега
   * @return объект web-модели
   * @throws TagNotFoundException     тег не найден
   * @throws AccessViolationException нарушение прав доступа
   */
  @RequestMapping(value = "/user-filter/ignore-tag", method = RequestMethod.POST, params = "del", headers = "Accept=application/json")
  public
  @ResponseBody
  Map<String, String> ignoreTagDelJSON(
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

    return ImmutableMap.of();
  }
}
