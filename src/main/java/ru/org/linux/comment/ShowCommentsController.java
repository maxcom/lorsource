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

package ru.org.linux.comment;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.view.RedirectView;
import ru.org.linux.auth.AccessViolationException;
import ru.org.linux.site.Template;
import ru.org.linux.user.User;
import ru.org.linux.user.UserNotFoundException;
import ru.org.linux.user.UserService;

import javax.servlet.http.HttpServletRequest;

@Controller
public class ShowCommentsController {
  @Autowired
  private UserService userService;

  @Autowired
  private CommentService commentService;

  @RequestMapping("/show-comments.jsp")
  public RedirectView showComments(
          @RequestParam String nick
  ) throws Exception {
    User user = userService.getUserCached(nick);

    return new RedirectView("search.jsp?range=COMMENTS&user="+user.getNick()+"&sort=DATE");
  }

  @RequestMapping(value="/people/{nick}/deleted-comments")
  public ModelAndView showCommentsOld(
    @PathVariable String nick,
    HttpServletRequest request
  ) throws Exception {
    Template tmpl = Template.getTemplate(request);
    if (!tmpl.isModeratorSession()) {
      throw new AccessViolationException("Not moderator");
    }

    ModelAndView mv = new ModelAndView("deleted-comments");

    User user = userService.getUserCached(nick);

    mv.getModel().put("user", user);

    mv.getModel().put("deletedList", commentService.getDeletedComments(user));

    return mv;
  }

  @ExceptionHandler(UserNotFoundException.class)
  @ResponseStatus(HttpStatus.NOT_FOUND)
  public ModelAndView handleUserNotFound() {
    ModelAndView mav = new ModelAndView("errors/good-penguin");
    mav.addObject("msgTitle", "Ошибка: пользователя не существует");
    mav.addObject("msgHeader", "Пользователя не существует");
    mav.addObject("msgMessage", "");
    return mav;
  }
}
