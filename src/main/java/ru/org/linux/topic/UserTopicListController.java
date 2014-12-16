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

package ru.org.linux.topic;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.util.UriComponentsBuilder;
import ru.org.linux.auth.AccessViolationException;
import ru.org.linux.section.Section;
import ru.org.linux.section.SectionService;
import ru.org.linux.site.Template;
import ru.org.linux.user.*;

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
  private SectionService sectionService;

  @Autowired
  private TopicPrepareService prepareService;

  @Autowired
  private TopicPermissionService topicPermissionService;

  @RequestMapping(value="favs", params="!output")
  public ModelAndView showUserFavs(
    HttpServletRequest request,
    TopicListRequest topicListForm,
    @PathVariable String nick,
    HttpServletResponse response,
    @RequestParam(value = "offset", defaultValue = "0") int offset
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

    offset = topicListService.fixOffset(offset);
    modelAndView.addObject("offset", offset);

    modelAndView.addObject("topicListForm", topicListForm);

    List<Topic> messages = topicListService.getUserTopicsFeed(user, offset, true, false);
    boolean rss = topicListForm.getOutput() != null && "rss".equals(topicListForm.getOutput());

    prepareTopicsForPlainOrRss(request, modelAndView, rss, messages);

    modelAndView.setViewName("user-topics");

    return modelAndView;
  }

  @RequestMapping(value="drafts")
  public ModelAndView showUserDrafts(
          HttpServletRequest request,
          TopicListRequest topicListForm,
          @PathVariable String nick,
          HttpServletResponse response,
          @RequestParam(value = "offset", defaultValue = "0") int offset
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

    offset = topicListService.fixOffset(offset);
    modelAndView.addObject("offset", offset);

    modelAndView.addObject("topicListForm", topicListForm);

    List<Topic> messages = topicListService.getDrafts(user, offset);
    boolean rss = topicListForm.getOutput() != null && "rss".equals(topicListForm.getOutput());

    prepareTopicsForPlainOrRss(request, modelAndView, rss, messages);

    modelAndView.setViewName("user-topics");

    return modelAndView;
  }

  @RequestMapping
  public ModelAndView showUserTopics(
    HttpServletRequest request,
    @PathVariable String nick,
    HttpServletResponse response,
    @RequestParam(value = "offset", defaultValue = "0") int offset,
    @RequestParam(value = "section", defaultValue = "0") int sectionId,
    @RequestParam(value = "output", required = false) String output
  ) throws Exception {
    TopicListController.setExpireHeaders(response, null, null);

    ModelAndView modelAndView = new ModelAndView();

    Section section = null;
    if (sectionId != 0) {
      section = sectionService.getSection(sectionId);
    }

    User user = getUserByNickname(modelAndView, nick);

    UserInfo userInfo = userDao.getUserInfoClass(user);

    if (topicPermissionService.followAuthorLinks(user)) {
      modelAndView.addObject("meLink", userInfo.getUrl());
    }

    modelAndView.addObject("nick", user.getNick());

    modelAndView.addObject("url",
        UriComponentsBuilder.fromUriString("/people/{nick}/").buildAndExpand(nick).encode().toUriString());
    modelAndView.addObject("whoisLink",
        UriComponentsBuilder.fromUriString("/people/{nick}/profile").buildAndExpand(nick).encode().toUriString());
    // TODO: modelAndView.addObject("archiveLink", "/people/"+nick+"/archive/");


    modelAndView.addObject("ptitle", "Сообщения " + user.getNick());
    modelAndView.addObject("navtitle", "Сообщения " + user.getNick());

    modelAndView.addObject("rssLink",
        UriComponentsBuilder.fromUriString("/people/{nick}/?output=rss").buildAndExpand(nick).encode().toUriString());

    offset = topicListService.fixOffset(offset);

    modelAndView.addObject("offset", offset);

    List<Topic> messages = topicListService.getUserTopicsFeed(
      user,
      section,
      null,
      offset,
      false,
      false
    );

    boolean rss = "rss".equals(output);
    if (!rss) {
      if (section != null) {
        modelAndView.addObject("section", section);
      }
      modelAndView.addObject("sectionList", sectionService.getSectionList());
    }

    modelAndView.addObject("params", section == null ? "" : ("section=" + sectionId));

    prepareTopicsForPlainOrRss(request, modelAndView, rss, messages);

    if (!rss) {
      modelAndView.setViewName("user-topics");
    }

    modelAndView.addObject("showSearch", true);

    return modelAndView;
  }

  @RequestMapping(value = "tracked", params="!output")
  public ModelAndView showUserWatches(
    HttpServletRequest request,
    TopicListRequest topicListForm,
    @PathVariable String nick,
    HttpServletResponse response,
    @RequestParam(value = "offset", defaultValue = "0") int offset
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

    offset = topicListService.fixOffset(offset);
    modelAndView.addObject("offset", offset);
    modelAndView.addObject("topicListForm", topicListForm);

    List<Topic> messages = topicListService.getUserTopicsFeed(user, offset, true, true);
    boolean rss = topicListForm.getOutput() != null && "rss".equals(topicListForm.getOutput());

    prepareTopicsForPlainOrRss(request, modelAndView, rss, messages);

    modelAndView.setViewName("user-topics");

    return modelAndView;
  }

  private void prepareTopicsForPlainOrRss(
    HttpServletRequest request,
    ModelAndView modelAndView,
    boolean rss,
    List<Topic> messages
  ) {
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
