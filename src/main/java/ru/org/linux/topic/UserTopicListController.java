package ru.org.linux.topic;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.servlet.ModelAndView;
import ru.org.linux.section.Section;
import ru.org.linux.section.SectionService;
import ru.org.linux.site.Template;
import ru.org.linux.tag.TagService;
import ru.org.linux.user.*;
import ru.org.linux.util.URLUtil;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.List;

@Controller
@RequestMapping("/people/{nick}")
public class UserTopicListController {
  @Autowired
  private TopicListService topicListService;

  @Autowired
  private UserDao userDao;

  @Autowired
  private TagService tagService;

  @Autowired
  private SectionService sectionService;

  @Autowired
  private TopicPrepareService prepareService;

  @RequestMapping(value="favs", params="!output")
  public ModelAndView showUserFavs(
    HttpServletRequest request,
    TopicListRequest topicListForm,
    @PathVariable String nick,
    HttpServletResponse response
  ) throws Exception {
    TopicListController.setExpireHeaders(response, topicListForm.getYear(), topicListForm.getMonth());

    ModelAndView modelAndView = new ModelAndView();

    User user = getUserByNickname(modelAndView, nick);

    modelAndView.addObject("url", "/people/" + nick + "/favs");
    modelAndView.addObject("whoisLink", "/people/" + nick + '/' + "profile");

    modelAndView.addObject("ptitle", "Избранные сообщения " + user.getNick());
    modelAndView.addObject("navtitle", "Избранные сообщения " + user.getNick());

    topicListForm.setOffset(
      topicListService.fixOffset(topicListForm.getOffset())
    );
    modelAndView.addObject("offsetNavigation", true);
    modelAndView.addObject("topicListForm", topicListForm);

    List<Topic> messages = topicListService.getUserTopicsFeed(user, topicListForm.getOffset(), true, false);
    prepareTopicsForPlainOrRss(request, modelAndView, topicListForm, messages);

    modelAndView.setViewName("user-topics");

    return modelAndView;
  }

  @RequestMapping
  public ModelAndView showUserTopics(
    HttpServletRequest request,
    TopicListRequest topicListForm,
    @PathVariable String nick,
    HttpServletResponse response
  ) throws Exception {
    TopicListController.setExpireHeaders(response, topicListForm.getYear(), topicListForm.getMonth());

    ModelAndView modelAndView = new ModelAndView();

    Section section = null;
    if (topicListForm.getSection() != null && topicListForm.getSection() != 0) {
      section = sectionService.getSection(topicListForm.getSection());
    }

    User user = getUserByNickname(modelAndView, nick);

    UserInfo userInfo = userDao.getUserInfoClass(user);
    modelAndView.addObject("meLink", userInfo.getUrl());

    modelAndView.addObject("url", "/people/" + nick + '/');
    modelAndView.addObject("whoisLink", "/people/" + nick + '/' + "profile");
    // TODO: modelAndView.addObject("archiveLink", "/people/"+nick+"/archive/");


    modelAndView.addObject("ptitle", "Сообщения " + user.getNick());
    modelAndView.addObject("navtitle", "Сообщения " + user.getNick());

    modelAndView.addObject("rssLink", "/people/" + nick + "/?output=rss");

    topicListForm.setOffset(
      topicListService.fixOffset(topicListForm.getOffset())
    );
    modelAndView.addObject("offsetNavigation", true);
    modelAndView.addObject("topicListForm", topicListForm);

    List<Topic> messages = topicListService.getUserTopicsFeed(
      user,
      section,
      null,
      topicListForm.getOffset(),
      false,
      false
    );

    boolean rss = topicListForm.getOutput() != null && "rss".equals(topicListForm.getOutput());
    if (!rss) {
      if (section != null) {
        modelAndView.addObject("section", section);
      }
      modelAndView.addObject("sectionList", sectionService.getSectionList());
    }

    if (Integer.valueOf(0).equals(topicListForm.getSection())) {
      topicListForm.setSection(null);
    }
    URLUtil.QueryString queryString = new URLUtil.QueryString();
    queryString.add("section", topicListForm.getSection());
    modelAndView.addObject("params", queryString.toString());

    prepareTopicsForPlainOrRss(request, modelAndView, topicListForm, messages);

    if (!rss) {
      modelAndView.setViewName("user-topics");
    }

    return modelAndView;
  }

  @RequestMapping(value = "tracked", params="!output")
  public ModelAndView showUserWatches(
    HttpServletRequest request,
    TopicListRequest topicListForm,
    @PathVariable String nick,
    HttpServletResponse response
  ) throws Exception {
    TopicListController.setExpireHeaders(response, topicListForm.getYear(), topicListForm.getMonth());

    ModelAndView modelAndView = new ModelAndView();

    User user = getUserByNickname(modelAndView, nick);

    modelAndView.addObject("url", "/people/" + nick + "/tracked");
    modelAndView.addObject("whoisLink", "/people/" + nick + '/' + "profile");

    modelAndView.addObject("ptitle", "Отслеживаемые сообщения " + user.getNick());
    modelAndView.addObject("navtitle", "Отслеживаемые сообщения " + user.getNick());

    topicListForm.setOffset(
      topicListService.fixOffset(topicListForm.getOffset())
    );
    modelAndView.addObject("offsetNavigation", true);
    modelAndView.addObject("topicListForm", topicListForm);

    List<Topic> messages = topicListService.getUserTopicsFeed(user, topicListForm.getOffset(), true, true);
    prepareTopicsForPlainOrRss(request, modelAndView, topicListForm, messages);

    modelAndView.setViewName("user-topics");

    return modelAndView;
  }

  private void prepareTopicsForPlainOrRss(
    HttpServletRequest request,
    ModelAndView modelAndView,
    TopicListRequest topicListForm,
    List<Topic> messages
  ) {
    boolean rss = topicListForm.getOutput() != null && "rss".equals(topicListForm.getOutput());
    if (rss) {
      modelAndView.addObject(
        "messages",
        prepareService.prepareMessages(messages, request.isSecure())
      );
      modelAndView.setViewName("section-rss");
    } else {
      Template tmpl = Template.getTemplate(request);
      modelAndView.addObject(
        "messages",
        prepareService.prepareMessagesForUser(
                messages,
                request.isSecure(),
                tmpl.getCurrentUser(),
                tmpl.getProf(),
                false
        )
      );
    }
  }

  private User getUserByNickname(ModelAndView modelAndView, String nick)
    throws UserNotFoundException, UserErrorException {
    User user = userDao.getUser(nick);
    if (user.getId() == User.ANONYMOUS_ID) {
      throw new UserErrorException("Лента для пользователя anonymous не доступна");
    }
    modelAndView.addObject("user", user);
    return user;
  }

  @ExceptionHandler(UserNotFoundException.class)
  @ResponseStatus(HttpStatus.NOT_FOUND)
  public ModelAndView handleNotFoundException() {
    return new ModelAndView("errors/code404");
  }
}
