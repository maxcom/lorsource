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

package ru.org.linux.tracker;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import ru.org.linux.site.Template;
import ru.org.linux.user.User;
import ru.org.linux.user.UserErrorException;

import java.sql.Timestamp;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

/**
 * Сервис для tracker.
 */

@Service
public class TrackerService {

  @Autowired
  private TrackerDao trackerDao;

  public List<TrackerItem> get(Template template, Integer offset, TrackerFilterEnum trackerFilter) throws UserErrorException {
    int messages = template.getProf().getMessages();
    int topics = template.getProf().getTopics();


    Calendar calendar = Calendar.getInstance();
    calendar.setTime(new Date());
    if (trackerFilter == TrackerFilterEnum.MINE) {
      calendar.add(Calendar.MONTH, -6);
    } else {
      calendar.add(Calendar.HOUR, -24);
    }
    Timestamp dateLimit = new Timestamp(calendar.getTimeInMillis());

    User user = template.getCurrentUser();
    if (trackerFilter == TrackerFilterEnum.MINE) {
      if (!template.isSessionAuthorized()) {
        throw new UserErrorException("Not authorized");
      }
    }
    return trackerDao.getTrackAll(trackerFilter, user, dateLimit, topics, offset, messages);
  }
}
