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

package ru.org.linux.user;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import ru.org.linux.comment.CommentPrepareService;
import ru.org.linux.site.MessageNotFoundException;
import ru.org.linux.topic.Topic;
import ru.org.linux.topic.TopicDao;
import ru.org.linux.util.bbcode.LorCodeService;

import java.util.ArrayList;
import java.util.List;

@Service
public class UserEventPrepareService {
  @Autowired
  private LorCodeService lorCodeService;
  
  @Autowired
  private TopicDao messageDao;
  
  @Autowired
  private UserDao userDao;
  
  @Autowired
  private CommentPrepareService commentPrepareService;

  /**
   * 
   * @param events список событий
   * @param readMessage возвращать ли отрендеренное содержимое уведомлений (используется только для RSS)
   * @param secure является ли текущие соединение https
   * @return
   */
  public List<PreparedUserEvent> prepare(List<UserEvent> events, boolean readMessage, boolean secure) {
    List<PreparedUserEvent> prepared = new ArrayList<PreparedUserEvent>(events.size());

    for (UserEvent event : events) {
      String messageText;
      if (readMessage) {
        if(event.isComment()) { // Комментарий
          messageText = commentPrepareService.prepareCommentTextRSS(event.getCid(), secure);
        } else { // Топик
          try {
            Topic message = messageDao.getById(event.getMsgid());
            messageText = lorCodeService.prepareTextRSS(message.getMessage(), secure, message.isLorcode());
          } catch (MessageNotFoundException e) {
            messageText = "";
          }
        }
      } else {
        messageText = null;
      }
      
      User commentAuthor;
      
      if (event.isComment()) {
        try {
          commentAuthor = userDao.getUserCached(event.getCommentAuthor());
        } catch (UserNotFoundException e) {
          throw new RuntimeException(e);
        }
      } else {
        commentAuthor = null;
      }
      
      prepared.add(new PreparedUserEvent(event, messageText, commentAuthor));
    }

    return prepared;
  }
}
