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

import com.google.common.collect.ImmutableMap;
import org.joda.time.DateTime;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import ru.org.linux.site.PublicApi;
import ru.org.linux.site.Template;
import ru.org.linux.user.User;

import javax.servlet.http.HttpServletRequest;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Controller
@PublicApi
public class TrackerApiController {
  @Autowired
  private TrackerDao trackerDao;

  @RequestMapping(value = "/api/tracker", produces = "application/json; charset=UTF-8", method = RequestMethod.GET)
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
        return ImmutableMap.of("error", "Некорректное значение offset");
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
            .put("url", trackerItem.getUrl())
            .put("title", trackerItem.getTitle())
            .put("groupTitle", trackerItem.getGroupTitle())
            .put("postDate", trackerItem.getPostdate())
            .put("lastModified", trackerItem.getLastmod())
            .put("lastCommentedBy", trackerItem.getAuthor().getNick())
            .put("pages", trackerItem.getPages())
            .put("tags", trackerItem.getTags())
            .put("author", trackerItem.getTopicAuthor().getNick())
            .build()).collect(Collectors.toList());

    return ImmutableMap.of(
            "trackerItems", trackerProperties
    );
  }
}
