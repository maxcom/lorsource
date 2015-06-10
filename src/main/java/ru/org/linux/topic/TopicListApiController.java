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

package ru.org.linux.topic;

import com.google.common.collect.ImmutableMap;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import ru.org.linux.auth.AuthUtil;
import ru.org.linux.group.Group;
import ru.org.linux.group.GroupDao;
import ru.org.linux.section.Section;
import ru.org.linux.section.SectionService;
import ru.org.linux.site.PublicApi;
import ru.org.linux.user.MemoriesDao;
import ru.org.linux.user.UserDao;

import java.sql.Date;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Controller
@PublicApi
public class TopicListApiController {
  @Autowired
  private SectionService sectionService;

  @Autowired
  private GroupDao groupDao;

  @Autowired
  private MemoriesDao memoriesDao;

  @Autowired
  private UserDao userDao;

  @Autowired
  private TopicListService topicListService;

  @Autowired
  private TopicTagService topicTagService;

  @RequestMapping(value = "/api/topics-simple", produces = "application/json; charset=UTF-8", method = RequestMethod.GET)
  @ResponseBody
  public Map<String, Object> getTopicsSimple(
          @RequestParam(value = "section") String sectionName,
          @RequestParam(value = "fromDate") String fromDate
  ) throws Exception {
    Section section = sectionService.getSectionByName(sectionName);

    if (!section.getUrlName().equals(sectionName) || !fromDate.matches("\\d{4}-\\d{2}-\\d{2}")) {
      return ImmutableMap.of("error", "Invalid arguments");
    }

    List<Topic> allTopics = topicListService.getAllTopicsFeed(section, Date.valueOf(fromDate));
    return ImmutableMap.of("topics", shortenTopicInfo(allTopics));
  }

  @RequestMapping(value = "/api/topics-complex", produces = "application/json; charset=UTF-8", method = RequestMethod.GET)
  @ResponseBody
  public Map<String, Object> getTopicsComplex(
          @RequestParam(value = "section") String sectionName,
          @RequestParam(value = "group") String groupName,
          @RequestParam(value = "fromDate") String fromDate,
          @RequestParam(value = "toDate") String toDate,
          @RequestParam(value = "offset") Integer offset,
          @RequestParam(value = "notalks") Boolean notalks,
          @RequestParam(value = "tech") Boolean tech
  ) throws Exception {
    Section section = sectionService.getSectionByName(sectionName);
    Group group = groupDao.getGroup(section, groupName);

    if (!section.getUrlName().equals(sectionName)
            ||!group.getUrlName().equals(groupName)
            || !fromDate.matches("\\d{4}-\\d{2}-\\d{2}")
            || !toDate.matches("\\d{4}-\\d{2}-\\d{2}")
            || offset < 0
            || notalks == null
            || tech == null) {
      return ImmutableMap.of("error", "Invalid arguments");
    }

    TopicListDto topicListDto = new TopicListDto();
    topicListDto.setSection(section.getId());
    topicListDto.setGroup(group.getId());
    topicListDto.setFromDate(Date.valueOf(fromDate));
    topicListDto.setToDate(Date.valueOf(toDate));
    topicListDto.setOffset(offset);
    topicListDto.setNotalks(notalks);
    topicListDto.setTech(tech);

    List<Topic> allTopics = topicListService.getTopics(topicListDto);

    return ImmutableMap.of("topics", shortenTopicInfo(allTopics));
  }

  private ImmutableMap<String, Object> shortenTopicInfo(List<Topic> allTopics) {
    List<ImmutableMap<String, Object>> listOfTopics = new ArrayList<>();

    for (Topic topic : allTopics) {
      ImmutableMap.Builder<String, Object> builder = new ImmutableMap.Builder<>();
      builder.put("url", topic.getLink())
              .put("title", topic.getTitle())
              .put("postDate", topic.getPostdate())
              .put("commentsCount", topic.getCommentCount())
              .put("favsCount", memoriesDao.getTopicInfo(topic.getId(), AuthUtil.getCurrentUser()).favsCount())
              .put("watchcount", memoriesDao.getTopicInfo(topic.getId(), AuthUtil.getCurrentUser()).watchCount())
              .put("tags", topicTagService.getTags(topic))
              .put("author", userDao.getUserCached(topic.getUid()).getNick());
      listOfTopics.add(builder.build());
    }
    return ImmutableMap.of("topics", listOfTopics);
  }
}
