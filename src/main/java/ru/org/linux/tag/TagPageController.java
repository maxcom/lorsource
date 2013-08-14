package ru.org.linux.tag;

import com.google.common.base.Function;
import com.google.common.collect.ImmutableListMultimap;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Multimaps;
import org.joda.time.DateMidnight;
import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.servlet.ModelAndView;
import ru.org.linux.auth.AccessViolationException;
import ru.org.linux.gallery.ImageDao;
import ru.org.linux.gallery.PreparedGalleryItem;
import ru.org.linux.group.Group;
import ru.org.linux.group.GroupDao;
import ru.org.linux.section.Section;
import ru.org.linux.section.SectionService;
import ru.org.linux.site.Template;
import ru.org.linux.topic.PersonalizedPreparedTopic;
import ru.org.linux.topic.Topic;
import ru.org.linux.topic.TopicListService;
import ru.org.linux.topic.TopicPrepareService;
import ru.org.linux.user.UserTagService;

import javax.servlet.http.HttpServletRequest;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import static ru.org.linux.util.ListUtil.*;

@Controller
@RequestMapping("/tag/{tag}/bigpage")
public class TagPageController {
  public static final int TOTAL_NEWS_COUNT = 21;
  public static final int FORUM_TOPIC_COUNT = 20;

  private static final DateTimeFormatter THIS_YEAR_FORMAT = DateTimeFormat.forPattern("MMMMMMMM YYYY");
  private static final DateTimeFormatter OLD_YEAR_FORMAT = DateTimeFormat.forPattern("YYYY");
  public static final Function<Topic,DateTime> LASTMOD_EXTRACTOR = new Function<Topic, DateTime>() {
    @Override
    public DateTime apply(Topic input) {
      return new DateTime(input.getLastModified());
    }
  };

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

    if (!tmpl.isModeratorSession()) {
      throw new AccessViolationException("Forbidden");
    }

    tagService.checkTag(tag);

    ModelAndView mv = new ModelAndView("tag-page");

    mv.addObject("tag", tag);

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

    int tagId = tagService.getTagId(tag);

    mv.addObject("favsCount", userTagService.countFavs(tagId));

    mv.addAllObjects(getNewsSection(request, tag));
    mv.addAllObjects(getGallerySection(tagId));
    mv.addAllObjects(getForumSection(tag));

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

    return ImmutableMap.<String, Object>of(
            "fullNews", fullNews,
            "briefNews1", firstHalf(briefNewsTopics),
            "briefNews2", secondHalf(briefNewsTopics)
    );
  }

  private Map<String, Object> getGallerySection(int tagId) throws TagNotFoundException {
    List<PreparedGalleryItem> list = imageDao.prepare(imageDao.getGalleryItems(3, tagId));

    return ImmutableMap.<String, Object>of(
            "gallery", list
    );
  }

  private Map<String, Map<String, Collection<ForumItem>>> getForumSection(String tag) throws TagNotFoundException {
    Section forumSection = sectionService.getSection(Section.SECTION_FORUM);

    List<Topic> forumTopics = topicListService.getTopicsFeed(
            forumSection,
            null,
            tag,
            0,
            null,
            null,
            FORUM_TOPIC_COUNT
    );

    ImmutableListMultimap<String, Topic> sections = datePartition(forumTopics, LASTMOD_EXTRACTOR);

    return ImmutableMap.of(
            "forum", Multimaps.transformValues(sections, forumPrepareFunction).asMap()
    );
  }

  private static ImmutableListMultimap<String, Topic> datePartition(
          Iterable<Topic> topics,
          final Function<Topic, DateTime> dateExtractor
  ) {
    final DateMidnight startOfToday = new DateMidnight();
    final DateMidnight startOfYesterday = startOfToday.minusDays(1);
    final DateMidnight startOfYear = startOfToday.withDayOfYear(1);

    return Multimaps.index(topics, new Function<Topic, String>() {
      @Override
      public String apply(Topic input) {
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
      }
    });
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
