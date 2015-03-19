/*
 * Copyright 1998-2015 Linux.org.ru
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

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import ru.org.linux.auth.AccessViolationException;
import ru.org.linux.site.Template;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

@Controller
public class UserEventApiController {
  @Autowired
  private UserEventService userEventService;

  @ResponseBody
  @RequestMapping(value = "/notifications-count", method= RequestMethod.GET)
  public int getEventsCount(HttpServletRequest request, HttpServletResponse response) throws Exception {
    Template tmpl = Template.getTemplate(request);
    if (!tmpl.isSessionAuthorized()) {
      throw new AccessViolationException("not authorized");
    }

    response.setHeader("Cache-control", "no-cache");

    return tmpl.getCurrentUser().getUnreadEvents();
  }

  @RequestMapping(value="/notifications-reset", method = RequestMethod.POST)
  @ResponseBody
  public String resetNotifications(
    HttpServletRequest request,
    @RequestParam int topId
  ) throws Exception {
    Template tmpl = Template.getTemplate(request);
    if (!tmpl.isSessionAuthorized()) {
      throw new AccessViolationException("not authorized");
    }

    User currentUser = tmpl.getCurrentUser();

    userEventService.resetUnreadReplies(currentUser, topId);

    return "ok";
  }
}
