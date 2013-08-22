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

package ru.org.linux.tracker;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.View;
import org.springframework.web.servlet.view.RedirectView;
import ru.org.linux.site.Template;
import ru.org.linux.spring.dao.DeleteInfoDao;
import ru.org.linux.user.User;
import ru.org.linux.user.UserDao;
import ru.org.linux.user.UserErrorException;

import javax.servlet.http.HttpServletRequest;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
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

  private static final Set<String> filterValues;

  static {
    filterValues = new HashSet<>();
    for (TrackerFilterEnum eventFilter : TrackerFilterEnum.values()) {
      filterValues.add(eventFilter.getValue());
    }
  }

  @ModelAttribute("filters")
  public static List<TrackerFilterEnum> getFilter(HttpServletRequest request) {
    Template tmpl = Template.getTemplate(request);
    if(tmpl.isSessionAuthorized()) {
      return Arrays.asList(TrackerFilterEnum.values());
    } else {
      List<TrackerFilterEnum> trackerFilters = new ArrayList<>();
      for(TrackerFilterEnum trackerFilter : TrackerFilterEnum.values()) {
        if("mine".equals(trackerFilter.getValue())) {
          continue;
        }
        trackerFilters.add(trackerFilter);
      }
      return trackerFilters;
    }
  }

  @RequestMapping("/tracker.jsp")
  public View trackerOldUrl(
          @RequestParam(value="filter", defaultValue = "all") String filterAction
  ) throws UnsupportedEncodingException {
    RedirectView redirectView = new RedirectView("/tracker/");

    redirectView.setExposeModelAttributes(false);

    if (filterValues.contains(filterAction) && !filterAction.equals("all")) {
      redirectView.setUrl("/tracker/?filter="+ URLEncoder.encode(filterAction, "UTF-8"));
    }

    return redirectView;
  }

  private TrackerFilterEnum getFilterValue(String filterAction) {
    if(filterValues.contains(filterAction)) {
      return TrackerFilterEnum.valueOf(filterAction.toUpperCase());
    } else {
      return TrackerFilterEnum.ALL;
    }
  }

  @RequestMapping("/tracker")
  public ModelAndView tracker(
      @RequestParam(value="filter", defaultValue = "all") String filterAction,
      @RequestParam(value="offset", required = false) Integer offset,
      HttpServletRequest request) throws Exception {

    if (offset==null) {
      offset = 0;
    } else {
      if (offset<0 || offset>300) {
        throw new UserErrorException("Некорректное значение offset");
      }
    }

    TrackerFilterEnum trackerFilter = getFilterValue(filterAction);

    Map<String, Object> params = new HashMap<>();
    params.put("mine", trackerFilter == TrackerFilterEnum.MINE);
    params.put("offset", offset);
    params.put("filter", trackerFilter.getValue());

    if(trackerFilter != TrackerFilterEnum.ALL) {
      params.put("addition_query", "&amp;filter=" + trackerFilter.getValue());
    } else {
      params.put("addition_query", "");
    }

    Calendar calendar = Calendar.getInstance();
    calendar.setTime(new Date());
    if(trackerFilter == TrackerFilterEnum.MINE) {
      calendar.add(Calendar.MONTH, -6);
    } else {
      calendar.add(Calendar.HOUR, -24);
    }
    Timestamp dateLimit = new Timestamp(calendar.getTimeInMillis());

    Template tmpl = Template.getTemplate(request);
    int messages = tmpl.getProf().getMessages();
    int topics = tmpl.getProf().getTopics();

    params.put("topics", topics);

    User user = tmpl.getCurrentUser();

    if (trackerFilter == TrackerFilterEnum.MINE) {
      if (!tmpl.isSessionAuthorized()) {
        throw new UserErrorException("Not authorized");
      }
      params.put("title", "Последние сообщения (мои темы)");
    } else {
      params.put("title", "Последние сообщения");
    }
    params.put("msgs", trackerDao.getTrackAll(trackerFilter, user, dateLimit, topics, offset, messages));

    if (tmpl.isModeratorSession() && trackerFilter != TrackerFilterEnum.MINE) {
      params.put("newUsers", userDao.getNewUsers());
      params.put("deleteStats", deleteInfoDao.getRecentStats());
    }

    return new ModelAndView("tracker", params);
  }

}
