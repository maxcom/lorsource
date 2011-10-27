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
import ru.org.linux.util.bbcode.LorCodeService;

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
  private LorCodeService lorCodeService;

  @Autowired
  private TagDao tagDao;

  @Autowired
  private MemoriesDao memoriesDao;

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

  @Autowired
  public void setLorCodeService(LorCodeService lorCodeService) {
    this.lorCodeService = lorCodeService;
  }

  /**
   * Функция подготовки голосования
   * @param poll голосование
   * @return подготовленное голосование
   */
  public PreparedPoll preparePoll(Poll poll) {
    return new PreparedPoll(poll, pollDao.getMaxVote(poll), pollDao.getCountUsers(poll), pollDao.getPollVariants(poll, Poll.ORDER_VOTES));
  }

  public PreparedMessage prepareMessage(Message message, boolean minimizeCut, boolean secure) {
    return prepareMessage(message, messageDao.getTags(message), minimizeCut, null, secure);
  }

  public PreparedMessage prepareMessage(Message message, List<String> tags, PreparedPoll newPoll, boolean secure) {
    return prepareMessage(message, tags, false, newPoll, secure);
  }

  /**
   * Функция подготовки топика
   * @param message топик
   * @param tags список тэгов
   * @param minimizeCut сворачивать ли cut
   * @param poll опрос к топику
   * @param secure является ли соединение https
   * @return подготовленный топик
   */
  private PreparedMessage prepareMessage(Message message, List<String> tags, boolean minimizeCut, PreparedPoll poll, boolean secure) {
    try {
      Group group = groupDao.getGroup(message.getGroupId());
      User author = userDao.getUserCached(message.getUid());
      Section section = sectionDao.getSection(message.getSectionId());

      DeleteInfo deleteInfo;
      User deleteUser;
      if (message.isDeleted()) {
        deleteInfo = deleteInfoDao.getDeleteInfo(message.getId());

        if (deleteInfo!=null) {
          deleteUser = userDao.getUserCached(deleteInfo.getUserid());
        } else {
          deleteUser = null;
        }
      } else {
        deleteInfo = null;
        deleteUser = null;
      }

      PreparedPoll preparedPoll;

      if (message.isVotePoll()) {
        if (poll==null) {
          preparedPoll = preparePoll(pollDao.getPollByTopicId(message.getId()));
        } else {
          preparedPoll = poll;
        }
      } else {
        preparedPoll = null;
      }

      User commiter;

      if (message.getCommitby()!=0) {
        commiter = userDao.getUserCached(message.getCommitby());
      } else {
        commiter = null;
      }

      List<EditInfoDTO> editInfo = messageDao.getEditInfo(message.getId());
      EditInfoDTO lastEditInfo;
      User lastEditor;
      int editCount;

      if (!editInfo.isEmpty()) {
        lastEditInfo = editInfo.get(0);
        lastEditor = userDao.getUserCached(lastEditInfo.getEditor());
        editCount = editInfo.size();
      } else {
        lastEditInfo = null;
        lastEditor = null;
        editCount = 0;
      }

      String processedMessage;
      if(message.isLorcode()) {
        if(minimizeCut) {
          String url = configuration.getMainUrl() + message.getLink();
          processedMessage = lorCodeService.parseTopicWithMinimizedCut(message.getMessage(), url, secure);
        } else {
          processedMessage = lorCodeService.parseTopic(message.getMessage(), secure);
        }
      } else {
        processedMessage = "<p>" + message.getMessage();
      }

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

  public PreparedComment prepareComment(Comment comment, CommentList comments, boolean secure) throws UserNotFoundException {
    User author = userDao.getUserCached(comment.getUserid());
    String processedMessage = commentDao.getPreparedComment(comment.getId(), secure);
    User replyAuthor;
    if (comment.getReplyTo()!=0 && comments!=null) {
      CommentNode replyNode = comments.getNode(comment.getReplyTo());

      if (replyNode!=null) {
        Comment reply = replyNode.getComment();
        replyAuthor = userDao.getUserCached(reply.getUserid());
      } else {
        replyAuthor = null;
      }
    } else {
      replyAuthor = null;
    }
    return new PreparedComment(comment, author, processedMessage, replyAuthor);
  }

  public PreparedComment prepareComment(Comment comment, boolean secure) throws UserNotFoundException {
    return prepareComment(comment, (CommentList)null, secure);
  }

  public PreparedComment prepareComment(Comment comment, String message, boolean secure) throws UserNotFoundException {
    User author = userDao.getUserCached(comment.getUserid());
    String processedMessage = lorCodeService.parseComment(message, secure);

    return new PreparedComment(comment, author, processedMessage, null);
  }

  public List<PreparedComment> prepareCommentList(CommentList comments, List<Comment> list, boolean secure) throws UserNotFoundException {
    List<PreparedComment> commentsPrepared = new ArrayList<PreparedComment>(list.size());
    for (Comment comment : list) {
      commentsPrepared.add(prepareComment(comment, comments, secure));
    }
    return commentsPrepared;
  }

  public PreparedGroupInfo prepareGroupInfo(Group group, boolean secure) {
    String longInfo;

    if (group.getLongInfo()!=null) {
      longInfo = lorCodeService.parseComment(group.getLongInfo(), secure);
    } else {
      longInfo = null;
    }

    return new PreparedGroupInfo(group, longInfo);
  }

  public MessageMenu getMessageMenu(PreparedMessage message, User currentUser) {
    boolean editable = currentUser!=null && message.isEditable(currentUser);
    boolean resolvable;
    int memoriesId;

    if (currentUser!=null) {
      resolvable = (currentUser.canModerate() || (message.getAuthor().getId()==currentUser.getId())) &&
            message.getGroup().isResolvable();

      memoriesId = memoriesDao.getId(currentUser, message.getMessage());
    } else {
      resolvable = false;
      memoriesId = 0;
    }

    return new MessageMenu(editable, resolvable, memoriesId);
  }

  /**
   * Подготовка ленты топиков, используется в NewsViewerController например
   * сообщения рендерятся со свернутым cut
   * @param messages список топиков
   * @param secure является ли соединение https
   * @return список подготовленных топиков
   */
  public List<PreparedMessage> prepareMessagesFeed(List<Message> messages, boolean secure) {
    List<PreparedMessage> pm = new ArrayList<PreparedMessage>(messages.size());

    for (Message message : messages) {
      PreparedMessage preparedMessage = prepareMessage(message, messageDao.getTags(message), true, null, secure);
      pm.add(preparedMessage);
    }

    return pm;
  }

  public List<PreparedEditInfo> build(Message message, boolean secure) throws UserNotFoundException, UserErrorException {
    List<EditInfoDTO> editInfoDTOs = messageDao.loadEditInfo(message.getId());
    List<PreparedEditInfo> editInfos = new ArrayList<PreparedEditInfo>(editInfoDTOs.size());

    String currentMessage = message.getMessage();
    String currentTitle = message.getTitle();
    String currentUrl = message.getUrl();
    String currentLinktext = message.getLinktext();
    List<String> currentTags = tagDao.getMessageTags(message.getMessageId());

    for (int i = 0; i<editInfoDTOs.size(); i++) {
      EditInfoDTO dto = editInfoDTOs.get(i);

      editInfos.add(
        new PreparedEditInfo(
          lorCodeService,
          secure,
          userDao,
          dto,
          dto.getOldmessage()!=null ? currentMessage : null,
          dto.getOldtitle()!=null ? currentTitle : null,
          dto.getOldurl()!=null ? currentUrl : null,
          dto.getOldlinktext()!=null ? currentLinktext : null,
          dto.getOldtags()!=null ? currentTags : null,
          i==0,
          false
        )
      );

      if (dto.getOldmessage() !=null) {
        currentMessage = dto.getOldmessage();
      }

      if (dto.getOldtitle() != null) {
        currentTitle = dto.getOldtitle();
      }

      if (dto.getOldurl() != null) {
        currentUrl = dto.getOldurl();
      }

      if (dto.getOldlinktext() != null) {
        currentLinktext = dto.getOldlinktext();
      }

      if (dto.getOldtags()!=null) {
        currentTags = TagDao.parseTags(dto.getOldtags());
      }
    }

    if (!editInfoDTOs.isEmpty()) {
      EditInfoDTO current = EditInfoDTO.createFromMessage(tagDao, message);

      editInfos.add(new PreparedEditInfo(lorCodeService, secure, userDao, current, currentMessage, currentTitle, currentUrl, currentLinktext, currentTags, false, true));
    }

    return editInfos;
  }
}
