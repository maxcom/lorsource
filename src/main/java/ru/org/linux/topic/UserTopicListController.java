/*
 * Copyright 1998-2013 Linux.org.ru
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

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.util.UriComponentsBuilder;
import ru.org.linux.auth.AccessViolationException;
import ru.org.linux.section.Section;
import ru.org.linux.section.SectionService;
import ru.org.linux.site.Template;
import ru.org.linux.user.*;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.net.URLEncoder;
import java.util.List;

@Controller
@RequestMapping("/people/{nick}")
public class UserTopicListController {
  @Autowired
  private TopicListService topicListService;

  @Autowired
  private UserDao userDao;

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

    modelAndView.addObject("url",
        UriComponentsBuilder.fromUriString("/people/{nick}/favs").buildAndExpand(nick).encode().toUriString());
    modelAndView.addObject("whoisLink",
        UriComponentsBuilder.fromUriString("/people/{nick}/profile").buildAndExpand(nick).encode().toUriString());

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

  @RequestMapping(value="drafts")
  public ModelAndView showUserDrafts(
          HttpServletRequest request,
          TopicListRequest topicListForm,
          @PathVariable String nick,
          HttpServletResponse response
  ) throws Exception {
    Template tmpl = Template.getTemplate(request);

    TopicListController.setExpireHeaders(response, topicListForm.getYear(), topicListForm.getMonth());

    ModelAndView modelAndView = new ModelAndView();

    User user = getUserByNickname(modelAndView, nick);

    if (!tmpl.isModeratorSession() && !user.equals(tmpl.getCurrentUser())) {
      throw new AccessViolationException("Вы не можете смотреть черновики другого пользователя");
    }

    modelAndView.addObject("url",
            UriComponentsBuilder.fromUriString("/people/{nick}/drafts").buildAndExpand(nick).encode().toUriString());
    modelAndView.addObject("whoisLink",
            UriComponentsBuilder.fromUriString("/people/{nick}/profile").buildAndExpand(nick).encode().toUriString());

    modelAndView.addObject("ptitle", "Черновики " + user.getNick());
    modelAndView.addObject("navtitle", "Черновики " + user.getNick());

    topicListForm.setOffset(
            topicListService.fixOffset(topicListForm.getOffset())
    );
    modelAndView.addObject("offsetNavigation", true);
    modelAndView.addObject("topicListForm", topicListForm);

    List<Topic> messages = topicListService.getDrafts(user, topicListForm.getOffset());
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

    modelAndView.addObject("url",
        UriComponentsBuilder.fromUriString("/people/{nick}/").buildAndExpand(nick).encode().toUriString());
    modelAndView.addObject("whoisLink",
        UriComponentsBuilder.fromUriString("/people/{nick}/profile").buildAndExpand(nick).encode().toUriString());
    // TODO: modelAndView.addObject("archiveLink", "/people/"+nick+"/archive/");


    modelAndView.addObject("ptitle", "Сообщения " + user.getNick());
    modelAndView.addObject("navtitle", "Сообщения " + user.getNick());

    modelAndView.addObject("rssLink",
        UriComponentsBuilder.fromUriString("/people/{nick}/?output=rss").buildAndExpand(nick).encode().toUriString());

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
    modelAndView.addObject("params", topicListForm.getSection() == null ? "" : URLEncoder.encode("section=" + topicListForm.getSection(), "UTF-8"));

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

    modelAndView.addObject("url",
        UriComponentsBuilder.fromUriString("/people/{nick}/tracked").buildAndExpand(nick).encode().toUriString());
    modelAndView.addObject("whoisLink",
        UriComponentsBuilder.fromUriString("/people/{nick}/profile").buildAndExpand(nick).encode().toUriString());

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
