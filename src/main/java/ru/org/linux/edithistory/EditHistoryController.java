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

package ru.org.linux.edithistory;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.ModelAndView;
import ru.org.linux.comment.Comment;
import ru.org.linux.comment.CommentService;
import ru.org.linux.group.Group;
import ru.org.linux.group.GroupDao;
import ru.org.linux.site.Template;
import ru.org.linux.topic.Topic;
import ru.org.linux.topic.TopicDao;
import ru.org.linux.topic.TopicPermissionService;

import javax.servlet.http.HttpServletRequest;
import java.util.List;

@Controller
public class EditHistoryController {
  @Autowired
  private TopicDao messageDao;

  @Autowired
  private EditHistoryService editHistoryService;

  @Autowired
  private CommentService commentService;

  @Autowired
  private TopicPermissionService topicPermissionService;

  @Autowired
  private GroupDao groupDao;

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

    topicPermissionService.checkView(group, message, tmpl.getCurrentUser(), false);

    List<PreparedEditHistory> editHistories = editHistoryService.prepareEditInfo(message, request.isSecure());

    ModelAndView modelAndView = new ModelAndView("history");

    modelAndView.getModel().put("message", message);
    modelAndView.getModel().put("editHistories", editHistories);

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

    List<PreparedEditHistory> editHistories = editHistoryService.prepareEditInfo(comment, request.isSecure());

    ModelAndView modelAndView = new ModelAndView("history");

    modelAndView.getModel().put("message", message);
    modelAndView.getModel().put("editHistories", editHistories);

    return modelAndView;
  }
}
