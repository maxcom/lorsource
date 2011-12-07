/*
 * Copyright 1998-2010 Linux.org.ru
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

package ru.org.linux.spring;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.view.RedirectView;
import ru.org.linux.dao.GroupDao;
import ru.org.linux.dao.MessageDao;
import ru.org.linux.dto.GroupDto;
import ru.org.linux.dto.MessageDto;
import ru.org.linux.dto.UserDto;
import ru.org.linux.exception.AccessViolationException;
import ru.org.linux.site.*;

import javax.servlet.http.HttpServletRequest;

@Controller
public class ResolveController  {
  @Autowired
  private MessageDao messageDao;

  @Autowired
  private GroupDao groupDao;

  @RequestMapping("/resolve.jsp")
  public ModelAndView resolve(
    HttpServletRequest request,
    @RequestParam("msgid") int msgid,
    @RequestParam("resolve") String resolved
  ) throws Exception {
    Template tmpl = Template.getTemplate(request);

    MessageDto messageDto = messageDao.getById(msgid);
    GroupDto groupDto = groupDao.getGroup(messageDto.getGroupId());
    UserDto currentUser = tmpl.getCurrentUser();
    if (!groupDto.isResolvable()) {
      throw new AccessViolationException("В данной группе нельзя помечать темы как решенные");
    }

    if (!tmpl.isSessionAuthorized()) {
      throw new AccessViolationException("Not authorized");
    }

    if (!tmpl.isModeratorSession() && currentUser.getId() != messageDto.getUid()) {
      throw new AccessViolationException("У Вас нет прав на решение данной темы");
    }
    messageDao.resolveMessage(messageDto.getId(), (resolved != null) && "yes".equals(resolved));

    return new ModelAndView(new RedirectView(messageDto.getLinkLastmod()));
  }
}
