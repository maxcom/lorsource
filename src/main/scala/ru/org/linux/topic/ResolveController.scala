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
package ru.org.linux.topic

import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.servlet.view.RedirectView
import ru.org.linux.auth.AccessViolationException
import ru.org.linux.auth.AuthUtil.AuthorizedOnly
import ru.org.linux.group.GroupDao

@Controller
class ResolveController(messageDao: TopicDao, groupDao: GroupDao) {
  @RequestMapping(Array("/resolve.jsp"))
  def resolve(@RequestParam("msgid") msgid: Int,
              @RequestParam("resolve") resolved: String): RedirectView = AuthorizedOnly { currentUser =>
    val message = messageDao.getById(msgid)
    val group = groupDao.getGroup(message.groupId)

    if (!group.isResolvable) {
      throw new AccessViolationException("В данной группе нельзя помечать темы как решенные")
    }

    if (!currentUser.moderator && currentUser.user.getId != message.authorUserId) {
      throw new AccessViolationException("У Вас нет прав на решение данной темы")
    }

    messageDao.resolveMessage(message.id, "yes" == resolved)

    new RedirectView(TopicLinkBuilder.baseLink(message).forceLastmod.build)
  }
}