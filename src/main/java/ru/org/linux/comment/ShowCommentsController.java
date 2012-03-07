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

package ru.org.linux.comment;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.ModelAndView;
import ru.org.linux.ApplicationController;
import ru.org.linux.comment.CommentDao.CommentsListItem;
import ru.org.linux.site.Template;
import ru.org.linux.user.User;
import ru.org.linux.user.UserDao;
import ru.org.linux.user.UserErrorException;
import ru.org.linux.util.ServletParameterException;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.List;

@Controller
public class ShowCommentsController extends ApplicationController {
  @Autowired
  private UserDao userDao;

  @Autowired
  private CommentDao commentDao;

  @RequestMapping("/show-comments.jsp")
  public ModelAndView showComments(
    @RequestParam String nick,
    @RequestParam(defaultValue="0") int offset,
    HttpServletRequest request,
    HttpServletResponse response
  ) throws Exception {
    Template tmpl = Template.getTemplate(request);

    ModelAndView modelAndView = new ModelAndView("show-comments");

    int topics = 50;
    modelAndView.getModel().put("topics", topics);

    if (offset<0) {
      throw new ServletParameterException("offset<0!?");
    }

    modelAndView.addObject("offset", offset);

    boolean firstPage = offset==0;

    if (firstPage) {
      response.setDateHeader("Expires", System.currentTimeMillis() + 90 * 1000);
    } else {
      response.setDateHeader("Expires", System.currentTimeMillis() + 60 * 60 * 1000L);
    }

    modelAndView.addObject("firstPage", firstPage);

    User user = userDao.getUser(nick);

    modelAndView.addObject("user", user);

    if (user.isAnonymous()) {
      throw new UserErrorException("Функция только для зарегистрированных пользователей");
    }

    List<CommentsListItem> out = commentDao.getUserComments(user, topics, offset);

    modelAndView.addObject("list", out);

    if (tmpl.isModeratorSession()) {
      modelAndView.addObject("deletedList", commentDao.getDeletedComments(user));
    }

    return render(modelAndView);
  }
}
