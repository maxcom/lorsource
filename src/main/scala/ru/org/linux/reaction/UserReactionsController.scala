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
import org.springframework.web.bind.annotation.{PathVariable, RequestMapping, RequestParam, ResponseBody}
import ru.org.linux.auth.{AccessViolationException, AuthUtil}
import ru.org.linux.site.BadParameterException
import ru.org.linux.user.UserService

@Controller
@RequestMapping(Array("/people/{nick}/reactions"))
class UserReactionsController(reactionService: ReactionService, userService: UserService) {
  @RequestMapping
  @ResponseBody // TODO replace with view
  def reactions(@PathVariable nick: String,
                @RequestParam(required = false, defaultValue = "0") offset: Int) = AuthUtil.ModeratorOnly { currentUser => // TODO AuthorizedOnly
    val user = userService.getUserCached(nick)

    if (offset > 10000) {
      throw new BadParameterException("offset", "too big")
    }

    if (!currentUser.moderator && currentUser.user != user) {
      throw new AccessViolationException("можно смотреть только свои реакции")
    }

    reactionService.getReactionsView(user, offset, 100)
  }
}
