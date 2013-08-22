/*
 * Copyright 1998-2013 Linux.org.ru
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

package ru.org.linux.topic;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import ru.org.linux.auth.AuthUtil;
import ru.org.linux.comment.*;
import ru.org.linux.group.Group;
import ru.org.linux.group.GroupDao;
import ru.org.linux.section.Section;
import ru.org.linux.section.SectionService;
import ru.org.linux.site.MessageNotFoundException;
import ru.org.linux.site.PublicApi;
import ru.org.linux.site.Template;

import javax.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.Map;

@Controller
@PublicApi
public class TopicApiController {
  @Autowired
  private TopicDao topicDao;

  @Autowired
  private GroupDao groupDao;

  @Autowired
  private SectionService sectionService;

  @Autowired
  private TopicPermissionService permissionService;

  @Autowired
  private CommentService commentService;

  @Autowired
  private CommentPrepareService prepareService;

  @RequestMapping(value="/{section}/{group}/{id}/comments", produces = "application/json; charset=UTF-8", method = RequestMethod.GET)
  @ResponseBody
  public Map<String, Object> getComments(
          @PathVariable("section") String sectionName,
          @PathVariable("group") String groupName,
          @PathVariable("id") int msgid,
          @RequestParam(value = "page", defaultValue = "0") int page,
          HttpServletRequest request
  ) throws Exception {
    Topic topic = topicDao.getById(msgid);
    Group group = groupDao.getGroup(topic.getGroupId());
    Section section = sectionService.getSection(group.getSectionId());

    if (!section.getUrlName().equals(sectionName)
            || !group.getUrlName().equals(groupName)
            || page<0 ) {
      throw new MessageNotFoundException(msgid);
    }

    permissionService.checkView(group, topic, AuthUtil.getCurrentUser(), false);

    CommentList comments = commentService.getCommentList(topic, false);

    CommentFilter cv = new CommentFilter(comments);

    int messagesPerPage = AuthUtil.getProfile().getMessages();

    List<Comment> commentsFiltered = cv.getCommentsForPage(
            false,
            page,
            messagesPerPage,
            ImmutableSet.<Integer>of()
    );

    List<PreparedComment> preparedComments = prepareService.prepareCommentList(
            comments,
            commentsFiltered,
            request.isSecure(),
            Template.getTemplate(request),
            topic
    );

    return ImmutableMap.of(
            "comments", preparedComments,
            "topic", new ApiCommentTopicInfo(
            topic.getId(),
            topic.getLink(),
            permissionService.isCommentsAllowed(
                    group,
                    topic,
                    AuthUtil.getCurrentUser())
    )
    );
  }
}
