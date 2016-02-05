/*
 * Copyright 1998-2015 Linux.org.ru
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

import com.google.common.base.Joiner;
import com.google.common.collect.ImmutableMap;
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
import ru.org.linux.tag.TagName;
import ru.org.linux.tag.TagNotFoundException;

import javax.servlet.ServletRequest;
import javax.servlet.http.HttpServletRequest;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Controller
public class UserFilterController {
  @Autowired
  private UserService userService;

  @Autowired
  private IgnoreListDao ignoreListDao;

  @Autowired
  private UserTagService userTagService;

  @Autowired
  private RemarkDao remarkDao;

  @RequestMapping(value = "/user-filter", method = {RequestMethod.GET, RequestMethod.HEAD})
  public ModelAndView showList(
    HttpServletRequest request,
    @RequestParam(value = "newFavoriteTagName", required = false) String newFavoriteTagName,
    @RequestParam(value = "newIgnoreTagName", required = false) String newIgnoreTagName
  ) throws AccessViolationException {
    Template tmpl = Template.getTemplate(request);

    if (!tmpl.isSessionAuthorized()) {
      throw new AccessViolationException("Not authorized");
    }

    User user = tmpl.getCurrentUser();
    user.checkAnonymous();

    ModelAndView modelAndView = new ModelAndView("user-filter-list");

    Map<Integer, User> ignoreMap = createIgnoreMap(ignoreListDao.get(user));

    Map<Integer, Remark> ignoreRemarks = remarkDao.getRemarks(user, ignoreMap.values());
    modelAndView.addObject("ignoreRemarks", ignoreRemarks);

    modelAndView.addObject("ignoreList", ignoreMap);
    modelAndView.addObject("favoriteTags", userTagService.favoritesGet(user));
    if (!tmpl.isModeratorSession()) {
      modelAndView.addObject("ignoreTags", userTagService.ignoresGet(user));
    } else {
      modelAndView.addObject("isModerator", true);
    }

    if (newFavoriteTagName != null && TagName.isGoodTag(newFavoriteTagName)) {
      modelAndView.addObject("newFavoriteTagName", newFavoriteTagName);
    }

    if (newIgnoreTagName != null && TagName.isGoodTag(newIgnoreTagName)) {
      modelAndView.addObject("newIgnoreTagName", newIgnoreTagName);
    }

    return modelAndView;
  }

  private Map<Integer, User> createIgnoreMap(Set<Integer> ignoreList) {
    Map<Integer, User> ignoreMap = new HashMap<>(ignoreList.size());

    for (int id : ignoreList) {
      ignoreMap.put(id, userService.getUserCached(id));
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

    User addUser;

    try {
      addUser = userService.getUserCached(nick);
    } catch (UserNotFoundException ex) {
      throw new BadInputException("указанный пользователь не существует");
    }

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

    User delUser = userService.getUserCached(id);

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
    Template tmpl = Template.getTemplate(request);

    if (!tmpl.isSessionAuthorized()) {
      throw new AccessViolationException("Not authorized");
    }

    User user = tmpl.getCurrentUser();
    user.checkAnonymous();

    List<String> r = userTagService.addMultiplyTags(user, tagName, true);

    if (!r.isEmpty()) {
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
  public Map<String, Object> favoriteTagAddJSON(
    HttpServletRequest request,
    @RequestParam String tagName
  ) throws AccessViolationException {
    Template tmpl = Template.getTemplate(request);

    if (!tmpl.isSessionAuthorized()) {
      throw new AccessViolationException("Not authorized");
    }

    User user = tmpl.getCurrentUser();
    user.checkAnonymous();

    try {
      int id = userTagService.favoriteAdd(user, tagName);

      return ImmutableMap.<String, Object>of("count", userTagService.countFavs(id));
    } catch (TagNotFoundException e) {
      return ImmutableMap.<String, Object>of("error", e.getMessage());
    }
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
  @ResponseBody
  @RequestMapping(value = "/user-filter/favorite-tag", method = RequestMethod.POST, params = "del", headers = "Accept=application/json")
  public
  Map<String, Object> favoriteTagDelJSON(
    ServletRequest request,
    @RequestParam String tagName
  ) throws TagNotFoundException, AccessViolationException {
    Template tmpl = Template.getTemplate(request);

    if (!tmpl.isSessionAuthorized()) {
      throw new AccessViolationException("Not authorized");
    }

    User user = tmpl.getCurrentUser();
    user.checkAnonymous();

    int tagId = userTagService.favoriteDel(user, tagName);

    return ImmutableMap.<String, Object>of("count", userTagService.countFavs(tagId));
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

    if (!errorMessage.isEmpty()) {
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
  @ResponseBody
  @RequestMapping(value = "/user-filter/ignore-tag", method = RequestMethod.POST, params = "add", headers = "Accept=application/json")
  public
  Map<String, Object> ignoreTagAddJSON(
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
    if (!errorMessage.isEmpty()) {
      return ImmutableMap.of("error", Joiner.on("; ").join(errorMessage));
    }
    
    try {
      int tagId = userTagService.ignoreAdd(user, tagName);

      return ImmutableMap.<String, Object>of("count", userTagService.countIgnore(tagId));
    } catch (TagNotFoundException e) {
      return ImmutableMap.<String, Object>of("error", e.getMessage());
    }
    
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
  @ResponseBody
  @RequestMapping(value = "/user-filter/ignore-tag", method = RequestMethod.POST, params = "del", headers = "Accept=application/json")
  public
  Map<String, Object> ignoreTagDelJSON(
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

    int tagId = userTagService.ignoreDel(user, tagName);

    return ImmutableMap.<String, Object>of("count", userTagService.countIgnore(tagId));
  }
}
