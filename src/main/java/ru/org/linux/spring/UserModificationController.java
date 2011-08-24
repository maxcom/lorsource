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
import org.springframework.beans.factory.annotation.Required;
import org.springframework.context.support.ApplicationObjectSupport;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.InitBinder;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.view.RedirectView;
import ru.org.linux.site.AccessViolationException;
import ru.org.linux.site.Template;
import ru.org.linux.site.User;
import ru.org.linux.site.UserErrorException;
import ru.org.linux.spring.dao.CommentDao;
import ru.org.linux.spring.dao.UserDao;
import ru.org.linux.util.HTMLFormatter;

import javax.servlet.ServletRequest;
import javax.servlet.http.HttpServletRequest;
import java.net.URLEncoder;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

@Controller
public class UserModificationController extends ApplicationObjectSupport {
  private SearchQueueSender searchQueueSender;
  private UserDao userDao;
  private CommentDao commentDao;

  @Autowired
  @Required
  public void setSearchQueueSender(SearchQueueSender searchQueueSender) {
    this.searchQueueSender = searchQueueSender;
  }

  @Autowired
  public void setCommentDao(CommentDao commentDao){
    this.commentDao = commentDao;
  }

  @Autowired
  public void setUserDao(UserDao userDao) {
    this.userDao = userDao;
  }

  /**
   * Возвращает объект User модератора, если текущая сессия не модераторская, тогда исключение
   * @param request текущий http запрос
   * @return текущий модератор
   * @throws Exception если модератора нет
   */
  private User getModerator(HttpServletRequest request) throws Exception {
    Template tmpl = Template.getTemplate(request);
    if (!tmpl.isModeratorSession()) {
      throw new AccessViolationException("Not moderator");
    }
    return tmpl.getCurrentUser();
  }

  /**
   * Контроллер блокировки пользователя
   * @param request http запрос
   * @param action всегда block
   * @param user блокируемый пользователь
   * @param reason причина блокировки
   * @return возвращаемся в профиль
   * @throws Exception обычно если текущий пользователь не модератор или блокируемого пользователя
   * нельзя блокировать
   */
  @RequestMapping(value = "/usermod.jsp", method = RequestMethod.POST, params = "action=block")
  public ModelAndView blockUser(
      HttpServletRequest request,
      @RequestParam("action") String action,
      @RequestParam("id") User user,
      @RequestParam(value = "reason", required = false) String reason
  ) throws Exception {

    User moderator = getModerator(request);
    if (!user.isBlockable() && !moderator.isAdministrator()) {
      throw new AccessViolationException("Пользователя " + user.getNick() + " нельзя заблокировать");
    }

    userDao.blockWithResetPassword(user, moderator, reason);
    logger.info("User " + user.getNick() + " blocked by " + moderator.getNick());
    Random random = new Random();
    return new ModelAndView(new RedirectView("/people/" + URLEncoder.encode(user.getNick()) + "/profile?nocache=" + random.nextInt()));
  }

  /**
   * Контроллер разблокировки пользователя
   * @param request http запрос
   * @param action всегда unblock
   * @param user разблокируемый пользователь
   * @return возвращаемся в профиль
   * @throws Exception обычно если текущий пользователь не модератор или пользователя нельзя разблокировать
   */
  @RequestMapping(value = "/usermod.jsp", method = RequestMethod.POST, params = "action=unblock")
  public ModelAndView unblockUser(
      HttpServletRequest request,
      @RequestParam("action") String action,
      @RequestParam("id") User user
  ) throws Exception {

    User moderator = getModerator(request);
    if (!user.isBlockable() && !moderator.isAdministrator()) {
      throw new AccessViolationException("Пользователя " + user.getNick() + " нельзя разблокировать");
    }
    userDao.unblock(user);
    logger.info("User " + user.getNick() + " unblocked by " + moderator.getNick());
    Random random = new Random();
    return new ModelAndView(new RedirectView("/people/" + URLEncoder.encode(user.getNick()) + "/profile?nocache=" + random.nextInt()));
  }

