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

import com.google.common.collect.ImmutableMap;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import ru.org.linux.auth.AccessViolationException;
import ru.org.linux.auth.AuthUtil;
import ru.org.linux.site.Template;
import ru.org.linux.topic.Topic;
import ru.org.linux.topic.TopicDao;

import java.util.Map;

@Controller
public class MemoriesController {
  @Autowired
  private TopicDao messageDao;

  @Autowired
  private MemoriesDao memoriesDao;

  @ResponseBody
  @RequestMapping(value = "/memories.jsp", params = {"add"}, method = RequestMethod.POST)
  public Map<String, Integer> add(
          @RequestParam("msgid") int msgid,
          @RequestParam("watch") boolean watch
  ) {
    Template tmpl = Template.getTemplate();

    if (!tmpl.isSessionAuthorized()) {
      throw new AccessViolationException("Not authorized");
    }

    User user = AuthUtil.getCurrentUser();
    user.checkBlocked();

    Topic topic = messageDao.getById(msgid);
    if (topic.isDeleted()) {
      throw new UserErrorException("Тема удалена");
    }

    int id = memoriesDao.addToMemories(user, topic, watch);

    MemoriesInfo memoriesInfo = memoriesDao.getTopicInfo(msgid, user);

    int count = watch? memoriesInfo.watchCount(): memoriesInfo.favsCount();

    return ImmutableMap.of("id", id, "count", count);
  }

  @ResponseBody
  @RequestMapping(value = "/memories.jsp", params = {"remove"}, method = RequestMethod.POST)
  public int remove(
          @RequestParam("id") int id
  ) {
    Template tmpl = Template.getTemplate();

    if (!tmpl.isSessionAuthorized()) {
      throw new AccessViolationException("Not authorized");
    }

    User user = AuthUtil.getCurrentUser();
    user.checkBlocked();

    return memoriesDao.getMemoriesListItem(id).map (m -> {
      if (m.getUserid() != user.getId()) {
        throw new AccessViolationException("Нельзя удалить чужую запись");
      }

      memoriesDao.delete(id);

      MemoriesInfo memoriesInfo = memoriesDao.getTopicInfo(m.getTopic(), user);

      return m.isWatch()? memoriesInfo.watchCount(): memoriesInfo.favsCount();
    }).orElse(-1);
  }
}
