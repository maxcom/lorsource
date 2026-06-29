/*
 * Copyright 1998-2026 Linux.org.ru
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
package ru.org.linux.comment

import org.springframework.http.HttpStatus
import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.*
import org.springframework.web.servlet.ModelAndView
import org.springframework.web.servlet.view.RedirectView
import ru.org.linux.auth.AuthUtil.ModeratorOnly
import ru.org.linux.user.{UserErrorException, UserNotFoundException, UserService}

import scala.jdk.CollectionConverters.SeqHasAsJava

@Controller
class ShowCommentsController(userService: UserService, commentService: CommentReadService):
  @ModelAttribute("filters")
  def getFilters: java.util.List[DeletedCommentsFilterEnum] = DeletedCommentsFilterEnum.values.toSeq.asJava

  @RequestMapping(Array("/show-comments.jsp"))
  def showComments(
      @RequestParam
      nick: String): RedirectView =
    val user = userService.getUserCached(nick)
    new RedirectView(s"search.jsp?range=COMMENTS&user=${user.nick}&sort=DATE")

  private def makeTitle(nick: String, filter: DeletedCommentsFilterEnum): String =
    val base = "Удаленные комментарии " + nick
    if filter == DeletedCommentsFilterEnum.ALL then
      base
    else
      base + " (" + filter.label + ")"

  private def buildUrl(nick: String, offset: Int, filter: DeletedCommentsFilterEnum): String =
    val additionalQuery =
      if filter != DeletedCommentsFilterEnum.ALL then
        Some("filter=" + filter.value)
      else
        None

    if offset > 0 then
      s"/people/$nick/deleted-comments?offset=$offset${additionalQuery.map("&amp;" + _).getOrElse("")}"
    else
      s"/people/$nick/deleted-comments${additionalQuery.map("?" + _).getOrElse("")}"

  private val PageSize = 50
  private val MaxOffset = 300

  @RequestMapping(value = Array("/people/{nick}/deleted-comments"))
  def showDeletedComments(
      @PathVariable
      nick: String,
      @RequestParam(value = "filter", required = false, defaultValue = "all")
      filterAction: String,
      @RequestParam(value = "offset", required = false, defaultValue = "0")
      offset: Int): ModelAndView =
    ModeratorOnly { _ =>
      if offset < 0 || offset > MaxOffset then
        throw new UserErrorException("Некорректное значение offset")

      val user = userService.getUserCached(nick)
      val filter = DeletedCommentsFilterEnum.getByValue(filterAction).getOrElse(DeletedCommentsFilterEnum.ALL)

      val mv = new ModelAndView("deleted-comments")

      mv.getModel.put("user", user)
      mv.getModel.put("filter", filter.value)
      mv.getModel.put("title", makeTitle(nick, filter))
      val deleted = commentService.getDeletedComments(user, filter, offset)
      mv.getModel.put("deletedList", deleted.asJava)

      if offset < MaxOffset && deleted.size == PageSize then
        mv.getModel.put("nextLink", buildUrl(nick, offset + PageSize, filter))

      if offset >= PageSize then
        mv.getModel.put("prevLink", buildUrl(nick, offset - PageSize, filter))

      mv
    }

  @ExceptionHandler(Array(classOf[UserNotFoundException])) @ResponseStatus(HttpStatus.NOT_FOUND)
  def handleUserNotFound: ModelAndView =
    val mav = new ModelAndView("errors/good-penguin")

    mav.addObject("msgTitle", "Ошибка: пользователя не существует")
    mav.addObject("msgHeader", "Пользователя не существует")
    mav.addObject("msgMessage", "")

    mav
