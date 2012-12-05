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
package ru.org.linux.user;

import com.google.common.collect.ImmutableList;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import ru.org.linux.comment.CommentDao;
import ru.org.linux.comment.CommentService;
import ru.org.linux.group.GroupDao;
import ru.org.linux.section.SectionService;
import ru.org.linux.topic.TopicListDao;

import java.util.List;

/**
 */
@Service
public class DeletedMessageService {

  @Autowired
  private TopicListDao topicListDao;

  @Autowired
  private CommentDao commentDao;

  @Autowired
  private SectionService sectionService;

  @Autowired
  private CommentService commentService;

  @Autowired
  private GroupDao groupDao;

  @Autowired
  private UserDao userDao;


  public List<PreparedDeleteMessage> prepareDeletedCommentForUser(User user) throws Exception {
    final ImmutableList.Builder<PreparedDeleteMessage> builder = ImmutableList.builder();

    for(CommentDao.DeletedCommentForUser deletedComment : commentDao.getDeletedCommentsForUser(user, 0, 0)) {
      builder.add(new PreparedDeleteMessage(
          deletedComment.getId(),
          sectionService.getSection(deletedComment.getSectionId()).getTitle(),
          groupDao.getGroup(deletedComment.getGroupId()).getTitle(),
          deletedComment.getTitle(),
          deletedComment.getReason(),
          deletedComment.getBonus(),
          userDao.getUserCached(deletedComment.getModeratorId()),
          deletedComment.getDate()
      ));
    }

    return builder.build();
  }

  public List<PreparedDeleteMessage> prepareDeletedTopicForUser(User user) throws Exception {
    final ImmutableList.Builder<PreparedDeleteMessage> builder = ImmutableList.builder();

    for(TopicListDao.DeletedTopicForUser deletedTopic : topicListDao.getDeletedTopicsForUser(user, 0, 0)) {
      builder.add(new PreparedDeleteMessage(
          deletedTopic.getId(),
          sectionService.getSection(deletedTopic.getSectionId()).getTitle(),
          groupDao.getGroup(deletedTopic.getGroupId()).getTitle(),
          deletedTopic.getTitle(),
          deletedTopic.getReason(),
          deletedTopic.getBonus(),
          userDao.getUserCached(deletedTopic.getModeratorId()),
          deletedTopic.getDate()
      ));
    }

    return builder.build();
  }
}
