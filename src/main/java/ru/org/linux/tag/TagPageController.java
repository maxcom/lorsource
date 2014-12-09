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

package ru.org.linux.tag;

import com.google.common.base.Function;
import com.google.common.collect.*;
import org.apache.commons.lang3.text.WordUtils;
import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.servlet.ModelAndView;
import ru.org.linux.gallery.ImageDao;
import ru.org.linux.gallery.PreparedGalleryItem;
import ru.org.linux.group.Group;
import ru.org.linux.group.GroupDao;
import ru.org.linux.section.Section;
import ru.org.linux.section.SectionService;
import ru.org.linux.site.DateFormats;
import ru.org.linux.site.Template;
import ru.org.linux.topic.*;
import ru.org.linux.user.UserTagService;

import javax.annotation.Nonnull;
import javax.servlet.http.HttpServletRequest;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import static ru.org.linux.util.ListUtil.headOrEmpty;
import static ru.org.linux.util.ListUtil.tailOrEmpty;

@Controller
@RequestMapping(value="/tag/{tag}", params = "!section")
public class TagPageController {
  public static final int TOTAL_NEWS_COUNT = 21;
  public static final int FORUM_TOPIC_COUNT = 20;

  private static final DateTimeFormatter THIS_YEAR_FORMAT = DateTimeFormat.forPattern("MMMMMMMM YYYY").withLocale(DateFormats.RUSSIAN_LOCALE);
  private static final DateTimeFormatter OLD_YEAR_FORMAT = DateTimeFormat.forPattern("YYYY");

  public static final int GALLERY_COUNT = 3;

  @Autowired
  private TagService tagService;

  @Autowired
  private TopicPrepareService prepareService;

  @Autowired
  private TopicListService topicListService;

  @Autowired
  private SectionService sectionService;

  @Autowired
  private GroupDao groupDao;

  @Autowired
  private UserTagService userTagService;

  @Autowired
  private ImageDao imageDao;
  private final Function<Topic,ForumItem> forumPrepareFunction = new Function<Topic, ForumItem>() {
    @Override
    public ForumItem apply(Topic input) {
      return new ForumItem(input, groupDao.getGroup(input.getGroupId()));
    }
  };

  @RequestMapping(method = {RequestMethod.GET, RequestMethod.HEAD})
  public ModelAndView tagPage(
          HttpServletRequest request,
          @PathVariable String tag
  ) throws Exception {
    Template tmpl = Template.getTemplate(request);

    if (!TagName.isGoodTag(tag)) {
      throw new TagNotFoundException();
    }

    ModelAndView mv = new ModelAndView("tag-page");

    mv.addObject("tag", tag);
    mv.addObject("title", WordUtils.capitalize(tag));

    TagInfo tagInfo = tagService.getTagInfo(tag, true);

    mv.addObject("counter", tagInfo.topicCount());

    if (tmpl.isSessionAuthorized()) {
      mv.addObject(
              "showFavoriteTagButton",
              !userTagService.hasFavoriteTag(tmpl.getCurrentUser(), tag)
      );

      mv.addObject(
              "showUnFavoriteTagButton",
              userTagService.hasFavoriteTag(tmpl.getCurrentUser(), tag)
      );
    }

    int tagId = tagInfo.id();

    mv.addObject("favsCount", userTagService.countFavs(tagId));

    List<TagRef> relatedTags = tagService.getRelatedTags(tagId);

    if (relatedTags.size()>1) {
      mv.addObject("relatedTags", relatedTags);
    }

    mv.addAllObjects(getNewsSection(request, tag));
    mv.addAllObjects(getGallerySection(tag, tagId, tmpl));
    mv.addAllObjects(getForumSection(tag, tagId));

    return mv;
  }

  private Map<String, Object> getNewsSection(HttpServletRequest request, String tag) throws TagNotFoundException {
    Template tmpl = Template.getTemplate(request);

    Section newsSection = sectionService.getSection(Section.SECTION_NEWS);

    List<Topic> newsTopics = topicListService.getTopicsFeed(
            newsSection,
            null,
            tag,
            0,
            null,
            null,
            TOTAL_NEWS_COUNT
    );

    List<Topic> fullNewsTopics = headOrEmpty(newsTopics);
    List<Topic> briefNewsTopics = tailOrEmpty(newsTopics);

    List<PersonalizedPreparedTopic> fullNews = prepareService.prepareMessagesForUser(
            fullNewsTopics,
            request.isSecure(),
            tmpl.getCurrentUser(),
            tmpl.getProf(),
            false
    );

    ImmutableListMultimap<String, Topic> briefNews = datePartition(briefNewsTopics, input -> new DateTime(input.getCommitDate()));

    ImmutableMap.Builder<String, Object> out = ImmutableMap.builder();

    out.put("addNews", AddTopicController.getAddUrl(newsSection, tag));

    if (newsTopics.size()==TOTAL_NEWS_COUNT) {
      out.put("moreNews", TagTopicListController.tagListUrl(tag, newsSection));
    }

    out.put("fullNews", fullNews);
    out.put("briefNews", split(briefNews));

    return out.build();
  }

