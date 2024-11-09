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
package ru.org.linux.group

import com.typesafe.scalalogging.StrictLogging
import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestMethod
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.servlet.ModelAndView
import ru.org.linux.auth.AuthUtil.ModeratorOnly

import scala.jdk.CollectionConverters.MapHasAsJava

@Controller
class GroupModificationController(groupDao: GroupDao, prepareService: GroupInfoPrepareService) extends StrictLogging {
  @RequestMapping(value = Array("/groupmod.jsp"), method = Array(RequestMethod.GET))
  def showForm(@RequestParam("group") id: Int): ModelAndView = ModeratorOnly { _ =>
    val group = groupDao.getGroup(id)

    val mv = new ModelAndView("groupmod", "group", group)

    mv.getModel.put("groupInfo", prepareService.prepareGroupInfo(group))

    mv
  }

  @RequestMapping(value = Array("/groupmod.jsp"), method = Array(RequestMethod.POST))
  def modifyGroup(@RequestParam("group") id: Int, @RequestParam("title") title: String,
                  @RequestParam("info") info: String, @RequestParam("urlName") urlName: String,
                  @RequestParam("longinfo") longInfo: String,
                  @RequestParam(value = "preview", required = false) preview: String,
                  @RequestParam(value = "resolvable", required = false) resolvable: String): ModelAndView = ModeratorOnly { currentUser =>
    var group = groupDao.getGroup(id)

    if (preview != null) {
      group = group.updated(title, info, longInfo)

      return new ModelAndView("groupmod", Map(
        "group" -> group,
        "groupInfo" -> prepareService.prepareGroupInfo(group),
        "preview" -> true
      ).asJava)
    }

    groupDao.setParams(group, title, info, longInfo, resolvable != null, urlName)

    logger.info("Настройки группы {} изменены {}", group.urlName, currentUser.user.getNick)

    new ModelAndView("action-done", "message", "Параметры изменены")
  }
}