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

package ru.org.linux.topic;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import ru.org.linux.group.BadGroupException;
import ru.org.linux.group.Group;
import ru.org.linux.group.GroupDao;
import ru.org.linux.poll.PollNotFoundException;
import ru.org.linux.poll.PollPrepareService;
import ru.org.linux.poll.PreparedPoll;
import ru.org.linux.section.Section;
import ru.org.linux.section.SectionDao;
import ru.org.linux.section.SectionNotFoundException;
import ru.org.linux.site.DeleteInfo;
import ru.org.linux.user.MemoriesDao;
import ru.org.linux.user.User;
import ru.org.linux.user.UserDao;
import ru.org.linux.user.UserNotFoundException;
import ru.org.linux.spring.Configuration;
import ru.org.linux.spring.dao.*;
import ru.org.linux.util.bbcode.LorCodeService;

import java.util.ArrayList;
import java.util.List;

@Service
public class TopicPrepareService {
  @Autowired
  private TopicDao messageDao;

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

  public PreparedTopic prepareMessage(Topic message, boolean minimizeCut, boolean secure) {
    return prepareMessage(message, messageDao.getTags(message), minimizeCut, null, secure);
  }

  public PreparedTopic prepareMessage(Topic message, List<String> tags, PreparedPoll newPoll, boolean secure) {
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
  private PreparedTopic prepareMessage(Topic message, List<String> tags, boolean minimizeCut, PreparedPoll poll, boolean secure) {
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

      List<EditInfoDto> editInfo = messageDao.getEditInfo(message.getId());
      EditInfoDto lastEditInfo;
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

      return new PreparedTopic(message, author, deleteInfo, deleteUser, processedMessage, preparedPoll, commiter, tags, group, section, lastEditInfo, lastEditor, editCount, userAgent);
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
  public List<PreparedTopic> prepareMessagesFeed(List<Topic> messages, boolean secure) {
    List<PreparedTopic> pm = new ArrayList<PreparedTopic>(messages.size());

    for (Topic message : messages) {
      PreparedTopic preparedMessage = prepareMessage(message, messageDao.getTags(message), true, null, secure);
      pm.add(preparedMessage);
    }

    return pm;
  }

  public TopicMenu getMessageMenu(PreparedTopic message, User currentUser) {
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

    return new TopicMenu(editable, resolvable, memoriesId);
  }
}