  /**
   * Контроллер блокирования и полного удаления комментариев и топиков пользователя
   * @param request http запрос
   * @param action всегда block-n-delete-comments
   * @param user блокируемый пользователь
   * @return возвращаемся в профиль
   * @throws Exception обычно если текущий пользователь не модератор или пользователя нельзя блокировать
   */
  @RequestMapping(value = "/usermod.jsp", method = RequestMethod.POST, params = "action=block-n-delete-comments")
  public ModelAndView blockAndMassiveDeleteCommentUser(
      HttpServletRequest request,
      @RequestParam("action") String action,
      @RequestParam("id") User user,
      @RequestParam(value = "reason", required = false) String reason
  ) throws Exception {

    User moderator = getModerator(request);
    if (!user.isBlockable() && !moderator.isAdministrator()) {
      throw new AccessViolationException("Пользователя " + user.getNick() + " нельзя заблокировать");
    }
    Map<String, Object> params = new HashMap<String, Object>();
    params.put("message", "Удалено");
    List<Integer> deleted = commentDao.deleteAllCommentsAndBlock(user, moderator, reason);
    logger.info("User " + user.getNick() + " blocked by " + moderator.getNick());
    params.put("bigMessage", deleted);
    searchQueueSender.updateComment(deleted);
    return new ModelAndView("action-done", params);
  }

  /**
   * Контроллер смена признака корректора
   * @param request http запрос
   * @param action всегда toggle_corrector
   * @param user блокируемый пользователь
   * @return возвращаемся в профиль
   * @throws Exception обычно если текущий пользователь не модератор или пользователя нельзя сделать корректором
   */
  @RequestMapping(value = "/usermod.jsp", method = RequestMethod.POST, params = "action=toggle_corrector")
  public ModelAndView toggleUserCorrector(
      HttpServletRequest request,
      @RequestParam("action") String action,
      @RequestParam("id") User user,
      @RequestParam(value = "reason", required = false) String reason
  ) throws Exception {

    User moderator = getModerator(request);
    if (user.getScore()<User.CORRECTOR_SCORE) {
      throw new AccessViolationException("Пользователя " + user.getNick() + " нельзя сделать корректором");
    }
    userDao.toggleCorrector(user);

    Random random = new Random();
    return new ModelAndView(new RedirectView("/people/" + URLEncoder.encode(user.getNick()) + "/profile?nocache=" + random.nextInt()));
  }

  /**
   * Контроллер отчистки дополнительной информации в профиле
   * @param request http запрос
   * @param action всегда remove_userinfo
   * @param user блокируемый пользователь
   * @return возвращаемся в профиль
   * @throws Exception обычно если текущий пользователь не модератор или нельзя трогать дополнительные сведения
   */
  @RequestMapping(value = "/usermod.jsp", method = RequestMethod.POST, params = "action=remove_userinfo")
  public ModelAndView removeUserInfo(
      HttpServletRequest request,
      @RequestParam("action") String action,
      @RequestParam("id") User user,
      @RequestParam(value = "reason", required = false) String reason
  ) throws Exception {

    User moderator = getModerator(request);
    if (user.canModerate()) {
      throw new AccessViolationException("Пользователю " + user.getNick() + " нельзя удалить сведения");
    }
    userDao.removeUserInfo(user);
    logger.info("Clearing " + user.getNick() + " userinfo");

    Random random = new Random();
    return new ModelAndView(new RedirectView("/people/" + URLEncoder.encode(user.getNick()) + "/profile?nocache=" + random.nextInt()));
  }

  @RequestMapping(value="/remove-userpic.jsp", method= RequestMethod.POST)
  public ModelAndView removeUserpic(
    ServletRequest request,
    @RequestParam("id") User user
  ) throws Exception {
    Template tmpl = Template.getTemplate(request);

    if (!tmpl.isSessionAuthorized()) {
      throw new AccessViolationException("Not autorized");
    }

    User currentUser = tmpl.getCurrentUser();

    if (!currentUser.canModerate() && currentUser.getId()!=user.getId()) {
      throw new AccessViolationException("Not permitted");
    }

    if (user.canModerate()) {
      throw new AccessViolationException("Пользователю " + user.getNick() + " нельзя удалить картинку");
    }

    if (user.getPhoto() == null) {
      throw new AccessViolationException("Пользователь " + user.getNick() + " картинки не имеет");
    }

    userDao.removePhoto(user, currentUser);
    logger.info("Clearing " + user.getNick() + " userpic by " + currentUser.getNick());

    Random random = new Random();

    return new ModelAndView(new RedirectView("/people/" + URLEncoder.encode(user.getNick()) + "/profile?nocache=" + random.nextInt()));
  }

  @InitBinder
  public void initBinder(WebDataBinder binder) {
    binder.registerCustomEditor(User.class, new UserIdPropertyEditor(userDao));
  }
}
