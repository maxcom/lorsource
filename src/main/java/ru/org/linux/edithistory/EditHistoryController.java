/*
 * Copyright 1998-2022 Linux.org.ru
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

package ru.org.linux.edithistory;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.ModelAndView;
import ru.org.linux.comment.Comment;
import ru.org.linux.comment.CommentReadService;
import ru.org.linux.group.Group;
import ru.org.linux.group.GroupDao;
import ru.org.linux.group.GroupPermissionService;
import ru.org.linux.site.Template;
import ru.org.linux.topic.*;

import javax.servlet.http.HttpServletRequest;
import java.util.List;

@Controller
public class EditHistoryController {
  private final TopicDao messageDao;

  private final EditHistoryService editHistoryService;

  private final CommentReadService commentService;

  private final TopicPermissionService topicPermissionService;

  private final GroupPermissionService groupPermissionService;

  private final GroupDao groupDao;

  private final TopicPrepareService topicPrepareService;

  public EditHistoryController(TopicDao messageDao, EditHistoryService editHistoryService,
                               CommentReadService commentService, TopicPermissionService topicPermissionService,
                               GroupPermissionService groupPermissionService, GroupDao groupDao,
                               TopicPrepareService topicPrepareService) {
    this.messageDao = messageDao;
    this.editHistoryService = editHistoryService;
    this.commentService = commentService;
    this.topicPermissionService = topicPermissionService;
    this.groupPermissionService = groupPermissionService;
    this.groupDao = groupDao;
    this.topicPrepareService = topicPrepareService;
  }

  @RequestMapping({
    "/news/{group}/{id}/history",
    "/forum/{group}/{id}/history",
    "/gallery/{group}/{id}/history",
    "/polls/{group}/{id}/history"
})
  public ModelAndView showEditInfo(
    HttpServletRequest request,
    @PathVariable("id") int msgid
  ) throws Exception {
    Topic message = messageDao.getById(msgid);
    Template tmpl = Template.getTemplate(request);
    Group group = groupDao.getGroup(message.getGroupId());

    PreparedTopic preparedMessage = topicPrepareService.prepareTopic(message, tmpl.getCurrentUser());

    topicPermissionService.checkView(group, message, tmpl.getCurrentUser(), preparedMessage.getAuthor(), false);

    List<PreparedEditHistory> editHistories = editHistoryService.prepareEditInfo(message);

    ModelAndView modelAndView = new ModelAndView("history");

    modelAndView.getModel().put("message", message);
    modelAndView.getModel().put("editHistories", editHistories);
    modelAndView.getModel().put("canRestore", groupPermissionService.isEditable(preparedMessage, tmpl.getCurrentUser()));

    return modelAndView;
  }

  @RequestMapping({
    "/news/{group}/{id}/{commentid}/history",
    "/forum/{group}/{id}/{commentid}/history",
    "/gallery/{group}/{id}/{commentid}/history",
    "/polls/{group}/{id}/{commentid}/history"
  })
  public ModelAndView showCommentEditInfo(
    HttpServletRequest request,
    @PathVariable("id") int msgid,
    @PathVariable("commentid") int commentId
  ) throws Exception {
    Topic message = messageDao.getById(msgid);
    Comment comment =  commentService.getById(commentId);

    List<PreparedEditHistory> editHistories = editHistoryService.prepareEditInfo(comment);

    ModelAndView modelAndView = new ModelAndView("history");

    modelAndView.getModel().put("message", message);
    modelAndView.getModel().put("editHistories", editHistories);
    modelAndView.getModel().put("canRestore", false);

    return modelAndView;
  }
}
