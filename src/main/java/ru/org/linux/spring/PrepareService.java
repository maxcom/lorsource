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
import org.springframework.stereotype.Service;
import ru.org.linux.site.*;
import ru.org.linux.spring.dao.*;

import java.util.ArrayList;
import java.util.List;

/**
 * Класс в котором функции для создания  PreparedPoll, PreparedMessage, PreparedComment
 */
@Service
public class PrepareService {
  private PollDao pollDao;
  private GroupDao groupDao;
  private UserDao userDao;
  private SectionDao sectionDao;
  private DeleteInfoDao deleteInfoDao;
  private MessageDao messageDao;
  private CommentDao commentDao;
  private UserAgentDao userAgentDao;
  private Configuration configuration;

  @Autowired
  public void setPollDao(PollDao pollDao) {
    this.pollDao = pollDao;
  }

  @Autowired
  public void setGroupDao(GroupDao groupDao) {
    this.groupDao = groupDao;
  }

  @Autowired
  public void setUserDao(UserDao userDao) {
    this.userDao = userDao;
  }

  @Autowired
  public void setSectionDao(SectionDao sectionDao) {
    this.sectionDao = sectionDao;
  }

  @Autowired
  public void setDeleteInfoDao(DeleteInfoDao deleteInfoDao) {
    this.deleteInfoDao = deleteInfoDao;
  }

  @Autowired
  public void setMessageDao(MessageDao messageDao) {
    this.messageDao = messageDao;
  }

  @Autowired
  public void setCommentDao(CommentDao commentDao) {
    this.commentDao = commentDao;
  }

  @Autowired
  public void setUserAgentDao(UserAgentDao userAgentDao) {
    this.userAgentDao = userAgentDao;
  }

  @Autowired
  public void setConfiguration(Configuration configuration) {
    this.configuration = configuration;
  }

  /**
   * Функция подготовки голосования
   * @param poll голосование
   * @return подготовленное голосование
   */
  public PreparedPoll preparePoll(Poll poll) {
    return new PreparedPoll(poll, pollDao.getMaxVote(poll), pollDao.getPollVariants(poll, Poll.ORDER_VOTES));
  }

  public PreparedMessage prepareMessage(Message message, boolean includeCut) {
    return prepareMessage(message, messageDao.getTags(message), includeCut);
  }

  public PreparedMessage prepareMessage(Message message, List<String> tags) {
    return prepareMessage(message, tags, true);
  }

  /**
   * Функция подготовки топика
   * @param message топик
   * @param tags список тэгов
   * @param includeCut отображать ли cut
   * @return подготовленный топик
   */
  public PreparedMessage prepareMessage(Message message, List<String> tags, boolean includeCut) {
    try {
      Group group = groupDao.getGroup(message.getGroupId());
      User author = userDao.getUser(message.getUid());
      Section section = sectionDao.getSection(message.getSectionId());

      DeleteInfo deleteInfo;
      User deleteUser;
      if (message.isDeleted()) {
        deleteInfo = deleteInfoDao.getDeleteInfo(message.getId());

        if (deleteInfo!=null) {
          deleteUser = userDao.getUser(deleteInfo.getUserid());
        } else {
          deleteUser = null;
        }
      } else {
        deleteInfo = null;
        deleteUser = null;
      }

      PreparedPoll preparedPoll;

      if (message.isVotePoll()) {
        preparedPoll = preparePoll(pollDao.getPoll(pollDao.getPollId(message.getId())));
      } else {
        preparedPoll = null;
      }

      User commiter;

      if (message.getCommitby()!=0) {
        commiter = userDao.getUser(message.getCommitby());
      } else {
        commiter = null;
      }

      List<EditInfoDTO> editInfo = messageDao.getEditInfo(message.getId());
      EditInfoDTO lastEditInfo;
      User lastEditor;
      int editCount;

      if (!editInfo.isEmpty()) {
        lastEditInfo = editInfo.get(0);
        lastEditor = userDao.getUser(lastEditInfo.getEditor());
        editCount = editInfo.size();
      } else {
        lastEditInfo = null;
        lastEditor = null;
        editCount = 0;
      }

      String processedMessage = message.getProcessedMessage(userDao, includeCut, configuration.getMainUrl());
      String userAgent = userAgentDao.getUserAgentById(message.getUserAgent());

      return new PreparedMessage(message, author, deleteInfo, deleteUser, processedMessage, preparedPoll, commiter, tags, group, section, lastEditInfo, lastEditor, editCount, userAgent);
    } catch (BadGroupException e) {
      throw new RuntimeException(e);
    } catch (UserNotFoundException e) {
      throw new RuntimeException(e);
    } catch (PollNotFoundException e) {
      throw new RuntimeException(e);
    } catch (SectionNotFoundException e) {
      throw new RuntimeException(e);
    }
  }

  public PreparedComment prepareComment(Comment comment, CommentList comments) throws UserNotFoundException {
    User author = userDao.getUser(comment.getUserid());
    String processedMessage = commentDao.getPreparedComment(comment.getId());
    User replyAuthor;
    if (comment.getReplyTo()!=0 && comments!=null) {
      CommentNode replyNode = comments.getNode(comment.getReplyTo());

      if (replyNode!=null) {
        Comment reply = replyNode.getComment();
        replyAuthor = userDao.getUser(reply.getUserid());
      } else {
        replyAuthor = null;
      }
    } else {
      replyAuthor = null;
    }
    return new PreparedComment(comment, author, processedMessage, replyAuthor);
  }

  public PreparedComment prepareComment(Comment comment) throws UserNotFoundException {
    return prepareComment(comment, (CommentList)null);
  }

  public PreparedComment prepareComment(Comment comment, String message) throws UserNotFoundException {
    User author = userDao.getUserCached(comment.getUserid());
    String processedMessage = PreparedComment.getProcessedMessage(userDao, message).getHtml();

    return new PreparedComment(comment, author, processedMessage, null);
  }

  public List<PreparedComment> prepareCommentList(CommentList comments, List<Comment> list) throws UserNotFoundException {
    List<PreparedComment> commentsPrepared = new ArrayList<PreparedComment>(list.size());
    for (Comment comment : list) {
      commentsPrepared.add(prepareComment(comment, comments));
    }
    return commentsPrepared;
  }
}
