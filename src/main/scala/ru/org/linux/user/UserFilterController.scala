/*
 * Copyright 1998-2024 Linux.org.ru
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
package ru.org.linux.user

import com.google.common.base.Joiner
import io.circe.Json
import io.circe.syntax.*
import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.{RequestMapping, RequestMethod, RequestParam, ResponseBody}
import org.springframework.web.servlet.ModelAndView
import org.springframework.web.servlet.view.RedirectView
import ru.org.linux.auth.AccessViolationException
import ru.org.linux.auth.AuthUtil.AuthorizedOnly
import ru.org.linux.site.BadInputException
import ru.org.linux.tag.{TagName, TagNotFoundException}

import scala.jdk.CollectionConverters.MapHasAsJava

@Controller
class UserFilterController(userService: UserService, ignoreListDao: IgnoreListDao, userTagService: UserTagService,
                           remarkDao: RemarkDao) {
  @RequestMapping(value = Array("/user-filter"), method = Array(RequestMethod.GET, RequestMethod.HEAD))
  @throws[AccessViolationException]
  def showList(@RequestParam(value = "newFavoriteTagName", required = false) newFavoriteTagName: String,
               @RequestParam(value = "newIgnoreTagName", required = false) newIgnoreTagName: String): ModelAndView = AuthorizedOnly { currentUser =>
    val modelAndView = new ModelAndView("user-filter-list")

    val ignoreMap = createIgnoreMap(ignoreListDao.get(currentUser.user.getId))
    val ignoreRemarks = remarkDao.getRemarks(currentUser.user, ignoreMap.values)

    modelAndView.addObject("ignoreRemarks", ignoreRemarks.asJava)
    modelAndView.addObject("ignoreList", ignoreMap.asJava)
    modelAndView.addObject("favoriteTags", userTagService.favoritesGet(currentUser.user))

    if (!currentUser.moderator) {
      modelAndView.addObject("ignoreTags", userTagService.ignoresGet(currentUser.user))
    } else {
      modelAndView.addObject("isModerator", true)
    }

    if (newFavoriteTagName != null && TagName.isGoodTag(newFavoriteTagName)) {
      modelAndView.addObject("newFavoriteTagName", newFavoriteTagName)
    }

    if (newIgnoreTagName != null && TagName.isGoodTag(newIgnoreTagName)) {
      modelAndView.addObject("newIgnoreTagName", newIgnoreTagName)
    }

    modelAndView
  }

  private def createIgnoreMap(ignoreList: Set[Int]) =
    ignoreList.view.map(id => id -> userService.getUserCached(id)).toMap

  @RequestMapping(value = Array("/user-filter/ignore-user"), method = Array(RequestMethod.POST), params = Array("add"))
  def listAdd(@RequestParam nick: String): ModelAndView = AuthorizedOnly { currentUser =>
    val addUser = try {
      userService.getUserCached(nick)
    } catch {
      case _: UserNotFoundException =>
        throw new BadInputException("указанный пользователь не существует")
    }

    // Add nick to ignore list
    if (nick == currentUser.user.getNick) {
      throw new BadInputException("нельзя игнорировать самого себя")
    }

    val ignoreSet = ignoreListDao.getJava(currentUser.user)

    if (!ignoreSet.contains(addUser.getId)) {
      ignoreListDao.addUser(currentUser.user, addUser)
    }

    new ModelAndView(new RedirectView("/user-filter"))
  }

  @RequestMapping(value = Array("/user-filter/ignore-user"), method = Array(RequestMethod.POST), params = Array("del"))
  def listDel(@RequestParam id: Int): ModelAndView = AuthorizedOnly { currentUser =>
    val delUser = userService.getUserCached(id)

    ignoreListDao.remove(currentUser.user, delUser)

    new ModelAndView(new RedirectView("/user-filter"))
  }

  /**
   * Добавление тега к пользователю.
   *
   * @param tagName название тега
   * @return объект web-модели
   */
  @RequestMapping(value = Array("/user-filter/favorite-tag"), method = Array(RequestMethod.POST), params = Array("add"))
  def favoriteTagAddHTML(@RequestParam tagName: String): ModelAndView = AuthorizedOnly { currentUser =>
    val r = userTagService.addMultiplyTags(currentUser.user, tagName, isFavorite = true)

    if (!r.isEmpty) {
      val modelAndView = showList(tagName, null)

      modelAndView.addObject("favoriteTagAddError", r)

      modelAndView
    } else {
      new ModelAndView(new RedirectView("/user-filter"))
    }
  }

  /**
   * Добавление тега к пользователю.
   *
   * @param tagName название тега
   * @return объект web-модели
   * @throws AccessViolationException нарушение прав доступа
   */
  @RequestMapping(value = Array("/user-filter/favorite-tag"), method = Array(RequestMethod.POST),
    params = Array("add"), headers = Array("Accept=application/json"))
  @ResponseBody
  def favoriteTagAddJSON(@RequestParam tagName: String): Json = AuthorizedOnly { currentUser =>
    try {
      val id = userTagService.favoriteAdd(currentUser.user, tagName)
      Map("count" -> userTagService.countFavs(id)).asJson
    } catch {
      case e: TagNotFoundException =>
        Map("error" -> e.getMessage).asJson
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
  @RequestMapping(value = Array("/user-filter/favorite-tag"), method = Array(RequestMethod.POST), params = Array("del"))
  def favoriteTagDel(@RequestParam tagName: String): ModelAndView = AuthorizedOnly { currentUser =>
    userTagService.favoriteDel(currentUser.user, tagName)

    new ModelAndView(new RedirectView("/user-filter"))
  }

  /**
   * Удаление тега у пользователя.
   *
   * @param tagName название тега
   * @return объект web-модели
   */
  @ResponseBody
  @RequestMapping(value = Array("/user-filter/favorite-tag"), method = Array(RequestMethod.POST), params = Array("del"),
    headers = Array("Accept=application/json"))
  def favoriteTagDelJSON(@RequestParam tagName: String): Json = AuthorizedOnly { currentUser =>
    val tagId = userTagService.favoriteDel(currentUser.user, tagName)

    Map("count" -> userTagService.countFavs(tagId)).asJson
  }

  /**
   * Добавление игнорированного тега к пользователю.
   *
   * @param tagName название тега
   */
  @RequestMapping(value = Array("/user-filter/ignore-tag"), method = Array(RequestMethod.POST), params = Array("add"))
  def ignoreTagAdd(@RequestParam tagName: String): ModelAndView = AuthorizedOnly { currentUser =>
    if (currentUser.moderator) {
      throw new AccessViolationException("Модераторам нельзя игнорировать теги")
    }

    val errorMessage = userTagService.addMultiplyTags(currentUser.user, tagName, isFavorite = false)

    if (!errorMessage.isEmpty) {
      val modelAndView = showList(null, tagName)

      modelAndView.addObject("ignoreTagAddError", errorMessage)

      modelAndView
    } else {
      new ModelAndView(new RedirectView("/user-filter"))
    }
  }

  /**
   * Добавление игнорированного тега к пользователю.
   *
   * @param tagName название тега
   * @return объект web-модели
   * @throws AccessViolationException нарушение прав доступа
   */
  @ResponseBody
  @RequestMapping(value = Array("/user-filter/ignore-tag"), method = Array(RequestMethod.POST), params = Array("add"),
    headers = Array("Accept=application/json"))
  def ignoreTagAddJSON(@RequestParam tagName: String): Json = AuthorizedOnly { currentUser =>
    if (currentUser.moderator) {
      throw new AccessViolationException("Модераторам нельзя игнорировать теги")
    }

    val errorMessage = userTagService.addMultiplyTags(currentUser.user, tagName, isFavorite = false)

    if (!errorMessage.isEmpty) {
      Map("error" -> Joiner.on("; ").join(errorMessage)).asJson
    } else {
      try {
        val tagId = userTagService.ignoreAdd(currentUser.user, tagName)
        Map("count" -> userTagService.countIgnore(tagId)).asJson
      } catch {
        case e: TagNotFoundException =>
          Map("error" -> e.getMessage).asJson
      }
    }
  }

  /**
   * Удаление игнорированного тега у пользователя.
   *
   * @param tagName название тега
   * @return объект web-модели
   */
  @RequestMapping(value = Array("/user-filter/ignore-tag"), method = Array(RequestMethod.POST), params = Array("del"))
  def ignoreTagDel(@RequestParam tagName: String): ModelAndView = AuthorizedOnly { currentUser =>
    if (currentUser.moderator) {
      throw new AccessViolationException("Модераторам нельзя игнорировать теги")
    }

    userTagService.ignoreDel(currentUser.user, tagName)

    new ModelAndView(new RedirectView("/user-filter"))
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
  @RequestMapping(value = Array("/user-filter/ignore-tag"), method = Array(RequestMethod.POST), params = Array("del"),
    headers = Array("Accept=application/json"))
  def ignoreTagDelJSON(@RequestParam tagName: String): Json = AuthorizedOnly { currentUser =>
    if (currentUser.moderator) {
      throw new AccessViolationException("Модераторам нельзя игнорировать теги")
    }

    val tagId = userTagService.ignoreDel(currentUser.user, tagName)

    Map("count" -> Integer.valueOf(userTagService.countIgnore(tagId))).asJson
  }
}