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
import javax.servlet.http.HttpSession;
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

  @RequestMapping(value="/usermod.jsp", method= RequestMethod.POST)
  public ModelAndView modifyUser(
    HttpServletRequest request,
    HttpSession session,
    @RequestParam("action") String action,
    @RequestParam("id") int id,
    @RequestParam(value="reason", required = false) String reason
  ) throws Exception {
    Template tmpl = Template.getTemplate(request);

    if (!tmpl.isModeratorSession()) {
      throw new AccessViolationException("Not moderator");
    }

    User user = userDao.getUser(id);
    User moderator = userDao.getUser(tmpl.getNick());

    if ("block".equals(action) || "block-n-delete-comments".equals(action)) {
      if (!user.isBlockable() && !moderator.isAdministrator()) {
        throw new AccessViolationException("Пользователя " + user.getNick() + " нельзя заблокировать");
      }

      userDao.block(user, moderator, reason);
      userDao.resetPassword(user);

      logger.info("User " + user.getNick() + " blocked by " + moderator.getNick());

      if ("block-n-delete-comments".equals(action)) {
        Map<String, Object> params = new HashMap<String, Object>();
        params.put("message", "Удалено");
        List<Integer> deleted = commentDao.deleteAllComments(user, moderator);
        params.put("bigMessage", deleted);

        searchQueueSender.updateComment(deleted);
        return new ModelAndView("action-done", params);
      }
    } else if ("toggle_corrector".equals(action)) {
      if (user.getScore()<User.CORRECTOR_SCORE) {
        throw new AccessViolationException("Пользователя " + user.getNick() + " нельзя сделать корректором");
      }
      userDao.toggleCorrector(user);
    } else if ("unblock".equals(action)) {
      if (!user.isBlockable() && !moderator.isAdministrator()) {
        throw new AccessViolationException("Пользователя " + user.getNick() + " нельзя разблокировать");
      }
      userDao.unblock(user);
      logger.info("User " + user.getNick() + " unblocked by " + moderator.getNick());
    } else if ("remove_userinfo".equals(action)) {
      if (user.canModerate()) {
        throw new AccessViolationException("Пользователю " + user.getNick() + " нельзя удалить сведения");
      }
      userDao.setUserInfo(user, null);
      userDao.changeScore(user, -10);
      logger.info("Clearing " + user.getNick() + " userinfo");
    } else {
      throw new UserErrorException("Invalid action=" + HTMLFormatter.htmlSpecialChars(action));
    }


    Random random = new Random();

    return new ModelAndView(new RedirectView("/people/" + URLEncoder.encode(user.getNick()) + "/profile?nocache=" + random.nextInt()));
  }

  @RequestMapping(value="/remove-userpic.jsp", method= RequestMethod.POST)
  public ModelAndView removeUserpic(
    ServletRequest request,
    @RequestParam("id") int id
  ) throws Exception {
    Template tmpl = Template.getTemplate(request);

    if (!tmpl.isSessionAuthorized()) {
      throw new AccessViolationException("Not autorized");
    }

    User user = userDao.getUser(id);
    User currentUser = userDao.getUser(tmpl.getNick());

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
}
