package ru.org.linux.tag;

import com.google.common.collect.ImmutableList;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.servlet.ModelAndView;
import ru.org.linux.auth.AccessViolationException;
import ru.org.linux.section.Section;
import ru.org.linux.section.SectionService;
import ru.org.linux.site.Template;
import ru.org.linux.topic.PersonalizedPreparedTopic;
import ru.org.linux.topic.Topic;
import ru.org.linux.topic.TopicListService;
import ru.org.linux.topic.TopicPrepareService;

import javax.servlet.http.HttpServletRequest;
import java.util.List;

@Controller
@RequestMapping(value = "/tag/{tag}/bigpage")
public class TagPageController {
  @Autowired
  private TagService tagService;

  @Autowired
  private TopicPrepareService prepareService;

  @Autowired
  private TopicListService topicListService;

  @Autowired
  private SectionService sectionService;

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

    Section newsSection = sectionService.getSection(Section.SECTION_NEWS);

    List<Topic> newsTopics = topicListService.getTopicsFeed(
            newsSection,
            null,
            tag,
            0,
            null,
            null,
            21
    );

    List<Topic> fullNewsTopics = newsTopics.isEmpty() ? ImmutableList.<Topic>of() : newsTopics.subList(0, 1);
    List<Topic> briefNewsTopics = newsTopics.size() <= 1 ? ImmutableList.<Topic>of() : newsTopics.subList(1, newsTopics.size());

    List<PersonalizedPreparedTopic> fullNews = prepareService.prepareMessagesForUser(
            fullNewsTopics,
            request.isSecure(),
            tmpl.getCurrentUser(),
            tmpl.getProf(),
            false
    );

    mv.addObject("fullNews", fullNews);
    mv.addObject("briefNews", briefNewsTopics);

    return mv;
  }
}
