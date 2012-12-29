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
import org.springframework.beans.factory.annotation.Required;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.ModelAndView;
import ru.org.linux.auth.AccessViolationException;
import ru.org.linux.search.SearchQueueSender;
import ru.org.linux.site.BadParameterException;
import ru.org.linux.site.ScriptErrorException;
import ru.org.linux.site.Template;
import ru.org.linux.topic.Topic;
import ru.org.linux.topic.TopicDao;
import ru.org.linux.topic.TopicPermissionService;
import ru.org.linux.user.User;
import ru.org.linux.user.UserErrorException;

import javax.servlet.http.HttpServletRequest;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

@Controller
public class DeleteCommentController {
  private SearchQueueSender searchQueueSender;
  private CommentService commentService;
  private TopicDao messageDao;
  private CommentPrepareService prepareService;

  @Autowired
  private TopicPermissionService permissionService;

  @Autowired
  @Required
  public void setSearchQueueSender(SearchQueueSender searchQueueSender) {
    this.searchQueueSender = searchQueueSender;
  }

  @Autowired
  public void setMessageDao(TopicDao messageDao) {
    this.messageDao = messageDao;
  }

  @Autowired
  public void setCommentService(CommentService commentService) {
    this.commentService = commentService;
  }

  @Autowired
  public void setPrepareService(CommentPrepareService prepareService) {
    this.prepareService = prepareService;
  }

  @RequestMapping(value = "/delete_comment.jsp", method = RequestMethod.GET)
  public ModelAndView showForm(
          HttpServletRequest request,
          @RequestParam("msgid") int msgid
  ) throws Exception {
    Map<String, Object> params = new HashMap<String, Object>();

    Template tmpl = Template.getTemplate(request);

    if (!tmpl.isSessionAuthorized()) {
      throw new AccessViolationException("нет авторизации");
    }

    params.put("msgid", msgid);

    Comment comment = commentService.getById(msgid);

    if (comment.isDeleted()) {
      throw new UserErrorException("комментарий уже удален");
    }

    int topicId = comment.getTopicId();

    Topic topic = messageDao.getById(topicId);

    if (topic.isDeleted()) {
      throw new AccessViolationException("тема удалена");
    }

    params.put("topic", topic);

    CommentList comments = commentService.getCommentList(topic, tmpl.isModeratorSession());

    CommentFilter cv = new CommentFilter(comments);

    List<Comment> list = cv.getCommentsSubtree(msgid);

    params.put("commentsPrepared", prepareService.prepareCommentList(comments, list, request.isSecure(), tmpl, topic));
    params.put("comments", comments);

    return new ModelAndView("delete_comment", params);
  }

  @RequestMapping(value = "/delete_comment.jsp", method = RequestMethod.POST)
  public ModelAndView deleteComments(
          @RequestParam("msgid") int msgid,
          @RequestParam("reason") String reason,
          @RequestParam(value = "bonus", defaultValue = "0") int bonus,
          HttpServletRequest request
  ) throws Exception {
    if (bonus < 0 || bonus > 20) {
      throw new BadParameterException("неправильный размер штрафа");
    }

    Template tmpl = Template.getTemplate(request);

    if (!tmpl.isSessionAuthorized()) {
      throw new AccessViolationException("нет авторизации");
    }

    User user = tmpl.getCurrentUser();
    user.checkBlocked();
    user.checkAnonymous();

    Comment comment = commentService.getById(msgid);

    if (comment.isDeleted()) {
      throw new UserErrorException("комментарий уже удален");
    }

    Topic topic = messageDao.getById(comment.getTopicId());

    final boolean haveAnswers = commentService.isHaveAnswers(comment);

    if (!permissionService.isCommentDeletableNow(comment, user, topic, haveAnswers)) {
      throw new UserErrorException("комментарий нельзя удалить");
    }

    if (!user.isModerator() || comment.getUserid() == user.getId()) {
      bonus = 0;
    }

    StringBuilder out = new StringBuilder();

    List<Integer> deleted = new LinkedList<Integer>();
    deleted.add(msgid);

    if (user.isModerator()) {
      List<Integer> deletedReplys = commentService.deleteReplys(msgid, user, bonus > 2);
      if (!deletedReplys.isEmpty()) {
        out.append("Удаленные ответы: ").append(deletedReplys).append("<br>");
      }

      deleted.addAll(deletedReplys);

      if (commentService.deleteComment(msgid, reason, user, -bonus)) {
        out.append("Сообщение ").append(msgid).append(" удалено");
      }
    } else {
      if (commentService.deleteComment(msgid, reason, user, 0)) {
        out.append("Сообщение ").append(msgid).append(" удалено");
      } else {
        out.append("Сообщение ").append(msgid).append(" уже было удалено");
      }
    }

    searchQueueSender.updateComment(deleted);

    Map<String, Object> params = new HashMap<String, Object>();
    params.put("message", "Удалено успешно");
    params.put("bigMessage", out.toString());

    params.put("link", topic.getLink());

    return new ModelAndView("action-done", params);
  }

  @ExceptionHandler({ScriptErrorException.class, UserErrorException.class, AccessViolationException.class})
  @ResponseStatus(HttpStatus.FORBIDDEN)
  public ModelAndView handleUserNotFound(Exception ex) {
    ModelAndView mav = new ModelAndView("errors/good-penguin");
    mav.addObject("msgTitle", "Ошибка: " + ex.getMessage());
    mav.addObject("msgHeader", ex.getMessage());
    mav.addObject("msgMessage", "");
    return mav;
  }
}
