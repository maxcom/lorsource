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
import org.springframework.web.bind.annotation.{PathVariable, RequestMapping, RequestParam}
import org.springframework.web.servlet.ModelAndView
import ru.org.linux.auth.{AccessViolationException, AuthUtil}

import scala.jdk.CollectionConverters.SeqHasAsJava

@Controller
class ShowRemarkController(remarkDao: RemarkDao, prepareService: PreparedRemarkService) {
  @RequestMapping(Array("/people/{nick}/remarks"))
  def showRemarks(@PathVariable nick: String, @RequestParam(value = "offset", defaultValue = "0") offset: Int,
                  @RequestParam(value = "sort", defaultValue = "0") sortorder: Int): ModelAndView = AuthUtil.AuthorizedOnly { currentUser =>
    if (currentUser.user.getNick != nick) {
      throw new AccessViolationException("Not authorized")
    }

    val count = remarkDao.remarkCount(currentUser.user)

    val mv = new ModelAndView("view-remarks")

    val limit = currentUser.profile.messages

    if (count > 0) {
      if (offset >= count || offset < 0) {
        throw new UserErrorException("Wrong offset")
      }

      if (sortorder != 0 && sortorder != 1) {
        throw new UserErrorException("Wrong sort")
      }

      if (sortorder != 1) {
        mv.getModel.put("sortorder", "")
      } else {
        mv.getModel.put("sortorder", "&amp;sort=1")
      }

      val remarks = remarkDao.getRemarkList(currentUser.user, offset, sortorder, limit)

      val preparedRemarks = prepareService.prepareRemarks(remarks)

      mv.getModel.put("remarks", preparedRemarks)
    } else {
      mv.getModel.put("remarks", Seq.empty.asJava)
    }

    mv.getModel.put("offset", offset)
    mv.getModel.put("limit", limit)
    mv.getModel.put("hasMore", count > (offset + limit))

    mv
  }
}