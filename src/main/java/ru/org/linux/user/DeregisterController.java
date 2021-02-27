/*
 * Copyright 1998-2018 Linux.org.ru
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

import org.springframework.stereotype.Controller;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.InitBinder;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.validation.Errors;
import ru.org.linux.auth.AccessViolationException;
import ru.org.linux.site.Template;
import ru.org.linux.util.ExceptionBindingErrorProcessor;
import ru.org.linux.user.UserDao;
import ru.org.linux.comment.CommentDao;
import ru.org.linux.topic.TopicDao;
import ru.org.linux.search.ElasticsearchIndexService;

import javax.servlet.http.HttpServletRequest;
import javax.validation.Valid;
import java.util.List;

@Controller
public class DeregisterController {
  @Autowired
  private UserDao userDao;

  @Autowired
  private CommentDao commentDao;

  @Autowired
  private TopicDao topicDao;

  @Autowired
  private ElasticsearchIndexService indexService;
  
  private boolean isFullDelete;

  @RequestMapping(value = "/deregister.jsp", method = {RequestMethod.GET, RequestMethod.HEAD})
  public ModelAndView show(
    HttpServletRequest request,
    @ModelAttribute("form") DeregisterRequest form
  ) {
    
    Template tmpl = Template.getTemplate(request);
    isFullDelete = tmpl.getConfig().isUserFullDelete();

    if (!tmpl.isSessionAuthorized()) {
      throw new AccessViolationException("Not authorized");
    }

    User user = tmpl.getCurrentUser();
    user.checkAnonymous();

    String msgDereg1, msgDereg2;
    if(isFullDelete) {
        msgDereg1 = "Удаление"; msgDereg2 = "удалить";
    }else{
        msgDereg1 = "Блокировка"; msgDereg2 = "заблокировать";
    }

    if (user.getScore() < 100) {
      throw new AccessViolationException(msgDereg1 + " аккаунта недоступно для пользователей со score < 100");
    }

    if (user.isAdministrator() || user.isModerator()) {
      throw new AccessViolationException("Нельзя " + msgDereg2 + " модераторский аккаунт");
    }

    return new ModelAndView("deregister");
  }

  @RequestMapping(value = "/deregister.jsp", method = {RequestMethod.POST})
  public ModelAndView deregister(
    HttpServletRequest request,
    @Valid @ModelAttribute("form") DeregisterRequest form,
    Errors errors
  ) {

    Template tmpl = Template.getTemplate(request);
    isFullDelete = tmpl.getConfig().isUserFullDelete();

    if (!tmpl.isSessionAuthorized()) {
      throw new AccessViolationException("Not authorized");
    }

    User user = tmpl.getCurrentUser();
    user.checkAnonymous();

    String msgDereg1, msgDereg2, msgDereg3;
    if(isFullDelete) {
        msgDereg1 = "Удаление"; msgDereg2 = "удалить"; msgDereg3 = "самостоятельное удаление";
    }else{
        msgDereg1 = "Блокировка"; msgDereg2 = "заблокировать"; msgDereg3 = "самостоятельная блокировка";
    }

    if (user.getScore() < 100) {
      throw new AccessViolationException(msgDereg1 + " аккаунта недоступно для пользователей со score < 100");
    }

    if (!user.matchPassword(form.getPassword())) {
      errors.rejectValue("password", null, "Неверный пароль");
    }

    if (user.isAdministrator() || user.isModerator()) {
      throw new AccessViolationException("Нельзя " + msgDereg2 + " модераторский аккаунт");
    }

    if (errors.hasErrors()) {
      return new ModelAndView("deregister");
    }

    // Move messages
    if (isFullDelete) {
        List<Integer> movedComments = commentDao.getAllByUser(user);
        List<Integer> movedTopics = topicDao.getAllByUser(user);

        userDao.moveMessages(user.getId(), userDao.findUserId("Deleted"));

        indexService.reindexComments(movedComments);
        indexService.reindexTopics(movedTopics);
    }

    // Remove user info
    userDao.resetUserpic(user, user);
    userDao.updateUser(user, "", "", null, "", null, "");

    // Block account
    userDao.block(user, user, msgDereg3 + " аккаунта");

    return new ModelAndView(
      "action-done",
      "message",
      msgDereg1 + " пользователя прошло успешно."
    );
  }

  @InitBinder("form")
  public void requestValidator(WebDataBinder binder) {
    binder.setValidator(new DeregisterRequestValidator(isFullDelete));
    binder.setBindingErrorProcessor(new ExceptionBindingErrorProcessor());
  }
}
