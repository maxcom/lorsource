/*
 * Copyright 1998-2023 Linux.org.ru
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
package ru.org.linux.reaction

import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.{PathVariable, RequestMapping, RequestParam}
import org.springframework.web.servlet.ModelAndView
import ru.org.linux.auth.{AccessViolationException, AuthUtil}
import ru.org.linux.reaction.UserReactionsController.{ItemsPerPage, MaxOffset}
import ru.org.linux.site.BadParameterException
import ru.org.linux.user.UserService
import scala.jdk.CollectionConverters.SeqHasAsJava

@Controller
@RequestMapping(path = Array("/people/{nick}/reactions","/people/{nick}/reactions/{mode}"))
class UserReactionsController(reactionService: ReactionService, userService: UserService) {
  @RequestMapping
  def reactions(@PathVariable nick: String,
                @PathVariable(required = false) mode: String,
                @RequestParam(required = false, defaultValue = "0") offset: Int): ModelAndView = AuthUtil.AuthorizedOnly { currentUser =>

    val user = userService.getUserCached(nick)

    if (offset > MaxOffset)
      throw new BadParameterException("offset", "too big")

    if (!currentUser.moderator && currentUser.user != user)
      throw new AccessViolationException("можно смотреть только свои реакции")

    val modelAndView = new ModelAndView("user-reactions")
    // меняется по логике ниже
    var url = s"/people/${user.getNick}/reactions"

    modelAndView.addObject("title", "Реакции")

    val title = if (user == currentUser.user)
      "мои реакции"
    else
      s"реакции ${user.getNick}"

    val titleTo = if (user == currentUser.user)
      "на мои сообщения"
    else
      s"реакции на ${user.getNick}"

    modelAndView.addObject("reactionsTitle", titleTo)
    modelAndView.addObject("meTitle", title)
    modelAndView.addObject("url", url)
    modelAndView.addObject("reactionsUrl", s"/people/${user.getNick}/reactions/to")

    val showDeleted = currentUser.moderator

    val items =if (mode != null) {
      if ("to".equalsIgnoreCase(mode)) {
        // признак включения режима "реакции на меня"
        modelAndView.addObject("modeTo", 1)
        // переделка url для пагинации
        url = s"/people/${user.getNick}/reactions/to"
        reactionService.getReactionsView(user, offset, ItemsPerPage + 1, modeTo = true,showDeleted)
      } else
        throw new BadParameterException("mode", "incorrect")

    } else {
      // вариант по-умолчанию (мои реакции)
      reactionService.getReactionsView(user, offset, ItemsPerPage + 1, modeTo = false,showDeleted)
    }

    modelAndView.addObject("items", items.take(ItemsPerPage).asJava)

    val nextUrl = if (items.sizeIs == ItemsPerPage + 1 && items.sizeIs < MaxOffset - ItemsPerPage)
      s"${url}?offset=${offset + ItemsPerPage}"
    else
      ""

    val prevUrl = if (offset == ItemsPerPage)
      url
    else if (offset > ItemsPerPage)
      s"${url}?offset=${offset - ItemsPerPage}"
    else
      ""

    modelAndView.addObject("nextUrl", nextUrl)
    modelAndView.addObject("prevUrl", prevUrl)
    modelAndView.addObject("user", user)

    modelAndView
  }
}

object UserReactionsController {
  val ItemsPerPage = 50
  val MaxOffset = 10000
}