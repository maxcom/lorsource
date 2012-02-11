/*
 * Copyright 1998-2012 Linux.org.ru
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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.View;
import org.springframework.web.servlet.view.RedirectView;
import ru.org.linux.group.Group;
import ru.org.linux.group.GroupDao;
import ru.org.linux.section.Section;
import ru.org.linux.section.SectionNotFoundException;
import ru.org.linux.section.SectionService;
import ru.org.linux.site.ScriptErrorException;
import ru.org.linux.site.Template;
import ru.org.linux.user.*;
import ru.org.linux.util.*;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.*;

@Controller
public class TopicListController {
  private static final Log logger = LogFactory.getLog(TopicListController.class);

  @Autowired
  private SectionService sectionService;

  @Autowired
  private TopicListService topicListService;

  @Autowired
  private TopicPrepareService prepareService;

  // TODO: здесь должен быть сервис, а не DAO
  @Autowired
  private GroupDao groupDao;

  // TODO: здесь должен быть сервис, а не DAO
  @Autowired
  private UserDao userDao;


  /**
   * @param request
   * @param topicListForm
   * @param response
   * @return
   * @throws Exception
   */
  @RequestMapping(value = "/view-news.jsp", method = {RequestMethod.GET, RequestMethod.HEAD})
  public ModelAndView mainTopicsFeedHandler(
    HttpServletRequest request,
    TopicListRequest topicListForm,
    HttpServletResponse response
  ) throws Exception {

    Section section = null;
    if (topicListForm.getSection() != null) {
      section = sectionService.getSection(topicListForm.getSection());
    }

    Group group = null;
    if (topicListForm.getGroup() != null) {
      group = groupDao.getGroup(topicListForm.getGroup());
    }
    checkRequestConditions(section, group, topicListForm);

    ModelAndView modelAndView = new ModelAndView("view-news");

    URLUtil.QueryString queryString = new URLUtil.QueryString();
    queryString.add("topicListForm", topicListForm);
    modelAndView.addObject("params", queryString.toString());

    modelAndView.addObject("url", "view-news.jsp");
    if (section != null) {
      modelAndView.addObject("section", section);
      modelAndView.addObject("archiveLink", section.getArchiveLink());
    }

    setExpireHeaders(response, topicListForm);

    modelAndView.addObject("ptitle", calculatePTitle(section, group, topicListForm));
    modelAndView.addObject("navtitle", calculateNavTitle(section, group, topicListForm));

    topicListForm.setOffset(
      topicListService.fixOffset(topicListForm.getOffset())
    );

    List<Topic> messages = topicListService.getTopicsFeed(
      section,
      group,
      topicListForm.getTag(),
      topicListForm.getOffset(),
      topicListForm.getYear(),
      topicListForm.getMonth()
    );

    Template tmpl = Template.getTemplate(request);
    modelAndView.addObject(
      "messages",
      prepareService.prepareMessagesForUser(messages, request.isSecure(), tmpl.getCurrentUser())
    );

    modelAndView.addObject("offsetNavigation", topicListForm.getMonth()== null);

    if (section != null) {
      String rssLink = "/section-rss.jsp?section=" + section.getId();
      if (group != null) {
        rssLink += "&group=" + group.getId();
      }

      modelAndView.addObject("rssLink", rssLink);
    }

    return modelAndView;
  }

  /**
   * @param request
   * @param topicListForm
   * @param response
   * @return
   * @throws Exception
   */
  @RequestMapping("/gallery/")
  public ModelAndView gallery(
    HttpServletRequest request,
    TopicListRequest topicListForm,
    HttpServletResponse response
  ) throws Exception {

    topicListForm.setSection(Section.SECTION_GALLERY);
    ModelAndView modelAndView =
      mainTopicsFeedHandler(request, topicListForm, response);

    modelAndView.addObject("url", "/gallery/");
    modelAndView.addObject("params", null);

    return modelAndView;
  }

  /**
   * @param request
   * @param topicListForm
   * @param response
   * @return
   * @throws Exception
   */
  @RequestMapping("/forum/lenta")
  public ModelAndView forum(
    HttpServletRequest request,
    TopicListRequest topicListForm,
    HttpServletResponse response
  ) throws Exception {

    topicListForm.setSection(Section.SECTION_FORUM);
    ModelAndView modelAndView =
      mainTopicsFeedHandler(request, topicListForm, response);

    modelAndView.addObject("url", "/forum/lenta");
    modelAndView.addObject("params", null);

    return modelAndView;
  }

  /**
   * @param request
   * @param topicListForm
   * @param response
   * @return
   * @throws Exception
   */
  @RequestMapping("/polls/")
  public ModelAndView polls(
    HttpServletRequest request,
    TopicListRequest topicListForm,
    HttpServletResponse response
  ) throws Exception {
    topicListForm.setSection(Section.SECTION_POLLS);
    ModelAndView modelAndView =
      mainTopicsFeedHandler(request, topicListForm, response);

    modelAndView.addObject("url", "/polls/");
    modelAndView.addObject("params", null);

    return modelAndView;
  }

  /**
   * @param request
   * @param topicListForm
   * @param response
   * @return
   * @throws Exception
   */
  @RequestMapping("/news/")
  public ModelAndView news(
    HttpServletRequest request,
    TopicListRequest topicListForm,
    HttpServletResponse response
  ) throws Exception {
    topicListForm.setSection(Section.SECTION_NEWS);
    ModelAndView modelAndView =
      mainTopicsFeedHandler(request, topicListForm, response);

    modelAndView.addObject("url", "/news/");
    modelAndView.addObject("params", null);

    return modelAndView;
  }

  /**
   * @param request
   * @param topicListForm
   * @param groupName
   * @param response
   * @return
   * @throws Exception
   */
  @RequestMapping("/gallery/{group}")
  public ModelAndView galleryGroup(
    HttpServletRequest request,
    TopicListRequest topicListForm,
    @PathVariable("group") String groupName,
    HttpServletResponse response
  ) throws Exception {
    topicListForm.setSection(Section.SECTION_GALLERY);
    return group(request, topicListForm, groupName, response);
  }

  /**
   * @param request
   * @param topicListForm
   * @param groupName
   * @param response
   * @return
   * @throws Exception
   */
  @RequestMapping("/news/{group}")
  public ModelAndView newsGroup(
    HttpServletRequest request,
    TopicListRequest topicListForm,
    @PathVariable("group") String groupName,
    HttpServletResponse response
  ) throws Exception {
    topicListForm.setSection(Section.SECTION_NEWS);
    return group(request, topicListForm, groupName, response);
  }

  /**
   * @param request
   * @param topicListForm
   * @param groupName
   * @param response
   * @return
   * @throws Exception
   */
  @RequestMapping("/polls/{group}")
  public ModelAndView pollsGroup(
    HttpServletRequest request,
    TopicListRequest topicListForm,
    @PathVariable("group") String groupName,
    HttpServletResponse response
  ) throws Exception {
    topicListForm.setSection(Section.SECTION_POLLS);
    return group(request, topicListForm, groupName, response);
  }

  /**
   * @param section
   * @param year
   * @param month
   * @param request
   * @param response
   * @return
   * @throws Exception
   */
  @RequestMapping("/{section}/archive/{year}/{month}")
  public ModelAndView galleryArchive(
    HttpServletRequest request,
    TopicListRequest topicListForm,
    @PathVariable String section,
    @PathVariable int year,
    @PathVariable int month,
    HttpServletResponse response
  ) throws Exception {
    topicListForm.setSection(Section.getSection(section));
    topicListForm.setYear(year);
    topicListForm.setMonth(month);

    ModelAndView modelAndView = mainTopicsFeedHandler( request, topicListForm, response);

    modelAndView.addObject("url", "/gallery/archive/" + year + '/' + month + '/');
    modelAndView.addObject("params", null);

    return modelAndView;
  }

  /**
   * @param nick
   * @param request
   * @param response
   * @return
   * @throws Exception
   */
  @RequestMapping("/people/{nick}")
  public ModelAndView showUserTopicsNew(
    HttpServletRequest request,
    TopicListRequest topicListForm,
    @PathVariable String nick,
    HttpServletResponse response
  ) throws Exception {


    setExpireHeaders(response, topicListForm);

    ModelAndView modelAndView = new ModelAndView();

    Section section = null;
    Group group = null;
    if (topicListForm.getSection() != null && topicListForm.getSection().intValue() != 0) {
      section = sectionService.getSection(topicListForm.getSection());

      if (topicListForm.getGroup() != null && topicListForm.getGroup().intValue() != 0) {
        group = groupDao.getGroup(topicListForm.getGroup());
      }
    }

    if (topicListForm.getTag() != null) {
      TagDao.checkTag(topicListForm.getTag());
    }

    User user = getUserByNickname(modelAndView, nick);

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
      group,
      topicListForm.getOffset(),
      false
    );

    boolean rss = topicListForm.getOutput() != null && "rss".equals(topicListForm.getOutput());
    if (!rss) {
      if (section != null) {
        modelAndView.addObject("section", section);
        modelAndView.addObject("group", group);
        List <Group> groupList = groupDao.getGroups(section);
        if (groupList.size() == 1) {
          groupList = null;
        }
        modelAndView.addObject("groupList", groupList);
      }
      modelAndView.addObject("sectionList", sectionService.getSectionList());
    }

    if ("0".equals(topicListForm.getSection())) {
      topicListForm.setSection(null);
    }
    if ("0".equals(topicListForm.getGroup())) {
      topicListForm.setGroup(null);
    }
    URLUtil.QueryString queryString = new URLUtil.QueryString();
    queryString.add("section", topicListForm.getSection());
    queryString.add("group", topicListForm.getGroup());
    queryString.add("tag", topicListForm.getTag());
    modelAndView.addObject("params", queryString.toString());

    prepareTopicsForPlainOrRss(request, modelAndView, topicListForm, messages);
    return modelAndView;
  }

  /**
   * @param nick
   * @param topicListForm
   * @param request
   * @param response
   * @return
   * @throws Exception
   */
  @RequestMapping("/people/{nick}/favs")
  public ModelAndView showUserFavs(
    HttpServletRequest request,
    TopicListRequest topicListForm,
    @PathVariable String nick,
    HttpServletResponse response
  ) throws Exception {
    setExpireHeaders(response, topicListForm);

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

    List<Topic> messages = topicListService.getUserTopicsFeed(user, topicListForm.getOffset(), true);
    prepareTopicsForPlainOrRss(request, modelAndView, topicListForm, messages);
    return modelAndView;
  }

  /**
   * @param nick
   * @param output
   * @return
   */
  @RequestMapping(value = "/show-topics.jsp", method = RequestMethod.GET)
  public View showUserTopics(
    @RequestParam("nick") String nick,
    @RequestParam(value = "output", required = false) String output
  ) {
    if (output != null) {
      return new RedirectView("/people/" + nick + "/?output=rss");
    }

    return new RedirectView("/people/" + nick + '/');
  }

  /**
   * @param sectionId
   * @param request
   * @return
   * @throws Exception
   */
  @RequestMapping(value = "/view-all.jsp", method = {RequestMethod.GET, RequestMethod.HEAD})
  public ModelAndView viewAll(
    @RequestParam(value = "section", required = false, defaultValue = "0") int sectionId,
    HttpServletRequest request
  ) throws Exception {
    Template tmpl = Template.getTemplate(request);

    ModelAndView modelAndView = new ModelAndView("view-all");

    Section section = null;
    if (sectionId != 0) {
      section = sectionService.getSection(sectionId);
      modelAndView.addObject("section", section);
    }

    Calendar calendar = Calendar.getInstance();
    calendar.setTime(new Date());
    calendar.add(Calendar.MONTH, -1);

    List<Topic> messages = topicListService.getAllTopicsFeed(section, calendar.getTime());
    modelAndView.addObject(
      "messages",
      prepareService.prepareMessagesForUser(messages, request.isSecure(), tmpl.getCurrentUser())
    );

    List<TopicListDto.DeletedTopic> deleted = topicListService.getDeletedTopicsFeed(sectionId);

    modelAndView.addObject("deletedTopics", deleted);
    modelAndView.addObject("sections", sectionService.getSectionList());

    return modelAndView;
  }

  /**
   * @param section
   * @param offset
   * @param month
   * @param year
   * @param groupId
   * @return
   * @throws Exception
   * @deprecated
   */
  @Deprecated
  @RequestMapping(value = "/view-news.jsp", params = {"section"})
  public View oldLink(
    @RequestParam int section,
    @RequestParam(required = false) Integer offset,
    @RequestParam(value = "month", required = false) Integer month,
    @RequestParam(value = "year", required = false) Integer year,
    @RequestParam(value = "group", required = false) Integer groupId
  ) throws Exception {
    if (offset != null) {
      return new RedirectView(Section.getNewsViewerLink(section) + "?offset=" + Integer.toString(offset));
    }

    if (year != null && month != null) {
      return new RedirectView(Section.getArchiveLink(section) + Integer.toString(year) + '/' + Integer.toString(month));
    }

    if (groupId != null) {
      Group group = groupDao.getGroup(groupId);

      return new RedirectView(Section.getNewsViewerLink(section) + group.getUrlName() + '/');
    }

    return new RedirectView(Section.getNewsViewerLink(section));
  }

  @RequestMapping("/section-rss.jsp")
  public ModelAndView showRSS(
    HttpServletRequest request,
    TopicListRequest topicListForm
  ) throws Exception {

    final String[] filterValues = {"all", "notalks", "tech"};
    final Set<String> filterValuesSet = new HashSet<String>(Arrays.asList(filterValues));

    if (topicListForm.getFilter() != null && !filterValuesSet.contains(topicListForm.getFilter())) {
      throw new UserErrorException("Некорректное значение filter");
    }
    boolean notalks = topicListForm.getFilter() != null && "notalks".equals(topicListForm.getFilter());
    boolean tech = topicListForm.getFilter() != null && "tech".equals(topicListForm.getFilter());


    if (topicListForm.getSection() == null) {
      topicListForm.setSection(1);
    }

    if (topicListForm.getGroup() == null) {
      topicListForm.setGroup(0);
    }

    String userAgent = request.getHeader("User-Agent");
    final boolean feedBurner = userAgent != null && userAgent.contains("FeedBurner");

    if (topicListForm.getSection().intValue() == 1 &&
      topicListForm.getGroup().intValue() == 0 && !notalks && !tech && !feedBurner
      && request.getParameter("noredirect") == null) {
      return new ModelAndView(new RedirectView("http://feeds.feedburner.com/org/LOR"));
    }

    Section section = sectionService.getSection(topicListForm.getSection());
    String ptitle = section.getName();

    Group group = null;
    if (topicListForm.getGroup() != 0) {
      group = groupDao.getGroup(topicListForm.getGroup());
      ptitle += " - " + group.getTitle();
    }


    checkRequestConditions(section, group, topicListForm);

    ModelAndView modelAndView = new ModelAndView("section-rss");

    modelAndView.addObject("group", group);
    modelAndView.addObject("section", section);
    modelAndView.addObject("ptitle", ptitle);

    Calendar calendar = Calendar.getInstance();
    calendar.setTime(new Date());
    calendar.add(Calendar.MONTH, -3);

    List<Topic> messages =
      topicListService.getRssTopicsFeed(section, group, calendar.getTime(), notalks, tech, feedBurner);

    modelAndView.addObject("messages", prepareService.prepareMessages(messages, request.isSecure()));
    return modelAndView;
  }


  /**
   * @param request
   * @param modelAndView
   * @param topicListForm
   * @param messages
   */
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
        prepareService.prepareMessagesForUser(messages, request.isSecure(), tmpl.getCurrentUser())
      );

      modelAndView.setViewName("view-news");
    }

  }

  /**
   * @param modelAndView
   * @param nick
   * @return
   * @throws UserNotFoundException
   * @throws UserErrorException
   */
  private User getUserByNickname(ModelAndView modelAndView, String nick)
    throws UserNotFoundException, UserErrorException {
    User user = userDao.getUser(nick);
    if (user.getId() == User.ANONYMOUS_ID) {
      throw new UserErrorException("Лента для пользователя anonymous не доступна");
    }
    UserInfo userInfo = userDao.getUserInfoClass(user);
    modelAndView.addObject("meLink", userInfo.getUrl());
    modelAndView.addObject("user", user);
    return user;
  }

  /**
   * @param request
   * @param topicListForm
   * @param groupName
   * @param response
   * @return
   * @throws Exception
   */
  private ModelAndView group(
    HttpServletRequest request,
    TopicListRequest topicListForm,
    String groupName,
    HttpServletResponse response
  ) throws Exception {
    Section section = sectionService.getSection(topicListForm.getSection());

    Group group = groupDao.getGroup(section, groupName);
    topicListForm.setGroup(group.getId());

    ModelAndView modelAndView = mainTopicsFeedHandler(
      request,
      topicListForm,
      response
    );

    modelAndView.addObject("url", group.getUrl());
    modelAndView.addObject("params", null);

    return modelAndView;
  }

  /**
   * @param response
   * @param topicListForm
   */
  private void setExpireHeaders(
    HttpServletResponse response,
    TopicListRequest topicListForm
  ) {
    if (topicListForm.getMonth()== null) {
      response.setDateHeader("Expires", System.currentTimeMillis() + 60 * 1000);
      response.setDateHeader("Last-Modified", System.currentTimeMillis());
    } else {
      long expires = System.currentTimeMillis() + 30 * 24 * 60 * 60 * 1000L;

      Calendar calendar = Calendar.getInstance();
      calendar.set(
        topicListForm.getYear(),
        topicListForm.getMonth() - 1, 1
      );
      calendar.add(Calendar.MONTH, 1);

      long lastmod = calendar.getTimeInMillis();

      if (lastmod < System.currentTimeMillis()) {
        response.setDateHeader("Expires", expires);
        response.setDateHeader("Last-Modified", lastmod);
      } else {
        response.setDateHeader("Expires", System.currentTimeMillis() + 60 * 1000);
        response.setDateHeader("Last-Modified", System.currentTimeMillis());
      }
    }

  }

  /**
   * @param section
   * @param group
   * @param topicListForm
   * @throws Exception
   */
  private void checkRequestConditions(Section section, Group group, TopicListRequest topicListForm)
    throws Exception {
    if (topicListForm.getMonth() != null && topicListForm.getYear() == null) {
      throw new ServletParameterMissingException("year");
    }
    if (section == null && topicListForm.getTag() == null) {
      throw new ServletParameterException("section or tag required");
    }
    if (topicListForm.getTag() != null) {
      TagDao.checkTag(topicListForm.getTag());
    }
    if (section != null && group != null && group.getSectionId() != section.getId()) {
      throw new ScriptErrorException("группа #" + group.getId() + " не принадлежит разделу #" + section.getId());
    }
  }

  /**
   * @param section
   * @param group
   * @param topicListForm
   * @return
   * @throws BadDateException
   */
  private String calculatePTitle(Section section, Group group, TopicListRequest topicListForm)
    throws BadDateException {
    StringBuilder ptitle = new StringBuilder();

    if (topicListForm.getMonth() == null) {
      if (section != null) {
        ptitle.append(section.getName());
        if (group != null) {
          ptitle.append(" - ").append(group.getTitle());
        }

        if (topicListForm.getTag() != null) {
          ptitle.append(" - ").append(topicListForm.getTag());
        }
      } else {
        ptitle.append(topicListForm.getTag());
      }
    } else {
      ptitle.append("Архив: ").append(section.getName());

      if (group != null) {
        ptitle.append(" - ").append(group.getTitle());
      }

      if (topicListForm.getTag() != null) {
        ptitle.append(" - ").append(topicListForm.getTag());
      }

      ptitle
        .append(", ")
        .append(topicListForm.getYear())
        .append(", ")
        .append(DateUtil.getMonth(topicListForm.getMonth()));
    }
    return ptitle.toString();
  }

  /**
   * @param section
   * @param group
   * @param topicListForm
   * @return
   * @throws BadDateException
   * @throws SectionNotFoundException
   */
  private String calculateNavTitle(Section section, Group group, TopicListRequest topicListForm)
    throws BadDateException, SectionNotFoundException {

    StringBuilder navTitle = new StringBuilder();
    navTitle.append(topicListForm.getTag());

    if (group == null) {
      if (section != null) {
        navTitle.setLength(0);
        navTitle.append(section.getName());
      }
    } else if (section != null) {
      navTitle.setLength(0);
      navTitle
        .append("<a href=\"")
        .append(Section.getNewsViewerLink(group.getSectionId()))
        .append("\">")
        .append(section.getName())
        .append("</a> - <strong>")
        .append(group.getTitle())
        .append("</strong>");
    }
    if (topicListForm.getMonth() != null) {
      navTitle
        .append(" - Архив ")
        .append(topicListForm.getYear())
        .append(", ")
        .append(DateUtil.getMonth(topicListForm.getMonth()));
    }
    return navTitle.toString();
  }

}
