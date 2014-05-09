/*
 * Copyright 1998-2014 Linux.org.ru
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

import com.google.common.base.Optional;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;
import org.elasticsearch.common.joda.time.DateTime;
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
import java.net.URLEncoder;
import java.util.*;

import static com.google.common.base.Predicates.equalTo;
import static com.google.common.base.Predicates.not;

@Controller
public class TrackerController {
  @Autowired
  private TrackerDao trackerDao;

  @Autowired
  private UserDao userDao;

  @Autowired
  private DeleteInfoDao deleteInfoDao;

  private static TrackerFilterEnum DEFAULT_FILTER = TrackerFilterEnum.ALL;

  @ModelAttribute("filters")
  public static List<TrackerFilterEnum> getFilter(HttpServletRequest request) {
    Template tmpl = Template.getTemplate(request);

    List<TrackerFilterEnum> filterValues = Arrays.asList(TrackerFilterEnum.values());

    if (tmpl.isSessionAuthorized()) {
      return filterValues;
    } else {
      return ImmutableList.copyOf(Iterables.filter(filterValues, not(equalTo(TrackerFilterEnum.MINE))));
    }
  }

  @RequestMapping("/tracker.jsp")
  public View trackerOldUrl(
          @RequestParam(value="filter", defaultValue = "all") String filterAction
  ) throws Exception {
    RedirectView redirectView = new RedirectView("/tracker/");

    redirectView.setExposeModelAttributes(false);

    Optional<TrackerFilterEnum> filter = TrackerFilterEnum.getByValue(filterAction);
    if (filter.isPresent() && filter.get()!=DEFAULT_FILTER) {
      redirectView.setUrl("/tracker/?filter="+ URLEncoder.encode(filterAction, "UTF-8"));
    }

    return redirectView;
  }

  private String makeTitle(TrackerFilterEnum filter) {
    if (filter != DEFAULT_FILTER) {
      return "Последние сообщения ("+filter.getLabel()+")";
    } else {
      return "Последние сообщения";
    }
  }

  @RequestMapping("/tracker")
  public ModelAndView tracker(
      @RequestParam(value="filter", defaultValue = "all") String filterAction,
      @RequestParam(value="offset", required = false) Integer offset,
      HttpServletRequest request
  ) throws Exception {
    if (offset==null) {
      offset = 0;
    } else {
      if (offset<0 || offset>300) {
        throw new UserErrorException("Некорректное значение offset");
      }
    }

    TrackerFilterEnum trackerFilter = TrackerFilterEnum.getByValue(filterAction).or(DEFAULT_FILTER);

    Map<String, Object> params = new HashMap<>();
    params.put("offset", offset);
    params.put("filter", trackerFilter.getValue());

    if (trackerFilter != DEFAULT_FILTER) {
      params.put("addition_query", "&amp;filter=" + trackerFilter.getValue());
    } else {
      params.put("addition_query", "");
    }

    params.put("defaultFilter", DEFAULT_FILTER);

    Date startDate;

    if (trackerFilter == TrackerFilterEnum.MINE) {
      startDate = DateTime.now().minusMonths(6).toDate();
    } else {
      startDate = DateTime.now().minusDays(1).toDate();
    }

    Template tmpl = Template.getTemplate(request);
    int messages = tmpl.getProf().getMessages();
    int topics = tmpl.getProf().getTopics();

    params.put("topics", topics);

    User user = tmpl.getCurrentUser();

    params.put("title", makeTitle(trackerFilter));

    if (trackerFilter == TrackerFilterEnum.MINE && !tmpl.isSessionAuthorized()) {
      throw new UserErrorException("Not authorized");
    }

    params.put("msgs", trackerDao.getTrackAll(trackerFilter, user, startDate, topics, offset, messages));

    if (tmpl.isModeratorSession() && trackerFilter != TrackerFilterEnum.MINE) {
      params.put("newUsers", userDao.getNewUsers());
      params.put("deleteStats", deleteInfoDao.getRecentStats());
    }

    return new ModelAndView("tracker", params);
  }
}
