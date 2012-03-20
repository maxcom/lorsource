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

import com.google.common.base.Function;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Iterables;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.ModelAndView;
import ru.org.linux.site.Template;
import ru.org.linux.spring.dao.DeleteInfoDao;
import ru.org.linux.spring.dao.TrackerDao;
import ru.org.linux.spring.dao.TrackerDao.TrackerFilter;
import ru.org.linux.user.User;
import ru.org.linux.user.UserDao;
import ru.org.linux.user.UserErrorException;

import javax.servlet.http.HttpServletRequest;
import java.sql.Timestamp;
import java.util.*;

@Controller
public class TrackerController {
  @Autowired
  private TrackerDao trackerDao;

  @Autowired
  private UserDao userDao;

  @Autowired
  private DeleteInfoDao deleteInfoDao;

  private static final Set<String> filterValuesSet =
          ImmutableSet.copyOf(Iterables.transform(
                  Arrays.asList(TrackerFilter.values()),
                  new Function<TrackerFilter, String>() {
                    @Override
                    public String apply(TrackerFilter input) {
                      return input.getValue();
                    }
                  }));

  @ModelAttribute("filterItems")
  public static List<TrackerFilter> getFilter() {
    return Arrays.asList(TrackerFilter.values());
  }

  @RequestMapping("/tracker.jsp")
  public ModelAndView tracker(
      @ModelAttribute("tracker") TrackerFilterAction action,
      @RequestParam(value="offset", required = false) Integer offset,
      HttpServletRequest request) throws Exception {

    if (action.getFilter()==null) {
      action.setFilter(TrackerFilter.ALL.getValue());
    }

    String filter = action.getFilter();

    if (!filterValuesSet.contains(filter)) {
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

    Map<String, Object> params = new HashMap<String, Object>();
    params.put("mine", trackerFilter == TrackerFilter.MINE);
    params.put("offset", offset);
    params.put("filter", filter);
    params.put("tracker", new TrackerFilterAction(filter));
    /*params.put("filterItems", filterItems);*/
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

    params.put("topics", topics);
    if (filter!=null) {
      params.put("query", "&filter="+filter);
    } else {
      params.put("query", "");
    }

    User user = tmpl.getCurrentUser();

    if (trackerFilter == TrackerFilter.MINE) {
      if (!tmpl.isSessionAuthorized()) {
        throw new UserErrorException("Not authorized");
      }
    }
    params.put("msgs", trackerDao.getTrackAll(trackerFilter, user, dateLimit, topics, offset, messages));

    if (tmpl.isModeratorSession() && trackerFilter != TrackerFilter.MINE) {
      params.put("newUsers", userDao.getNewUsers());
      params.put("deleteStats", deleteInfoDao.getRecentStats());
    }

    return new ModelAndView("tracker", params);
  }

}
