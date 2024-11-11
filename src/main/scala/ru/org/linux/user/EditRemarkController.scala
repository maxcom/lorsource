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

import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestMethod
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.servlet.ModelAndView
import org.springframework.web.servlet.view.RedirectView
import ru.org.linux.auth.AuthUtil.AuthorizedOnly

@Controller
@RequestMapping(Array("/people/{nick}/remark"))
class EditRemarkController(userService: UserService, remarkDao: RemarkDao) {
  @RequestMapping(method = Array(RequestMethod.GET))
  def showForm(@PathVariable nick: String): ModelAndView = AuthorizedOnly { currentUser =>
    val refUser = userService.getUserCached(nick)

    if (currentUser.user.getNick != nick) {
      val remark = remarkDao.getRemark(currentUser.user, refUser)

      val mv = new ModelAndView("edit-remark")

      if (remark.isDefined) {
        mv.getModel.put("remark", remark.get)
      }

      mv
    } else {
      throw new UserErrorException("Нельзя оставить заметку самому себе")
    }
  }

  @RequestMapping(method = Array(RequestMethod.POST))
  def editProfile(@RequestParam("text") text: String,
                  @PathVariable nick: String): ModelAndView = AuthorizedOnly { currentUser =>
    val refUser = userService.getUserCached(nick)

    remarkDao.setOrUpdateRemark(currentUser.user, refUser, text.take(255))

    new ModelAndView(new RedirectView("/people/" + nick + "/profile"))
  }
}