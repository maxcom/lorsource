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

package ru.org.linux.spring;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.ModelAndView;
import ru.org.linux.ApplicationController;
import ru.org.linux.site.Template;
import ru.org.linux.spring.dao.DeleteInfoDao;
import ru.org.linux.spring.dao.TrackerDao.TrackerFilter;
import ru.org.linux.user.User;
import ru.org.linux.user.UserDao;
import ru.org.linux.user.UserErrorException;
import ru.org.linux.spring.dao.TrackerDao;

import javax.servlet.http.HttpServletRequest;
import java.sql.Timestamp;
import java.util.*;

@Controller
public class TrackerController extends ApplicationController {
  @Autowired
  private TrackerDao trackerDao;

  @Autowired
  private UserDao userDao;

  @Autowired
  private DeleteInfoDao deleteInfoDao;

  private static final String[] filterValues = { "all", "notalks", "tech", "mine" };
  private static final Set<String> filterValuesSet = new HashSet<String>(Arrays.asList(filterValues));

  @ModelAttribute("filterItems")
  public static List<TrackerFilter> getFilter() {
    return Arrays.asList(TrackerFilter.values());
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

    TrackerFilter trackerFilter;
    if(filter != null) {
      if("notalks".equals(filter)) {
        trackerFilter = TrackerFilter.NOTALKS;
      } else if ("tech".equals(filter)) {
        trackerFilter = TrackerFilter.TECH;
      } else if ("mine".equals(filter)) {
        trackerFilter = TrackerFilter.MINE;
      } else {
        trackerFilter = TrackerFilter.ALL;
      }
    } else {
      trackerFilter = TrackerFilter.ALL;
    }

    ModelAndView modelAndView = new ModelAndView("tracker");

    modelAndView.addObject("mine", trackerFilter == TrackerFilter.MINE);
    modelAndView.addObject("offset", offset);
    modelAndView.addObject("filter", filter);
    modelAndView.addObject("tracker", new TrackerFilterAction(filter));
    /*modelAndView.addObject("filterItems", filterItems);*/
    Calendar calendar = Calendar.getInstance();
    calendar.setTime(new Date());
    if(trackerFilter == TrackerFilter.MINE) {
      calendar.add(Calendar.MONTH, -6);
    } else {
      calendar.add(Calendar.HOUR, -24);
    }
    Timestamp dateLimit = new Timestamp(calendar.getTimeInMillis());

    Template tmpl = Template.getTemplate(request);
    int messages = tmpl.getProf().getMessages();
    int topics = tmpl.getProf().getTopics();

    modelAndView.addObject("topics", topics);
    if (filter!=null) {
      modelAndView.addObject("query", "&filter="+filter);
    } else {
      modelAndView.addObject("query", "");
    }

    User user = tmpl.getCurrentUser();

    if (trackerFilter == TrackerFilter.MINE) {
      if (!tmpl.isSessionAuthorized()) {
        throw new UserErrorException("Not authorized");
      }
    }
    modelAndView.addObject("msgs", trackerDao.getTrackAll(trackerFilter, user, dateLimit, topics, offset, messages));

    if (tmpl.isModeratorSession() && trackerFilter != TrackerFilter.MINE) {
      modelAndView.addObject("newUsers", userDao.getNewUsers());
      modelAndView.addObject("deleteStats", deleteInfoDao.getRecentStats());
    }

    return render(modelAndView);
  }

}
