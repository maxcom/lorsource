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
import ru.org.linux.dao.*;
import ru.org.linux.dto.*;
import ru.org.linux.dao.TagCloudDao;
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
  private TagCloudDao tagCloudDao;

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
   *
   * @param poll голосование
   * @return подготовленное голосование
   */
  public PreparedPoll preparePoll(Poll poll) {
    return new PreparedPoll(poll, pollDao.getMaxVote(poll), pollDao.getCountUsers(poll), pollDao.getPollVariants(poll, Poll.ORDER_VOTES));
  }

  public PreparedMessage prepareMessage(MessageDto messageDto, boolean minimizeCut, boolean secure) {
    return prepareMessage(messageDto, messageDao.getTags(messageDto), minimizeCut, null, secure);
  }

  public PreparedMessage prepareMessage(MessageDto messageDto, List<String> tags, PreparedPoll newPoll, boolean secure) {
    return prepareMessage(messageDto, tags, false, newPoll, secure);
  }

  /**
   * Функция подготовки топика
   *
   * @param messageDto     топик
   * @param tags        список тэгов
   * @param minimizeCut сворачивать ли cut
   * @param poll        опрос к топику
   * @param secure      является ли соединение https
   * @return подготовленный топик
   */
  private PreparedMessage prepareMessage(MessageDto messageDto, List<String> tags, boolean minimizeCut, PreparedPoll poll, boolean secure) {
    try {
      GroupDto groupDto = groupDao.getGroup(messageDto.getGroupId());
      UserDto author = userDao.getUserCached(messageDto.getUid());
      SectionDto sectionDto = sectionDao.getSection(messageDto.getSectionId());

      DeleteInfoDto deleteInfoDto;
      UserDto deleteUser;
      if (messageDto.isDeleted()) {
        deleteInfoDto = deleteInfoDao.getDeleteInfo(messageDto.getId());

        if (deleteInfoDto != null) {
          deleteUser = userDao.getUserCached(deleteInfoDto.getUserid());
        } else {
          deleteUser = null;
        }
      } else {
        deleteInfoDto = null;
        deleteUser = null;
      }

      PreparedPoll preparedPoll;

      if (messageDto.isVotePoll()) {
        if (poll == null) {
          preparedPoll = preparePoll(pollDao.getPollByTopicId(messageDto.getId()));
        } else {
          preparedPoll = poll;
        }
      } else {
        preparedPoll = null;
      }

      UserDto commiter;

      if (messageDto.getCommitby() != 0) {
        commiter = userDao.getUserCached(messageDto.getCommitby());
      } else {
        commiter = null;
      }

      List<EditInfoDTO> editInfo = messageDao.getEditInfo(messageDto.getId());
      EditInfoDTO lastEditInfo;
      UserDto lastEditor;
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
      if (messageDto.isLorcode()) {
        if (minimizeCut) {
          String url = configuration.getMainUrl() + messageDto.getLink();
          processedMessage = lorCodeService.parseTopicWithMinimizedCut(messageDto.getMessage(), url, secure);
        } else {
          processedMessage = lorCodeService.parseTopic(messageDto.getMessage(), secure);
        }
      } else {
        processedMessage = "<p>" + messageDto.getMessage();
      }

      String userAgent = userAgentDao.getUserAgentById(messageDto.getUserAgent());

      return new PreparedMessage(messageDto, author, deleteInfoDto, deleteUser, processedMessage, preparedPoll, commiter, tags, groupDto, sectionDto, lastEditInfo, lastEditor, editCount, userAgent);
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

  public PreparedComment prepareCommentRSS(CommentDto commentDto, CommentList comments, boolean secure) throws UserNotFoundException {
    return prepareComment(commentDto, comments, secure, true);
  }

  public PreparedComment prepareComment(CommentDto commentDto, CommentList comments, boolean secure) throws UserNotFoundException {
    return prepareComment(commentDto, comments, secure, false);
  }

  public PreparedComment prepareComment(CommentDto commentDto, CommentList comments, boolean secure, boolean rss) throws UserNotFoundException {
    UserDto author = userDao.getUserCached(commentDto.getUserid());
    String processedMessage;
    if (!rss) {
      processedMessage = commentDao.getPreparedComment(commentDto.getId(), secure);
    } else {
      processedMessage = commentDao.getPreparedCommentRSS(commentDto.getId(), secure);
    }
    UserDto replyAuthor;
    if (commentDto.getReplyTo() != 0 && comments != null) {
      CommentNode replyNode = comments.getNode(commentDto.getReplyTo());

      if (replyNode != null) {
        CommentDto reply = replyNode.getComment();
        replyAuthor = userDao.getUserCached(reply.getUserid());
      } else {
        replyAuthor = null;
      }
    } else {
      replyAuthor = null;
    }
    return new PreparedComment(commentDto, author, processedMessage, replyAuthor);
  }

  public PreparedComment prepareComment(CommentDto commentDto, boolean secure) throws UserNotFoundException {
    return prepareComment(commentDto, (CommentList) null, secure);
  }

  public PreparedComment prepareComment(CommentDto commentDto, String message, boolean secure) throws UserNotFoundException {
    UserDto author = userDao.getUserCached(commentDto.getUserid());
    String processedMessage = lorCodeService.parseComment(message, secure);

    return new PreparedComment(commentDto, author, processedMessage, null);
  }

  public List<PreparedComment> prepareCommentListRSS(CommentList comments, List<CommentDto> list, boolean secure) throws UserNotFoundException {
    List<PreparedComment> commentsPrepared = new ArrayList<PreparedComment>(list.size());
    for (CommentDto commentDto : list) {
      commentsPrepared.add(prepareCommentRSS(commentDto, comments, secure));
    }
    return commentsPrepared;
  }

  public List<PreparedComment> prepareCommentList(CommentList comments, List<CommentDto> list, boolean secure) throws UserNotFoundException {
    List<PreparedComment> commentsPrepared = new ArrayList<PreparedComment>(list.size());
    for (CommentDto commentDto : list) {
      commentsPrepared.add(prepareComment(commentDto, comments, secure));
    }
    return commentsPrepared;
  }

  public PreparedGroupInfo prepareGroupInfo(GroupDto groupDto, boolean secure) {
    String longInfo;

    if (groupDto.getLongInfo() != null) {
      longInfo = lorCodeService.parseComment(groupDto.getLongInfo(), secure);
    } else {
      longInfo = null;
    }

    return new PreparedGroupInfo(groupDto, longInfo);
  }

  public MessageMenu getMessageMenu(PreparedMessage message, UserDto currentUser) {
    boolean editable = currentUser != null && message.isEditable(currentUser);
    boolean resolvable;
    int memoriesId;

    if (currentUser != null) {
      resolvable = (currentUser.isModerator() || (message.getAuthor().getId() == currentUser.getId())) &&
          message.getGroupDto().isResolvable();

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
   *
   * @param messageDtos список топиков
   * @param secure   является ли соединение https
   * @return список подготовленных топиков
   */
  public List<PreparedMessage> prepareMessagesFeed(List<MessageDto> messageDtos, boolean secure) {
    List<PreparedMessage> pm = new ArrayList<PreparedMessage>(messageDtos.size());

    for (MessageDto messageDto : messageDtos) {
      PreparedMessage preparedMessage = prepareMessage(messageDto, messageDao.getTags(messageDto), true, null, secure);
      pm.add(preparedMessage);
    }

    return pm;
  }

  public List<PreparedEditInfo> build(MessageDto messageDto, boolean secure) throws UserNotFoundException, UserErrorException {
    List<EditInfoDTO> editInfoDTOs = messageDao.loadEditInfo(messageDto.getId());
    List<PreparedEditInfo> editInfos = new ArrayList<PreparedEditInfo>(editInfoDTOs.size());

    String currentMessage = messageDto.getMessage();
    String currentTitle = messageDto.getTitle();
    String currentUrl = messageDto.getUrl();
    String currentLinktext = messageDto.getLinktext();
    List<String> currentTags = tagCloudDao.getMessageTags(messageDto.getMessageId());

    for (int i = 0; i < editInfoDTOs.size(); i++) {
      EditInfoDTO dto = editInfoDTOs.get(i);

      editInfos.add(
          new PreparedEditInfo(
              lorCodeService,
              secure,
              userDao,
              dto,
              dto.getOldmessage() != null ? currentMessage : null,
              dto.getOldtitle() != null ? currentTitle : null,
              dto.getOldurl() != null ? currentUrl : null,
              dto.getOldlinktext() != null ? currentLinktext : null,
              dto.getOldtags() != null ? currentTags : null,
              i == 0,
              false
          )
      );

      if (dto.getOldmessage() != null) {
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

      if (dto.getOldtags() != null) {
        currentTags = TagCloudDao.parseTags(dto.getOldtags());
      }
    }

    if (!editInfoDTOs.isEmpty()) {
      EditInfoDTO current = EditInfoDTO.createFromMessage(tagCloudDao, messageDto);

      editInfos.add(new PreparedEditInfo(lorCodeService, secure, userDao, current, currentMessage, currentTitle, currentUrl, currentLinktext, currentTags, false, true));
    }

    return editInfos;
  }
}
