/*
 * Copyright 1998-2011 Linux.org.ru
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

package ru.org.linux.admin.ipmanage;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import ru.org.linux.message.MessageNotFoundException;
import ru.org.linux.user.User;
import ru.org.linux.user.UserDao;
import ru.org.linux.user.UserNotFoundException;

import java.util.ArrayList;
import java.util.List;

@Service
public class SameIpService {

  @Autowired
  SameIpDao sameIpDao;

  @Autowired
  private IPBlockDao ipBlockDao;

  @Autowired
  private UserDao userDao;


  /**
   * Получить список пользователей, заходивших с указанного IP-адреса.
   *
   * @param ipAddress IP-адрес
   * @param uaId      - идентификатор строки UserAgent
   * @return Список пользователей
   */
  public List<SameIp.UserItem> getUsers(String ipAddress, final int uaId) {
    List<SameIpDto.UserItem> userItemsDto = sameIpDao.getUsers(ipAddress);
    List<SameIp.UserItem> userItems = new ArrayList<SameIp.UserItem>();

    for (SameIpDto.UserItem userItemDto : userItemsDto) {
      userItems.add(new SameIp.UserItem(userItemDto, uaId));
    }
    return userItems;
  }

  /**
   * Получить список тем или комментариев, написанных с указанного IP-адреса.
   *
   * @param ipAddress IP-адрес
   * @param isComment true если необходимо получить комментарии; false если необходимо получить темы
   * @return список тем или комментариев
   */
  public List<SameIp.TopicItem> getForumMessages(String ipAddress, boolean isComment) {
    List<SameIpDto.TopicItem> topicItemsDto;

    topicItemsDto = isComment
      ? sameIpDao.getComments(ipAddress)
      : sameIpDao.getTopics(ipAddress);

    List<SameIp.TopicItem> topicItems = new ArrayList<SameIp.TopicItem>();

    for (SameIpDto.TopicItem topicItemDto : topicItemsDto) {
      topicItems.add(new SameIp.TopicItem(topicItemDto, isComment));
    }
    return topicItems;
  }

  /**
   * Получение информации о блокировке указанного адреса.
   *
   * @param ipAddress IP-адрес
   * @return объект, содержащий информацию блокировке IP-адреса
   */
  public SameIp.BlockInfo getBlockInfo(String ipAddress) {
    SameIp.BlockInfo blockInfo = new SameIp.BlockInfo();

    IPBlockInfo ipBlockInfo = ipBlockDao.getBlockInfo(ipAddress);
    blockInfo.setBlocked(ipBlockInfo != null);

    if (ipBlockInfo != null) {
      blockInfo.setBanDate(ipBlockInfo.getBanDate());
      blockInfo.setOriginalDate(ipBlockInfo.getOriginalDate());
      blockInfo.setReason(ipBlockInfo.getReason());
      blockInfo.setBlockExpired(!ipBlockInfo.isBlocked());
      User moderator;
      try {
        moderator = userDao.getUserCached(ipBlockInfo.getModerator());
        blockInfo.setModeratorNick(moderator.getNick());
      } catch (UserNotFoundException ignored) {
        blockInfo.setModeratorNick("Модератор не найден");
      }
    }
    return blockInfo;
  }

  /**
   * получить информацию об IP-адресе и об идентификаторе строки  userAgent по идентификатору сообщения.
   *
   * @param msgId идентификатор сообщения
   * @return объект, содержащий информацию об IP-адресе и об идентификаторе строки userAgent
   * @throws MessageNotFoundException если сообщение не найдено.
   */
  public SameIp.IpInfo getIpInfo(Integer msgId)
    throws MessageNotFoundException {
    return sameIpDao.getIpInfo(msgId);
  }
}
