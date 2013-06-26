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
import org.springframework.context.support.ApplicationObjectSupport;
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
import ru.org.linux.spring.dao.MessageText;
import ru.org.linux.spring.dao.MsgbaseDao;
import ru.org.linux.topic.TopicPermissionService;
import ru.org.linux.user.User;
import ru.org.linux.util.ServletParameterException;

import javax.servlet.http.HttpServletRequest;
import javax.validation.Valid;
import java.util.HashMap;
import java.util.Map;

@Controller
public class EditCommentController extends ApplicationObjectSupport {

  @Autowired
  private CommentService commentService;

  @Autowired
  private MsgbaseDao msgbaseDao;

  @Autowired
  private IPBlockDao ipBlockDao;

  @Autowired
  private TopicPermissionService topicPermissionService;

  @Autowired
  private CommentPrepareService commentPrepareService;

  @Autowired
  private SearchQueueSender searchQueueSender;

  @InitBinder("edit")
  public void requestValidator(WebDataBinder binder) {
    commentService.requestValidator(binder);
  }

  @InitBinder
  public void initBinder(WebDataBinder binder) {
    commentService.initBinder(binder);
  }

  @ModelAttribute("ipBlockInfo")
  private IPBlockInfo loadIPBlock(HttpServletRequest request) {
    return ipBlockDao.getBlockInfo(request.getRemoteAddr());
  }

  /**
   * Показ формы изменения комментария.
   *
   *
   * @param commentRequest  WEB-форма, содержащая данные
   * @return объект web-модели
   * @throws ServletParameterException
   */
  @RequestMapping(value = "/edit_comment", method = RequestMethod.GET)
  public ModelAndView editCommentShowHandler(
          @ModelAttribute("edit") @Valid CommentRequest commentRequest
  ) throws ServletParameterException {
    if (commentRequest.getTopic() == null) {
      throw new ServletParameterException("тема на задана");
    }
    Comment original = commentRequest.getOriginal();
    if (original == null) {
      throw new ServletParameterException("Комментарий на задан");
    }
    MessageText messageText = msgbaseDao.getMessageText(original.getId());
    commentRequest.setMsg(messageText.getText());
    commentRequest.setTitle(original.getTitle());

    return new ModelAndView("edit_comment");
  }

  /**
   * Изменение комментария.
   *
   * @param commentRequest WEB-форма, содержащая данные
   * @param errors         обработчик ошибок ввода для формы
   * @param request        данные запроса от web-клиента
   * @return объект web-модели
   * @throws Exception
   */
  @RequestMapping(value = "/edit_comment", method = RequestMethod.POST)
  @CSRFNoAuto
  public ModelAndView editCommentPostHandler(
    @ModelAttribute("edit") @Valid CommentRequest commentRequest,
    Errors errors,
    HttpServletRequest request,
    @ModelAttribute("ipBlockInfo") IPBlockInfo ipBlockInfo
  ) throws Exception {
    Map<String, Object> formParams = new HashMap<>();

    User user = commentService.getCommentUser(commentRequest, request, errors);

    commentService.checkPostData(commentRequest, user, ipBlockInfo, request, errors);
    commentService.prepareReplyto(commentRequest, formParams, request);

    String msg = commentService.getCommentBody(commentRequest, user, errors);
    Comment comment = commentService.getComment(commentRequest, user, request);

    if (commentRequest.getTopic() != null) {
      int postscore = topicPermissionService.getPostscore(commentRequest.getTopic());

      formParams.put("postscoreInfo", TopicPermissionService.getPostScoreInfo(postscore));
      topicPermissionService.checkCommentsAllowed(commentRequest.getTopic(), user, errors);
      formParams.put("comment", commentPrepareService.prepareCommentForEdit(comment, msg, request.isSecure()));
    }

    Template tmpl = Template.getTemplate(request);

    boolean editable = topicPermissionService.isCommentsEditingAllowed(
            commentRequest.getOriginal(),
            commentRequest.getTopic(),
            tmpl.getCurrentUser()
    );

    if (!editable) {
      throw new AccessViolationException("у Вас нет прав на редактирование этого сообщения");
    }

    if (commentRequest.isPreviewMode() || errors.hasErrors() && comment == null) {
      ModelAndView modelAndView = new ModelAndView("edit_comment", formParams);
      modelAndView.addObject("ipBlockInfo", ipBlockInfo);
      return modelAndView;
    }

    String originalMessageText = msgbaseDao.getMessageText(commentRequest.getOriginal().getId()).getText();

    commentService.edit(
      commentRequest.getOriginal(),
      comment,
      msg,
      request.getRemoteAddr(),
      request.getHeader("X-Forwarded-For")
    );
    searchQueueSender.updateComment(commentRequest.getOriginal().getId());

    commentService.addEditHistoryItem(user, commentRequest.getOriginal(), originalMessageText, comment, msg);
    commentService.updateLatestEditorInfo(user, commentRequest.getOriginal(), comment);

    String returnUrl =
      "/jump-message.jsp?msgid=" + commentRequest.getTopic().getId() +
        "&cid=" + commentRequest.getOriginal().getId();

    return new ModelAndView(new RedirectView(returnUrl));
  }
}
