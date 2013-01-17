/*
 * Copyright 1998-2012 Linux.org.ru
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

package ru.org.linux.topic;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.view.RedirectView;
import ru.org.linux.auth.AccessViolationException;
import ru.org.linux.group.Group;
import ru.org.linux.group.GroupDao;
import ru.org.linux.site.Template;
import ru.org.linux.user.User;

import javax.servlet.http.HttpServletRequest;

@Controller
public class ResolveController  {
  @Autowired
  private TopicDao messageDao;

  @Autowired
  private GroupDao groupDao;

  @RequestMapping("/resolve.jsp")
  public RedirectView resolve(
    HttpServletRequest request,
    @RequestParam("msgid") int msgid,
    @RequestParam("resolve") String resolved
  ) throws Exception {
    Template tmpl = Template.getTemplate(request);

    Topic message = messageDao.getById(msgid);
    Group group = groupDao.getGroup(message.getGroupId());
    User currentUser = tmpl.getCurrentUser();
    if (!group.isResolvable()) {
      throw new AccessViolationException("В данной группе нельзя помечать темы как решенные");
    }

    if (!tmpl.isSessionAuthorized()) {
      throw new AccessViolationException("Not authorized");
    }

    if (!tmpl.isModeratorSession() && currentUser.getId() != message.getUid()) {
      throw new AccessViolationException("У Вас нет прав на решение данной темы");
    }
    messageDao.resolveMessage(message.getId(), (resolved != null) && "yes".equals(resolved));

    return new RedirectView(TopicLinkBuilder.baseLink(message).forceLastmod().build());
  }
}
