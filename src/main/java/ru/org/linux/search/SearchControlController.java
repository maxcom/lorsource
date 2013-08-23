/*
 * Copyright 1998-2013 Linux.org.ru
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

package ru.org.linux.search;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Required;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.servlet.ModelAndView;
import ru.org.linux.topic.TopicDao;

import javax.servlet.ServletRequest;
import java.sql.Timestamp;
import java.util.Calendar;

@Controller
public class SearchControlController {
  private SearchQueueSender searchQueueSender;
  private TopicDao messageDao;

  @Autowired
  @Required
  public void setSearchQueueSender(SearchQueueSender searchQueueSender) {
    this.searchQueueSender = searchQueueSender;
  }

  @Autowired
  public void setMessageDao(TopicDao messageDao) {
    this.messageDao = messageDao;
  }

  @RequestMapping(value="/admin/search-reindex", method=RequestMethod.POST, params = "action=all")
  @PreAuthorize("hasRole('ROLE_ADMIN')")
  public ModelAndView reindexAll(ServletRequest request) throws Exception {

    Timestamp startDate = messageDao.getTimeFirstTopic();

    Calendar start = Calendar.getInstance();
    start.setTime(startDate);

    start.set(Calendar.DAY_OF_MONTH, 1);
    start.set(Calendar.HOUR, 0);
    start.set(Calendar.MINUTE, 0);

    for  (Calendar i = Calendar.getInstance(); i.after(start); i.add(Calendar.MONTH, -1)) {
      searchQueueSender.updateMonth(i.get(Calendar.YEAR), i.get(Calendar.MONTH)+1);
    }

    searchQueueSender.updateMonth(1970, 1);

    return new ModelAndView("action-done", "message", "Scheduled reindex");
  }

  @RequestMapping(value="/admin/search-reindex", method=RequestMethod.POST, params = "action=current")
  @PreAuthorize("hasRole('ROLE_ADMIN')")
  public ModelAndView reindexCurrentMonth(ServletRequest request) throws Exception {
    Calendar current = Calendar.getInstance();

    for (int i=0; i<3; i++) {
      searchQueueSender.updateMonth(current.get(Calendar.YEAR), current.get(Calendar.MONTH)+1);
      current.add(Calendar.MONTH, -1);
    }

    return new ModelAndView("action-done", "message", "Scheduled reindex last 3 month");
  }

  @RequestMapping(value="/admin/search-reindex", method=RequestMethod.GET)
  @PreAuthorize("hasRole('ROLE_ADMIN')")
  public ModelAndView reindexAll()  {
    return new ModelAndView("search-reindex");
  }
}