  private Map<String, Object> getGallerySection(String tag, int tagId, Template tmpl) throws TagNotFoundException {
    List<PreparedGalleryItem> list = imageDao.prepare(imageDao.getGalleryItems(GALLERY_COUNT, tagId));

    ImmutableMap.Builder<String, Object> out = ImmutableMap.builder();
    Section section = sectionService.getSection(Section.SECTION_GALLERY);

    if (tmpl.isSessionAuthorized()) {
      out.put("addGallery", AddTopicController.getAddUrl(section, tag));
    }

    if (list.size()==GALLERY_COUNT) {
      out.put("moreGallery", TagTopicListController.tagListUrl(tag, section));
    }

    out.put("gallery", list);

    return out.build();
  }

  private ImmutableMap<String, Object> getForumSection(@Nonnull String tag, int tagId) {
    Section forumSection = sectionService.getSection(Section.SECTION_FORUM);

    TopicListDto topicListDto = new TopicListDto();

    topicListDto.setSection(forumSection.getId());
    topicListDto.setCommitMode(TopicListDao.CommitMode.POSTMODERATED_ONLY);

    topicListDto.setTag(tagId);

    topicListDto.setLimit(FORUM_TOPIC_COUNT);
    topicListDto.setLastmodSort(true);

    List<Topic> forumTopics = topicListService.getTopics(topicListDto);

    ImmutableListMultimap<String, Topic> sections = datePartition(forumTopics, input -> new DateTime(input.getLastModified()));

    ImmutableMap.Builder<String, Object> out = ImmutableMap.builder();

    if (forumTopics.size()==FORUM_TOPIC_COUNT) {
      out.put("moreForum", TagTopicListController.tagListUrl(tag, forumSection));
    }

    out.put("addForum", AddTopicController.getAddUrl(forumSection, tag));

    out.put("forum", split(Multimaps.transformValues(sections, forumPrepareFunction)));

    return out.build();
  }

  private static ImmutableListMultimap<String, Topic> datePartition(
          Iterable<Topic> topics,
          final Function<Topic, DateTime> dateExtractor
  ) {
    final DateTime startOfToday = DateTime.now().withTimeAtStartOfDay();
    final DateTime startOfYesterday = DateTime.now().minusDays(1).withTimeAtStartOfDay();
    final DateTime startOfYear = DateTime.now().withDayOfYear(1).withTimeAtStartOfDay();

    return Multimaps.index(topics, (input) -> {
      DateTime date = dateExtractor.apply(input);

      if (date.isAfter(startOfToday)) {
        return "Сегодня";
      } else if (date.isAfter(startOfYesterday)) {
        return "Вчера";
      } else if (date.isAfter(startOfYear)) {
        return THIS_YEAR_FORMAT.print(date);
      } else {
        return OLD_YEAR_FORMAT.print(date);
      }
    });
  }

  private static <T> ImmutableList<ImmutableMap<String, List<T>>> split(ListMultimap<String, T> topics) {
    if (topics.isEmpty()) {
      return ImmutableList.of();
    }

    int split = topics.size() / 2 + (topics.size() % 2);

    ImmutableMap.Builder<String, List<T>> first = ImmutableMap.builder();
    ImmutableMap.Builder<String, List<T>> second = ImmutableMap.builder();

    int total = 0;

    for (Map.Entry<String, Collection<T>> entry : topics.asMap().entrySet()) {
      int currentSize = entry.getValue().size();

      if (total+(currentSize/2)<=split) {
        first.put(entry.getKey(), ImmutableList.copyOf(entry.getValue()));
      } else {
        second.put(entry.getKey(), ImmutableList.copyOf(entry.getValue()));
      }

      total += currentSize;
    }

    return ImmutableList.of(first.build(), second.build());
  }

  public static class ForumItem {
    private final Topic topic;
    private final Group group;

    public ForumItem(Topic topic, Group group) {
      this.topic = topic;
      this.group = group;
    }

    public Topic getTopic() {
      return topic;
    }

    public Group getGroup() {
      return group;
    }
  }
}
