package ru.org.linux.topic;

import org.apache.commons.lang.WordUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.View;
import org.springframework.web.servlet.view.RedirectView;
import org.springframework.web.util.UriComponentsBuilder;
import org.springframework.web.util.UriTemplate;
import ru.org.linux.section.Section;
import ru.org.linux.section.SectionService;
import ru.org.linux.site.Template;
import ru.org.linux.tag.TagService;
import ru.org.linux.user.UserTagService;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.List;

@Controller
public class TagTopicListController {
  private static final UriTemplate TAG_URI_TEMPLATE = new UriTemplate("/tag/{tag}");
  private static final UriTemplate TAGS_URI_TEMPLATE = new UriTemplate("/tags/{tag}");
  @Autowired
  private UserTagService userTagService;

  @Autowired
  private SectionService sectionService;

  @Autowired
  private TagService tagService;

  @Autowired
  private TopicListService topicListService;

  @Autowired
  private TopicPrepareService prepareService;

  public static String tagListUrl(String tag) {
    return TAG_URI_TEMPLATE.expand(tag).toString();
  }

  public static String tagsUrl(char letter) {
    return TAGS_URI_TEMPLATE.expand(letter).toString();
  }

  private String getTitle(@Nonnull String tag, @Nullable Section section) {
    if (section==null) {
      return WordUtils.capitalize(tag);
    } else {
      return WordUtils.capitalize(tag) + " (" + section.getName() + ")";
    }
  }

  @RequestMapping(value = "/tag/{tag}", method = {RequestMethod.GET, RequestMethod.HEAD})
  public ModelAndView tagFeed(
    HttpServletRequest request,
    HttpServletResponse response,
    @PathVariable String tag,
    @RequestParam(value = "offset", defaultValue = "0") int offset,
    @RequestParam(value = "section", defaultValue = "0") int sectionId
  ) throws Exception {
    ModelAndView modelAndView = new ModelAndView("tag-topics");

    Section section;

    if (sectionId!=0) {
      section = sectionService.getSection(sectionId);
      modelAndView.addObject("section", section);
    } else {
      section = null;
    }

    tagService.checkTag(tag);

    TopicListController.setExpireHeaders(response, null, null);

    String title = getTitle(tag, section);

    modelAndView.addObject("navtitle", title);
    modelAndView.addObject("ptitle", title);

    offset = topicListService.fixOffset(offset);

    List<Topic> topics = topicListService.getTopicsFeed(
            section,
            null,
            tag,
            offset,
            null,
            null
    );

    Template tmpl = Template.getTemplate(request);

    List<PersonalizedPreparedTopic> preparedTopics = prepareService.prepareMessagesForUser(
            topics,
            request.isSecure(),
            tmpl.getCurrentUser(),
            tmpl.getProf(),
            false
    );

    modelAndView.addObject(
            "messages",
            preparedTopics
    );

    modelAndView.addObject("offsetNavigation", true);

    modelAndView.addObject("tag", tag);
    modelAndView.addObject("section", sectionId);
    modelAndView.addObject("offset", offset);
    modelAndView.addObject("sectionList", sectionService.getSectionList());

    if (tmpl.isSessionAuthorized()) {
      modelAndView.addObject(
              "isShowFavoriteTagButton",
              !userTagService.hasFavoriteTag(tmpl.getCurrentUser(), tag)
      );

      modelAndView.addObject(
              "isShowUnFavoriteTagButton",
              userTagService.hasFavoriteTag(tmpl.getCurrentUser(), tag)
      );

      if (!tmpl.isModeratorSession()) {
        modelAndView.addObject(
                "isShowIgnoreTagButton",
                !userTagService.hasIgnoreTag(tmpl.getCurrentUser(), tag)
        );
        modelAndView.addObject(
                "isShowUnIgnoreTagButton",
                userTagService.hasIgnoreTag(tmpl.getCurrentUser(), tag)
        );
      }
    }

    modelAndView.addObject("counter", tagService.getCounter(tag));

    modelAndView.addObject("url", tagListUrl(tag));
    modelAndView.addObject("favsCount", userTagService.countFavs(tagService.getTagId(tag)));

    if (offset<200 && preparedTopics.size()==20) {
      modelAndView.addObject("nextLink", buildTagUri(tag, sectionId, offset + 20));
    }

    if (offset>=20) {
      modelAndView.addObject("prevLink", buildTagUri(tag, sectionId, offset - 20));
    }

    return modelAndView;
  }

  private String buildTagUri(String tag, int section, int offset) {
    UriComponentsBuilder builder = UriComponentsBuilder.fromUri(TAG_URI_TEMPLATE.expand(tag));

    if (section!=0) {
      builder.queryParam("section", section);
    }

    if (offset!=0) {
      builder.queryParam("offset", offset);
    }

    return builder.build().toUriString();
  }

  @RequestMapping(value = "/view-news.jsp", method = {RequestMethod.GET, RequestMethod.HEAD}, params = {"tag"})
  public View tagFeedOld(
          @RequestParam String tag
  ) {
    return new RedirectView(TagTopicListController.tagListUrl(tag));
  }
}
