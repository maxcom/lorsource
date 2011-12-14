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
package ru.org.linux.admin.ipmanage;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import ru.org.linux.comment.CommentDao;
import ru.org.linux.comment.DeleteCommentResult;
import ru.org.linux.search.SearchQueueSender;
import ru.org.linux.user.User;
import ru.org.linux.user.UserErrorException;

import java.sql.Timestamp;
import java.util.Calendar;
import java.util.Date;

@Service
public class DeleteMessagesByIpService {

  @Autowired
  private SearchQueueSender searchQueueSender;

  @Autowired
  private CommentDao commentDao;

  /**
   * @param ipAddress
   * @param moderator
   * @param timestamp
   * @param reason
   * @return
   */
  public DeleteCommentResult deleteMessages(String ipAddress, User moderator, Timestamp timestamp, String reason) {


    DeleteCommentResult deleteResult = commentDao.deleteCommentsByIPAddress(ipAddress, timestamp, moderator, reason);


    for (int topicId : deleteResult.getDeletedTopicIds()) {
      searchQueueSender.updateMessage(topicId, true);
    }

    searchQueueSender.updateComment(deleteResult.getDeletedCommentIds());
    return deleteResult;
  }

  /**
   * @param deletePeriodEnum
   * @return
   * @throws UserErrorException
   */
  public Timestamp calculateTimestamp(DeletePeriodEnum deletePeriodEnum)
    throws UserErrorException {
    Calendar calendar = Calendar.getInstance();
    calendar.setTime(new Date());

    calendar.add(deletePeriodEnum.getCalendarPeriod(), deletePeriodEnum.getCalendarNumPeriods());
    return new Timestamp(calendar.getTimeInMillis());
  }
}
