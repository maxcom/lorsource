/*
 * Copyright 1998-2014 Linux.org.ru
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
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.view.RedirectView;
import ru.org.linux.site.Template;
import ru.org.linux.user.User;
import ru.org.linux.user.UserDao;
import ru.org.linux.user.UserErrorException;
import ru.org.linux.user.UserNotFoundException;
import ru.org.linux.util.ServletParameterException;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.List;

@Controller
public class ShowCommentsController {
  @Autowired
  private UserDao userDao;

  @Autowired
  private CommentService commentService;

  @RequestMapping("/show-comments.jsp")
  public RedirectView showComments(
          @RequestParam String nick
  ) throws Exception {
    User user = userDao.getUser(nick);

    return new RedirectView("search.jsp?range=COMMENTS&user="+user.getNick()+"&sort=DATE");
  }

  @RequestMapping("/show-comments-old.jsp")
  public ModelAndView showCommentsOld(
    @RequestParam String nick,
    @RequestParam(defaultValue="0") int offset,
    HttpServletRequest request,
    HttpServletResponse response
  ) throws Exception {
    Template tmpl = Template.getTemplate(request);

    ModelAndView mv = new ModelAndView("show-comments");

    int topics = 50;
    mv.getModel().put("topics", topics);

    if (offset<0) {
      throw new ServletParameterException("offset<0!?");
    }

    if (offset>1000) {
      throw new ServletParameterException("Доступно не более 1000 комментариев");
    }

    mv.getModel().put("offset", offset);

    boolean firstPage = offset==0;

    if (firstPage) {
      response.setDateHeader("Expires", System.currentTimeMillis() + 90 * 1000);
    } else {
      response.setDateHeader("Expires", System.currentTimeMillis() + 60 * 60 * 1000L);
    }

    mv.getModel().put("firstPage", firstPage);

    User user = userDao.getUser(nick);

    mv.getModel().put("user", user);

    if (user.isAnonymous()) {
      throw new UserErrorException("Функция только для зарегистрированных пользователей");
    }

    List<CommentDao.CommentsListItem> out = commentService.getUserComments(user, topics, offset);

    mv.getModel().put("list", out);

    if (tmpl.isModeratorSession()) {
      mv.getModel().put("deletedList", commentService.getDeletedComments(user));
    }

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
