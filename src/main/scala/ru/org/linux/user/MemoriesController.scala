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

import io.circe.Json
import io.circe.syntax.*
import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.{RequestMapping, RequestMethod, RequestParam, ResponseBody}
import ru.org.linux.auth.AccessViolationException
import ru.org.linux.auth.AuthUtil.AuthorizedOnly
import ru.org.linux.topic.TopicDao

@Controller
class MemoriesController(messageDao: TopicDao, memoriesDao: MemoriesDao) {
  @ResponseBody
  @RequestMapping(value = Array("/memories.jsp"), params = Array("add"), method = Array(RequestMethod.POST))
  def add(@RequestParam("msgid") msgid: Int,
          @RequestParam("watch") watch: Boolean): Json = AuthorizedOnly { currentUser =>
    val topic = messageDao.getById(msgid)

    if (topic.deleted) {
      throw new UserErrorException("Тема удалена")
    }

    val id = memoriesDao.addToMemories(currentUser.user, topic, watch)
    val memoriesInfo = memoriesDao.getTopicInfo(msgid, Some(currentUser.user))

    val count = if (watch) {
      memoriesInfo.watchCount
    } else {
      memoriesInfo.favsCount
    }

    Map("id" -> Integer.valueOf(id), "count" -> Integer.valueOf(count)).asJson
  }

  @ResponseBody
  @RequestMapping(value = Array("/memories.jsp"), params = Array("remove"), method = Array(RequestMethod.POST))
  def remove(@RequestParam("id") id: Int): Json = AuthorizedOnly { currentUser =>
    memoriesDao.getMemoriesListItem(id).map { m =>
      if (m.getUserid != currentUser.user.getId) {
        throw new AccessViolationException("Нельзя удалить чужую запись")
      }

      memoriesDao.delete(id)

      val memoriesInfo = memoriesDao.getTopicInfo(m.getTopic, Some(currentUser.user))

      if (m.isWatch) {
        memoriesInfo.watchCount
      } else {
        memoriesInfo.favsCount
      }
    }.orElse(-1).asJson
  }
}