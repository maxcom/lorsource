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
import org.springframework.stereotype.Controller;
import org.springframework.validation.Errors;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.InitBinder;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.view.RedirectView;
import ru.org.linux.auth.AccessViolationException;
import ru.org.linux.auth.IPBlockDao;
import ru.org.linux.auth.IPBlockInfo;
import ru.org.linux.csrf.CSRFNoAuto;
import ru.org.linux.search.SearchQueueSender;
import ru.org.linux.site.Template;
import ru.org.linux.topic.PreparedTopic;
import ru.org.linux.topic.TopicPermissionService;
import ru.org.linux.topic.TopicPrepareService;
import ru.org.linux.user.User;
import ru.org.linux.util.ServletParameterException;
import ru.org.linux.util.StringUtil;

import javax.servlet.http.HttpServletRequest;
import javax.validation.Valid;
import java.util.HashMap;
import java.util.Map;

@Controller
public class AddCommentController {
  @Autowired
  private IPBlockDao ipBlockDao;

  @Autowired
  private CommentPrepareService commentPrepareService;

  @Autowired
  private CommentService commentService;

  @Autowired
  private TopicPermissionService topicPermissionService;

  @Autowired
  private TopicPrepareService messagePrepareService;

  @Autowired
  private SearchQueueSender searchQueueSender;

  @ModelAttribute("ipBlockInfo")
  private IPBlockInfo loadIPBlock(HttpServletRequest request) {
    return ipBlockDao.getBlockInfo(request.getRemoteAddr());
  }

  /**
   * Показ формы добавления комментария.
   *
   * @param add      WEB-форма, содержащая данные
   * @param request  данные запроса от web-клиента
   * @return объект web-модели
   * @throws Exception
   */
  @RequestMapping(value = "/add_comment.jsp", method = RequestMethod.GET)
  public ModelAndView showFormReply(
    @ModelAttribute("add") @Valid CommentRequest add,
    HttpServletRequest request
  ) throws Exception {
    if (add.getTopic() == null) {
      throw new ServletParameterException("тема на задана");
    }

    Template tmpl = Template.getTemplate(request);

    Map<String, Object> params = new HashMap<>();

    if (add.getMode() == null) {
      add.setMode(tmpl.getFormatMode());
    }

    commentService.prepareReplyto(add, params, request);

    int postscore = topicPermissionService.getPostscore(add.getTopic());

    params.put("postscoreInfo", TopicPermissionService.getPostScoreInfo(postscore));

    return new ModelAndView("add_comment", params);
  }

  /**
   * Показ топика с формой добавления комментария.
   *
   * @param add      WEB-форма, содержащая данные
   * @param request  данные запроса от web-клиента
   * @return объект web-модели
   * @throws Exception
   */
  @RequestMapping("/comment-message.jsp")
  public ModelAndView showFormTopic(
    @ModelAttribute("add") @Valid CommentRequest add,
    HttpServletRequest request
  ) throws AccessViolationException {
    Template tmpl = Template.getTemplate(request);

    PreparedTopic preparedTopic = messagePrepareService.prepareTopic(add.getTopic(), request.isSecure(), tmpl.getCurrentUser());

    if (!topicPermissionService.isCommentsAllowed(preparedTopic.getGroup(), add.getTopic(), tmpl.getCurrentUser())) {
      throw new AccessViolationException("Это сообщение нельзя комментировать");
    }

    if (add.getMode() == null) {
      add.setMode(tmpl.getFormatMode());
    }

    return new ModelAndView(
      "comment-message",
      "preparedMessage",
            preparedTopic
    );
  }

  /**
   * Добавление комментария.
   *
   * @param add      WEB-форма, содержащая данные
   * @param errors   обработчик ошибок ввода для формы
   * @param request  данные запроса от web-клиента
   * @return объект web-модели
   * @throws Exception
   */
  @RequestMapping(value = "/add_comment.jsp", method = RequestMethod.POST)
  @CSRFNoAuto
  public ModelAndView addComment(
    @ModelAttribute("add") @Valid CommentRequest add,
    Errors errors,
    HttpServletRequest request,
    @ModelAttribute("ipBlockInfo") IPBlockInfo ipBlockInfo
  ) throws Exception {

    Map<String, Object> formParams = new HashMap<>();

    User user = commentService.getCommentUser(add, request, errors);

    commentService.checkPostData(add, user, ipBlockInfo, request, errors);
    commentService.prepareReplyto(add, formParams, request);

    String msg = commentService.getCommentBody(add, user, errors);
    Comment comment = commentService.getComment(add, user, request);

    if (add.getTopic() != null) {
      int postscore = topicPermissionService.getPostscore(add.getTopic());
      formParams.put("postscoreInfo", TopicPermissionService.getPostScoreInfo(postscore));

      topicPermissionService.checkCommentsAllowed(add.getTopic(), user, errors);
      formParams.put("comment", commentPrepareService.prepareCommentForEdit(comment, msg, request.isSecure()));
    }

    if (add.isPreviewMode() || errors.hasErrors() || comment == null) {
      ModelAndView modelAndView = new ModelAndView("add_comment", formParams);
      add.setMsg(StringUtil.escapeForceHtml(add.getMsg()));
      return modelAndView;
    }

    int msgid = commentService.create(
      user,
      comment,
      msg,
      request.getRemoteAddr(),
      request.getHeader("X-Forwarded-For"),
      request.getHeader("user-agent")
    );
    searchQueueSender.updateComment(msgid);

    String returnUrl = "jump-message.jsp?msgid=" + add.getTopic().getId() + "&cid=" + msgid;
    return new ModelAndView(new RedirectView(returnUrl));
  }

  @InitBinder("add")
  public void requestValidator(WebDataBinder binder) {
    commentService.requestValidator(binder);
  }

  @InitBinder
  public void initBinder(WebDataBinder binder) {
    commentService.initBinder(binder);
  }
}
