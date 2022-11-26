/*
 * Copyright 1998-2022 Linux.org.ru
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
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.view.RedirectView;
import ru.org.linux.auth.AccessViolationException;
import ru.org.linux.auth.AuthUtil;
import ru.org.linux.site.BadInputException;
import ru.org.linux.site.Template;
import ru.org.linux.tag.TagName;
import ru.org.linux.tag.TagNotFoundException;

import javax.servlet.http.HttpServletRequest;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Controller
public class UserFilterController {
  private final UserService userService;
  private final IgnoreListDao ignoreListDao;
  private final UserTagService userTagService;
  private final RemarkDao remarkDao;

  public UserFilterController(UserService userService, IgnoreListDao ignoreListDao, UserTagService userTagService,
                              RemarkDao remarkDao) {
    this.userService = userService;
    this.ignoreListDao = ignoreListDao;
    this.userTagService = userTagService;
    this.remarkDao = remarkDao;
  }

  @RequestMapping(value = "/user-filter", method = {RequestMethod.GET, RequestMethod.HEAD})
  public ModelAndView showList(
    @RequestParam(value = "newFavoriteTagName", required = false) String newFavoriteTagName,
    @RequestParam(value = "newIgnoreTagName", required = false) String newIgnoreTagName
  ) throws AccessViolationException {
    Template tmpl = Template.getTemplate();

    if (!tmpl.isSessionAuthorized()) {
      throw new AccessViolationException("Not authorized");
    }

    User user = AuthUtil.getCurrentUser();

    ModelAndView modelAndView = new ModelAndView("user-filter-list");

    Map<Integer, User> ignoreMap = createIgnoreMap(ignoreListDao.getJava(user));

    Map<Integer, Remark> ignoreRemarks = remarkDao.getRemarksJava(user, ignoreMap.values());
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
    @RequestParam String nick
  ) {
    Template tmpl = Template.getTemplate();

    if (!tmpl.isSessionAuthorized()) {
      throw new AccessViolationException("Not authorized");
    }

    User user = AuthUtil.getCurrentUser();

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

    Set<Integer> ignoreSet = ignoreListDao.getJava(user);

    if (!ignoreSet.contains(addUser.getId())) {
      ignoreListDao.addUser(user, addUser);
    }

    return new ModelAndView(new RedirectView("/user-filter"));
  }

  @RequestMapping(value = "/user-filter/ignore-user", method = RequestMethod.POST, params = "del")
  public ModelAndView listDel(
    @RequestParam int id
  ) {
    Template tmpl = Template.getTemplate();

    if (!tmpl.isSessionAuthorized()) {
      throw new AccessViolationException("Not authorized");
    }

    User user = AuthUtil.getCurrentUser();

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
    Template tmpl = Template.getTemplate();

    if (!tmpl.isSessionAuthorized()) {
      throw new AccessViolationException("Not authorized");
    }

    User user = AuthUtil.getCurrentUser();

    List<String> r = userTagService.addMultiplyTags(user, tagName, true);

    if (!r.isEmpty()) {
      ModelAndView modelAndView = showList(tagName, null);
      modelAndView.addObject("favoriteTagAddError", r);
      return modelAndView;
    }

    return new ModelAndView(new RedirectView("/user-filter"));
  }

  /**
   * Добавление тега к пользователю.
   *
   * @param tagName название тега
   * @return объект web-модели
   * @throws AccessViolationException нарушение прав доступа
   */
  @RequestMapping(value = "/user-filter/favorite-tag", method = RequestMethod.POST, params = "add", headers = "Accept=application/json")
  @ResponseBody
  public Map<String, Object> favoriteTagAddJSON(
    @RequestParam String tagName
  ) throws AccessViolationException {
    Template tmpl = Template.getTemplate();

    if (!tmpl.isSessionAuthorized()) {
      throw new AccessViolationException("Not authorized");
    }

    User user = AuthUtil.getCurrentUser();

    try {
      int id = userTagService.favoriteAdd(user, tagName);

      return ImmutableMap.of("count", userTagService.countFavs(id));
    } catch (TagNotFoundException e) {
      return ImmutableMap.of("error", e.getMessage());
    }
  }

  /**
   * Удаление тега у пользователя.
   *
   * @param tagName название тега
   * @return объект web-модели
   * @throws TagNotFoundException     тег не найден
   * @throws AccessViolationException нарушение прав доступа
   */
  @RequestMapping(value = "/user-filter/favorite-tag", method = RequestMethod.POST, params = "del")
  public ModelAndView favoriteTagDel(
    @RequestParam String tagName
  ) throws TagNotFoundException, AccessViolationException {
    Template tmpl = Template.getTemplate();

    if (!tmpl.isSessionAuthorized()) {
      throw new AccessViolationException("Not authorized");
    }

    User user = AuthUtil.getCurrentUser();

    userTagService.favoriteDel(user, tagName);

    return new ModelAndView(new RedirectView("/user-filter"));
  }

  /**
   * Удаление тега у пользователя.
   *
   * @param tagName название тега
   * @return объект web-модели
   * @throws TagNotFoundException     тег не найден
   * @throws AccessViolationException нарушение прав доступа
   */
  @ResponseBody
  @RequestMapping(value = "/user-filter/favorite-tag", method = RequestMethod.POST, params = "del", headers = "Accept=application/json")
  public
  Map<String, Object> favoriteTagDelJSON(
    @RequestParam String tagName
  ) throws TagNotFoundException, AccessViolationException {
    Template tmpl = Template.getTemplate();

    if (!tmpl.isSessionAuthorized()) {
      throw new AccessViolationException("Not authorized");
    }

    User user = AuthUtil.getCurrentUser();

    int tagId = userTagService.favoriteDel(user, tagName);

    return ImmutableMap.of("count", userTagService.countFavs(tagId));
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
    Template tmpl = Template.getTemplate();

    if (!tmpl.isSessionAuthorized()) {
      throw new AccessViolationException("Not authorized");
    }
    if (tmpl.isModeratorSession()) {
      throw new AccessViolationException("Модераторам нельзя игнорировать теги");
    }

    User user = AuthUtil.getCurrentUser();

    List<String> errorMessage = userTagService.addMultiplyTags(user, tagName, false);

    if (!errorMessage.isEmpty()) {
      ModelAndView modelAndView = showList(null, tagName);
      modelAndView.addObject("ignoreTagAddError", errorMessage);
      return modelAndView;
    }

    return new ModelAndView(new RedirectView("/user-filter"));
  }

  /**
   * Добавление игнорированного тега к пользователю.
   *
   * @param tagName название тега
   * @return объект web-модели
   * @throws AccessViolationException нарушение прав доступа
   */
  @ResponseBody
  @RequestMapping(value = "/user-filter/ignore-tag", method = RequestMethod.POST, params = "add", headers = "Accept=application/json")
  public
  Map<String, Object> ignoreTagAddJSON(
    @RequestParam String tagName
  ) throws AccessViolationException {
    Template tmpl = Template.getTemplate();

    if (!tmpl.isSessionAuthorized()) {
      throw new AccessViolationException("Not authorized");
    }
    if (tmpl.isModeratorSession()) {
      throw new AccessViolationException("Модераторам нельзя игнорировать теги");
    }

    User user = AuthUtil.getCurrentUser();

    List<String> errorMessage = userTagService.addMultiplyTags(user, tagName, false);
    if (!errorMessage.isEmpty()) {
      return ImmutableMap.of("error", Joiner.on("; ").join(errorMessage));
    }
    
    try {
      int tagId = userTagService.ignoreAdd(user, tagName);

      return ImmutableMap.of("count", userTagService.countIgnore(tagId));
    } catch (TagNotFoundException e) {
      return ImmutableMap.of("error", e.getMessage());
    }
    
  }

  /**
   * Удаление игнорированного тега у пользователя.
   *
   * @param tagName название тега
   * @return объект web-модели
   * @throws TagNotFoundException     тег не найден
   * @throws AccessViolationException нарушение прав доступа
   */
  @RequestMapping(value = "/user-filter/ignore-tag", method = RequestMethod.POST, params = "del")
  public ModelAndView ignoreTagDel(
    @RequestParam String tagName
  ) throws TagNotFoundException, AccessViolationException {
    Template tmpl = Template.getTemplate();

    if (!tmpl.isSessionAuthorized()) {
      throw new AccessViolationException("Not authorized");
    }

    if (tmpl.isModeratorSession()) {
      throw new AccessViolationException("Модераторам нельзя игнорировать теги");
    }

    User user = AuthUtil.getCurrentUser();

    userTagService.ignoreDel(user, tagName);

    return new ModelAndView(new RedirectView("/user-filter"));
  }

  /**
   * Удаление игнорированного тега у пользователя.
   *
   * @param tagName название тега
   * @return объект web-модели
   * @throws TagNotFoundException     тег не найден
   * @throws AccessViolationException нарушение прав доступа
   */
  @ResponseBody
  @RequestMapping(value = "/user-filter/ignore-tag", method = RequestMethod.POST, params = "del", headers = "Accept=application/json")
  public
  Map<String, Object> ignoreTagDelJSON(
    @RequestParam String tagName
  ) throws TagNotFoundException, AccessViolationException {
    Template tmpl = Template.getTemplate();

    if (!tmpl.isSessionAuthorized()) {
      throw new AccessViolationException("Not authorized");
    }

    if (tmpl.isModeratorSession()) {
      throw new AccessViolationException("Модераторам нельзя игнорировать теги");
    }

    User user = AuthUtil.getCurrentUser();

    int tagId = userTagService.ignoreDel(user, tagName);

    return ImmutableMap.of("count", userTagService.countIgnore(tagId));
  }
}
