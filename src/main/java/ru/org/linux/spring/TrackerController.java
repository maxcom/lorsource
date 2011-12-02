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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.ModelAndView;
import ru.org.linux.dto.UserDto;
import ru.org.linux.site.Template;
import ru.org.linux.site.UserErrorException;
import ru.org.linux.spring.dao.TrackerDao;
import ru.org.linux.spring.dao.UserDao;

import javax.servlet.http.HttpServletRequest;
import java.sql.Timestamp;
import java.util.*;

@Controller
public class TrackerController {

  private static final Log logger = LogFactory.getLog(TrackerController.class);

  @Autowired
  TrackerDao trackerDao;

  @Autowired
  UserDao userDao;

  private static final String[] filterValues = { "all", "notalks", "tech", "mine" };
  private static final Set<String> filterValuesSet = new HashSet<String>(Arrays.asList(filterValues));

  @ModelAttribute("filterItems")
  public static List<TrackerDao.TrackerFilter> getFilter() {
    return Arrays.asList(TrackerDao.TrackerFilter.values());
  }

  @RequestMapping("/tracker.jsp")
  public ModelAndView tracker(
      @ModelAttribute("tracker") TrackerFilterAction action,
      @RequestParam(value="offset", required = false) Integer offset,
      HttpServletRequest request) throws Exception {

    String filter = action.getFilter();

    if (filter!=null && !filterValuesSet.contains(filter)) {
      throw new UserErrorException("Некорректное значение filter");
    }

    if (offset==null) {
      offset = 0;
    } else {
      if (offset<0 || offset>300) {
        throw new UserErrorException("Некорректное значение offset");
      }
    }

    TrackerDao.TrackerFilter trackerFilter;
    if(filter != null) {
      if("notalks".equals(filter)) {
        trackerFilter = TrackerDao.TrackerFilter.NOTALKS;
      } else if ("tech".equals(filter)) {
        trackerFilter = TrackerDao.TrackerFilter.TECH;
      } else if ("mine".equals(filter)) {
        trackerFilter = TrackerDao.TrackerFilter.MINE;
      } else {
        trackerFilter = TrackerDao.TrackerFilter.ALL;
      }
    } else {
      trackerFilter = TrackerDao.TrackerFilter.ALL;
    }

    Map<String, Object> params = new HashMap<String, Object>();
    params.put("mine", trackerFilter == TrackerDao.TrackerFilter.MINE);
    params.put("offset", offset);
    params.put("filter", filter);
    params.put("tracker", new TrackerFilterAction(filter));
    /*params.put("filterItems", filterItems);*/
    Calendar calendar = Calendar.getInstance();
    calendar.setTime(new Date());
    Timestamp dateLimit;
    if(trackerFilter == TrackerDao.TrackerFilter.MINE) {
      calendar.add(Calendar.MONTH, -6);
    } else {
      calendar.add(Calendar.HOUR, -24);
    }
    dateLimit = new Timestamp(calendar.getTimeInMillis());

    Template tmpl = Template.getTemplate(request);
    int messages = tmpl.getProf().getMessages();
    int topics = tmpl.getProf().getTopics();

    params.put("topics", topics);
    if (filter!=null) {
      params.put("query", "&filter="+filter);
    } else {
      params.put("query", "");
    }

    UserDto user = tmpl.getCurrentUser();

    if (trackerFilter == TrackerDao.TrackerFilter.MINE) {
      if (!tmpl.isSessionAuthorized()) {
        throw new UserErrorException("Not authorized");
      }
    }
    params.put("msgs", trackerDao.getTrackAll(trackerFilter, user, dateLimit, topics, offset, messages));

    if (tmpl.isModeratorSession() && trackerFilter != TrackerDao.TrackerFilter.MINE) {
      params.put("newUsers", userDao.getNewUsers());
    }

    return new ModelAndView("tracker", params);
  }

}
