/*
 * Copyright 1998-2015 Linux.org.ru
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
import com.google.common.collect.ImmutableMap;
import org.elasticsearch.common.joda.time.DateTime;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.View;
import org.springframework.web.servlet.view.RedirectView;
import ru.org.linux.site.Template;
import ru.org.linux.user.User;
import ru.org.linux.user.UserErrorException;
import ru.org.linux.user.UserService;

import javax.servlet.http.HttpServletRequest;
import java.net.URLEncoder;
import java.util.*;
import java.util.stream.Collectors;

@Controller
public class TrackerController {
  @Autowired
  private TrackerDao trackerDao;

  @Autowired
  private UserService userService;

  @ModelAttribute("filters")
  public static List<TrackerFilterEnum> getFilter() {
    return Arrays.asList(TrackerFilterEnum.values());
  }

  @RequestMapping("/tracker.jsp")
  public View trackerOldUrl(
          @RequestParam(value="filter", defaultValue = "all") String filterAction,
          HttpServletRequest request
  ) throws Exception {
    Template tmpl = Template.getTemplate(request);

    TrackerFilterEnum defaultFilter = tmpl.getProf().getTrackerMode();

    RedirectView redirectView = new RedirectView("/tracker/");

    redirectView.setExposeModelAttributes(false);

    Optional<TrackerFilterEnum> filter = TrackerFilterEnum.getByValue(filterAction);
    if (filter.isPresent() && filter.get()!=defaultFilter) {
      redirectView.setUrl("/tracker/?filter="+ URLEncoder.encode(filterAction, "UTF-8"));
    }

    return redirectView;
  }

  private String makeTitle(TrackerFilterEnum filter, TrackerFilterEnum defaultFilter) {
    if (filter != defaultFilter) {
      return "Последние сообщения ("+filter.getLabel()+")";
    } else {
      return "Последние сообщения";
    }
  }

  @RequestMapping(value = "/tracker/json", produces = "application/json; charset=UTF-8", method = RequestMethod.GET)
  @ResponseBody
  public Map<String, Object> getMessage(
          @RequestParam(value = "filter", required = false) String filterAction,
          @RequestParam(value = "offset", required = false) Integer offset,
          HttpServletRequest request
  ) throws Exception {
    if (offset==null) {
      offset = 0;
    } else {
      if (offset<0 || offset>300) {
        throw new UserErrorException("Некорректное значение offset");
      }
    }

    Template tmpl = Template.getTemplate(request);
    TrackerFilterEnum defaultFilter = tmpl.getProf().getTrackerMode();
    TrackerFilterEnum trackerFilter = TrackerFilterEnum.getByValue(filterAction).or(defaultFilter);
    Date startDate = DateTime.now().minusDays(1).toDate();

    int messages = tmpl.getProf().getMessages();
    int topics = tmpl.getProf().getTopics();
    User user = tmpl.getCurrentUser();
    List<TrackerItem> trackerItems = trackerDao.getTrackAll(trackerFilter, user, startDate, topics, offset, messages);

    List<ImmutableMap> trackerProperties = trackerItems.stream().map(trackerItem -> ImmutableMap.builder()
            .put("id", trackerItem.getCid())
            .put("link", trackerItem.getUrl())
            .put("title", trackerItem.getTitle())
            .put("groupId", trackerItem.getGroupId())
            .put("groupUrl", trackerItem.getGroupUrl())
            .put("postdate", trackerItem.getPostdate())
            .put("lastmodified", trackerItem.getLastmod())
            .put("lastcommentedby", trackerItem.getAuthor().getNick())
            .put("pages", trackerItem.getPages())
            .put("tags", trackerItem.getTags())
            .put("author", trackerItem.getTopicAuthor().getNick())
            .build()).collect(Collectors.toList());

    return ImmutableMap.of(
            "trackerItems", trackerProperties
    );
  }

  @RequestMapping("/tracker")
  public ModelAndView tracker(
      @RequestParam(value="filter", required = false) String filterAction,
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

    Template tmpl = Template.getTemplate(request);

    TrackerFilterEnum defaultFilter = tmpl.getProf().getTrackerMode();

    TrackerFilterEnum trackerFilter = TrackerFilterEnum.getByValue(filterAction).or(defaultFilter);

    Map<String, Object> params = new HashMap<>();
    params.put("offset", offset);
    params.put("filter", trackerFilter.getValue());

    if (trackerFilter != defaultFilter) {
      params.put("addition_query", "&amp;filter=" + trackerFilter.getValue());
    } else {
      params.put("addition_query", "");
    }

    params.put("defaultFilter", defaultFilter);

    Date startDate = DateTime.now().minusDays(1).toDate();

    int messages = tmpl.getProf().getMessages();
    int topics = tmpl.getProf().getTopics();

    params.put("topics", topics);

    User user = tmpl.getCurrentUser();

    params.put("title", makeTitle(trackerFilter, defaultFilter));

    params.put("msgs", trackerDao.getTrackAll(trackerFilter, user, startDate, topics, offset, messages));

    if (tmpl.isModeratorSession()) {
      params.put("newUsers", userService.getNewUsers());
    }

    return new ModelAndView("tracker", params);
  }
}
