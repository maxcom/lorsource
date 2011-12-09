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

package ru.org.linux.site;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import ru.org.linux.poll.PollNotFoundException;
import ru.org.linux.poll.PollPrepareService;
import ru.org.linux.poll.PreparedPoll;
import ru.org.linux.spring.Configuration;
import ru.org.linux.spring.dao.*;
import ru.org.linux.util.bbcode.LorCodeService;

import java.util.ArrayList;
import java.util.List;

@Service
public class MessagePrepareService {
  @Autowired
  private MessageDao messageDao;

  @Autowired
  private GroupDao groupDao;

  @Autowired
  private UserDao userDao;

  @Autowired
  private SectionDao sectionDao;

  @Autowired
  private DeleteInfoDao deleteInfoDao;

  @Autowired
  private PollPrepareService pollPrepareService;

  @Autowired
  private LorCodeService lorCodeService;

  @Autowired
  private Configuration configuration;

  @Autowired
  private UserAgentDao userAgentDao;

  @Autowired
  private MemoriesDao memoriesDao;

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
          preparedPoll = pollPrepareService.preparePoll(message);
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

  public MessageMenu getMessageMenu(PreparedMessage message, User currentUser) {
    boolean editable = currentUser!=null && message.isEditable(currentUser);
    boolean resolvable;
    int memoriesId;

    if (currentUser!=null) {
      resolvable = (currentUser.isModerator() || (message.getAuthor().getId()==currentUser.getId())) &&
            message.getGroup().isResolvable();

      memoriesId = memoriesDao.getId(currentUser, message.getMessage());
    } else {
      resolvable = false;
      memoriesId = 0;
    }

    return new MessageMenu(editable, resolvable, memoriesId);
  }
}
