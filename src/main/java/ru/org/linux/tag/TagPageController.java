package ru.org.linux.tag;

import com.google.common.collect.ImmutableMap;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.servlet.ModelAndView;
import ru.org.linux.auth.AccessViolationException;
import ru.org.linux.gallery.ImageDao;
import ru.org.linux.gallery.PreparedGalleryItem;
import ru.org.linux.section.Section;
import ru.org.linux.section.SectionService;
import ru.org.linux.site.Template;
import ru.org.linux.topic.PersonalizedPreparedTopic;
import ru.org.linux.topic.Topic;
import ru.org.linux.topic.TopicListService;
import ru.org.linux.topic.TopicPrepareService;

import javax.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.Map;

import static ru.org.linux.util.ListUtil.*;

@Controller
@RequestMapping("/tag/{tag}/bigpage")
public class TagPageController {
  public static final int TOTAL_NEWS_COUNT = 21;
  public static final int FORUM_TOPIC_COUNT = 20;
  @Autowired
  private TagService tagService;

  @Autowired
  private TopicPrepareService prepareService;

  @Autowired
  private TopicListService topicListService;

  @Autowired
  private SectionService sectionService;

  @Autowired
  private ImageDao imageDao;

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

    mv.addAllObjects(getNewsSection(request, tag));
    mv.addAllObjects(getGallerySection(tag));
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

  private Map<String, Object> getGallerySection(String tag) throws TagNotFoundException {
    int tagId = tagService.getTagId(tag);

    List<PreparedGalleryItem> list = imageDao.prepare(imageDao.getGalleryItems(3, tagId));

    return ImmutableMap.<String, Object>of(
            "gallery", list
    );
  }

  private Map<String, List<Topic>> getForumSection(String tag) throws TagNotFoundException {
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

    return ImmutableMap.of(
            "forum1", firstHalf(forumTopics),
            "forum2", secondHalf(forumTopics)
    );
  }
}
