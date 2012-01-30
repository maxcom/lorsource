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
public class FeedTopicController {
  private static final Log logger = LogFactory.getLog(FeedTopicController.class);

  @Autowired
  private SectionService sectionService;

  @Autowired
  private FeedTopicService feedTopicService;

  @Autowired
  private TopicPrepareService prepareService;

  // TODO: здесь должен быть сервис, а не DAO
  @Autowired
  private GroupDao groupDao;

  // TODO: здесь должен быть сервис, а не DAO
  @Autowired
  private UserDao userDao;


  /**
   * @param month
   * @param year
   * @param sectionId
   * @param groupId
   * @param tag
   * @param offset
   * @param request
   * @param response
   * @return
   * @throws Exception
   */
  @RequestMapping(value = "/view-news.jsp", method = {RequestMethod.GET, RequestMethod.HEAD})
  public ModelAndView mainTopicsFeedHandler(
    @RequestParam(value = "month", required = false) Integer month,
    @RequestParam(value = "year", required = false) Integer year,
    @RequestParam(value = "section", required = false) Integer sectionId,
    @RequestParam(value = "group", required = false) Integer groupId,
    @RequestParam(value = "tag", required = false) String tag,
    @RequestParam(value = "offset", required = false) Integer offset,
    HttpServletRequest request,
    HttpServletResponse response
  ) throws Exception {

    Section section = null;
    if (sectionId != null) {
      section = sectionService.getSection(sectionId);
    }

    Group group = null;
    if (groupId != null) {
      group = groupDao.getGroup(groupId);
    }
    checkRequestConditions(section, group, month, year, tag);

    ModelAndView modelAndView = new ModelAndView("view-news");

    URLUtil.QueryString queryString = new URLUtil.QueryString();
    queryString.add("section", sectionId);
    queryString.add("tag", tag);
    queryString.add("group", groupId);
    modelAndView.addObject("params", queryString.toString());

    modelAndView.addObject("url", "view-news.jsp");
    if (section != null) {
      modelAndView.addObject("section", section);
      modelAndView.addObject("archiveLink", section.getArchiveLink());
    }

    setExpireHeaders(response, month, year);
    if (month == null) {
      modelAndView.addObject("year", year);
      modelAndView.addObject("month", month);
    }
    modelAndView.addObject("group", group);

    modelAndView.addObject("tag", tag);

    modelAndView.addObject("ptitle", calculatePTitle(section, group, tag, year, month));
    modelAndView.addObject("navtitle", calculateNavTitle(section, tag, group, year, month));

    offset = feedTopicService.fixOffset(offset);

    List<Topic> messages = feedTopicService.getTopicsFeed(section, group, tag, offset, year, month);

    Template tmpl = Template.getTemplate(request);
    modelAndView.addObject(
      "messages",
      prepareService.prepareMessagesForUser(messages, request.isSecure(), tmpl.getCurrentUser())
    );

    modelAndView.addObject("offsetNavigation", month == null);
    modelAndView.addObject("offset", offset);

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
   * @param offset
   * @param request
   * @param response
   * @return
   * @throws Exception
   */
  @RequestMapping("/gallery/")
  public ModelAndView gallery(
    @RequestParam(required = false) Integer offset,
    HttpServletRequest request,
    HttpServletResponse response
  ) throws Exception {
    ModelAndView modelAndView =
      mainTopicsFeedHandler(null, null, Section.SECTION_GALLERY, null, null, offset, request, response);

    modelAndView.addObject("url", "/gallery/");
    modelAndView.addObject("params", null);

    return modelAndView;
  }

  /**
   * @param offset
   * @param request
   * @param response
   * @return
   * @throws Exception
   */
  @RequestMapping("/forum/lenta")
  public ModelAndView forum(
    @RequestParam(required = false) Integer offset,
    HttpServletRequest request,
    HttpServletResponse response
  ) throws Exception {
    ModelAndView modelAndView =
      mainTopicsFeedHandler(null, null, Section.SECTION_FORUM, null, null, offset, request, response);

    modelAndView.addObject("url", "/forum/lenta");
    modelAndView.addObject("params", null);

    return modelAndView;
  }

  /**
   * @param offset
   * @param request
   * @param response
   * @return
   * @throws Exception
   */
  @RequestMapping("/polls/")
  public ModelAndView polls(
    @RequestParam(required = false) Integer offset,
    HttpServletRequest request,
    HttpServletResponse response
  ) throws Exception {
    ModelAndView modelAndView =
      mainTopicsFeedHandler(null, null, Section.SECTION_POLLS, null, null, offset, request, response);

    modelAndView.addObject("url", "/polls/");
    modelAndView.addObject("params", null);

    return modelAndView;
  }

  /**
   * @param offset
   * @param request
   * @param response
   * @return
   * @throws Exception
   */
  @RequestMapping("/news/")
  public ModelAndView news(
    @RequestParam(required = false) Integer offset,
    HttpServletRequest request,
    HttpServletResponse response
  ) throws Exception {
    ModelAndView modelAndView =
      mainTopicsFeedHandler(null, null, Section.SECTION_NEWS, null, null, offset, request, response);

    modelAndView.addObject("url", "/news/");
    modelAndView.addObject("params", null);

    return modelAndView;
  }

  /**
   * @param offset
   * @param groupName
   * @param request
   * @param response
   * @return
   * @throws Exception
   */
  @RequestMapping("/gallery/{group}")
  public ModelAndView galleryGroup(
    @RequestParam(required = false) Integer offset,
    @PathVariable("group") String groupName,
    HttpServletRequest request,
    HttpServletResponse response
  ) throws Exception {
    return group(Section.SECTION_GALLERY, offset, groupName, request, response);
  }

  /**
   * @param offset
   * @param groupName
   * @param request
   * @param response
   * @return
   * @throws Exception
   */
  @RequestMapping("/news/{group}")
  public ModelAndView newsGroup(
    @RequestParam(required = false) Integer offset,
    @PathVariable("group") String groupName,
    HttpServletRequest request,
    HttpServletResponse response
  ) throws Exception {
    return group(Section.SECTION_NEWS, offset, groupName, request, response);
  }

  /**
   * @param offset
   * @param groupName
   * @param request
   * @param response
   * @return
   * @throws Exception
   */
  @RequestMapping("/polls/{group}")
  public ModelAndView pollsGroup(
    @RequestParam(required = false) Integer offset,
    @PathVariable("group") String groupName,
    HttpServletRequest request,
    HttpServletResponse response
  ) throws Exception {
    return group(Section.SECTION_POLLS, offset, groupName, request, response);
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
    @PathVariable String section,
    @PathVariable int year,
    @PathVariable int month,
    HttpServletRequest request,
    HttpServletResponse response
  ) throws Exception {
    ModelAndView modelAndView =
      mainTopicsFeedHandler(month, year, Section.getSection(section), null, null, null, request, response);

    modelAndView.addObject("url", "/gallery/archive/" + year + '/' + month + '/');
    modelAndView.addObject("params", null);

    return modelAndView;
  }

  /**
   * @param nick
   * @param offset
   * @param output
   * @param request
   * @param response
   * @return
   * @throws Exception
   */
  @RequestMapping("/people/{nick}")
  public ModelAndView showUserTopicsNew(
    @PathVariable String nick,
    @RequestParam(value = "offset", required = false) Integer offset,
    @RequestParam(value = "output", required = false) String output,
    HttpServletRequest request,
    HttpServletResponse response
  ) throws Exception {

    setExpireHeaders(response, null, null);

    ModelAndView modelAndView = new ModelAndView();

    User user = getUserByNickname(modelAndView, nick);

    modelAndView.addObject("url", "/people/" + nick + '/');
    modelAndView.addObject("whoisLink", "/people/" + nick + '/' + "profile");
    // TODO: modelAndView.addObject("archiveLink", "/people/"+nick+"/archive/");


    modelAndView.addObject("ptitle", "Сообщения " + user.getNick());
    modelAndView.addObject("navtitle", "Сообщения " + user.getNick());

    modelAndView.addObject("offsetNavigation", true);
    modelAndView.addObject("offset", offset);

    modelAndView.addObject("rssLink", "/people/" + nick + "/?output=rss");

    offset = feedTopicService.fixOffset(offset);

    List<Topic> messages = feedTopicService.getUserTopicsFeed(user, offset, false);
    prepareTopicsForPlainOrRss(request, modelAndView, output, messages);
    return modelAndView;
  }

  /**
   * @param nick
   * @param offset
   * @param output
   * @param request
   * @param response
   * @return
   * @throws Exception
   */
  @RequestMapping("/people/{nick}/favs")
  public ModelAndView showUserFavs(
    @PathVariable String nick,
    @RequestParam(value = "offset", required = false) Integer offset,
    @RequestParam(value = "output", required = false) String output,
    HttpServletRequest request,
    HttpServletResponse response
  ) throws Exception {
    setExpireHeaders(response, null, null);

    ModelAndView modelAndView = new ModelAndView();

    User user = getUserByNickname(modelAndView, nick);

    modelAndView.addObject("url", "/people/" + nick + "/favs");
    modelAndView.addObject("whoisLink", "/people/" + nick + '/' + "profile");

    modelAndView.addObject("ptitle", "Избранные сообщения " + user.getNick());
    modelAndView.addObject("navtitle", "Избранные сообщения " + user.getNick());

    offset = feedTopicService.fixOffset(offset);
    List<Topic> messages = feedTopicService.getUserTopicsFeed(user, offset, false);
    prepareTopicsForPlainOrRss(request, modelAndView, output, messages);
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

    List<Topic> messages = feedTopicService.getAllTopicsFeed(section, calendar.getTime());
    modelAndView.addObject(
      "messages",
      prepareService.prepareMessagesForUser(messages, request.isSecure(), tmpl.getCurrentUser())
    );

    List<FeedTopicDto.DeletedTopic> deleted = feedTopicService.getDeletedTopicsFeed(sectionId);

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
    @RequestParam(value = "filter", required = false) String filter,
    @RequestParam(value = "section", required = false) Integer sectionId,
    @RequestParam(value = "group", required = false) Integer groupId,
    HttpServletRequest request
  ) throws Exception {

    final String[] filterValues = {"all", "notalks", "tech"};
    final Set<String> filterValuesSet = new HashSet<String>(Arrays.asList(filterValues));

    if (filter != null && !filterValuesSet.contains(filter)) {
      throw new UserErrorException("Некорректное значение filter");
    }
    boolean notalks = filter != null && "notalks".equals(filter);
    boolean tech = filter != null && "tech".equals(filter);


    if (sectionId == null) {
      sectionId = 1;
    }

    if (groupId == null) {
      groupId = 0;
    }

    String userAgent = request.getHeader("User-Agent");
    final boolean feedBurner = userAgent != null && userAgent.contains("FeedBurner");

    if (sectionId == 1 && groupId == 0 && !notalks && !tech && !feedBurner && request.getParameter("noredirect") == null) {
      return new ModelAndView(new RedirectView("http://feeds.feedburner.com/org/LOR"));
    }

    Section section = sectionService.getSection(sectionId);
    String ptitle = section.getName();

    Group group = null;
    if (groupId != 0) {
      group = groupDao.getGroup(groupId);
      ptitle += " - " + group.getTitle();
    }


    checkRequestConditions(section, group, null, null, null);

    ModelAndView modelAndView = new ModelAndView("section-rss");

    modelAndView.addObject("group", group);
    modelAndView.addObject("section", section);
    modelAndView.addObject("ptitle", ptitle);

    Calendar calendar = Calendar.getInstance();
    calendar.setTime(new Date());
    calendar.add(Calendar.MONTH, -3);

    List<Topic> messages =
      feedTopicService.getRssTopicsFeed(section, group, calendar.getTime(), notalks, tech, feedBurner);

    modelAndView.addObject("messages", prepareService.prepareMessages(messages, request.isSecure()));
    return modelAndView;
  }


  /**
   * @param request
   * @param modelAndView
   * @param output
   * @param messages
   */
  private void prepareTopicsForPlainOrRss(
    HttpServletRequest request,
    ModelAndView modelAndView,
    String output,
    List<Topic> messages
  ) {
    boolean rss = output != null && "rss".equals(output);
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
   * @param sectionId
   * @param offset
   * @param groupName
   * @param request
   * @param response
   * @return
   * @throws Exception
   */
  private ModelAndView group(
    int sectionId,
    Integer offset,
    String groupName,
    HttpServletRequest request,
    HttpServletResponse response
  ) throws Exception {
    Section section = sectionService.getSection(sectionId);

    Group group = groupDao.getGroup(section, groupName);

    ModelAndView modelAndView =
      mainTopicsFeedHandler(null, null, group.getSectionId(), group.getId(), null, offset, request, response);

    modelAndView.addObject("url", group.getUrl());
    modelAndView.addObject("params", null);

    return modelAndView;
  }

  /**
   * @param response
   * @param month
   * @param year
   */
  private void setExpireHeaders(
    HttpServletResponse response,
    Integer month,
    Integer year
  ) {
    if (month == null) {
      response.setDateHeader("Expires", System.currentTimeMillis() + 60 * 1000);
      response.setDateHeader("Last-Modified", System.currentTimeMillis());
    } else {
      long expires = System.currentTimeMillis() + 30 * 24 * 60 * 60 * 1000L;

      Calendar calendar = Calendar.getInstance();
      calendar.set(year, month - 1, 1);
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
   * @param month
   * @param year
   * @param tag
   * @throws Exception
   */
  private void checkRequestConditions(Section section, Group group, Integer month, Integer year, String tag)
    throws Exception {
    if (month != null && year == null) {
      throw new ServletParameterMissingException("year");
    }
    if (section == null && tag == null) {
      throw new ServletParameterException("section or tag required");
    }
    if (tag != null) {
      TagDao.checkTag(tag);
    }
    if (section != null && group != null && group.getSectionId() != section.getId()) {
      throw new ScriptErrorException("группа #" + group.getId() + " не принадлежит разделу #" + section.getId());
    }
  }

  /**
   * @param section
   * @param group
   * @param tag
   * @param year
   * @param month
   * @return
   * @throws BadDateException
   */
  private String calculatePTitle(Section section, Group group, String tag, Integer year, Integer month)
    throws BadDateException {
    String ptitle;

    if (month == null) {
      if (section != null) {
        ptitle = section.getName();
        if (group != null) {
          ptitle += " - " + group.getTitle();
        }

        if (tag != null) {
          ptitle += " - " + tag;
        }
      } else {
        ptitle = tag;
      }
    } else {
      ptitle = "Архив: " + section.getName();

      if (group != null) {
        ptitle += " - " + group.getTitle();
      }

      if (tag != null) {
        ptitle += " - " + tag;
      }

      ptitle += ", " + year + ", " + DateUtil.getMonth(month);
    }
    return ptitle;
  }

  /**
   * @param section
   * @param tag
   * @param group
   * @param year
   * @param month
   * @return
   * @throws BadDateException
   * @throws SectionNotFoundException
   */
  private String calculateNavTitle(Section section, String tag, Group group, Integer year, Integer month)
    throws BadDateException, SectionNotFoundException {

    String navTitle = tag;

    if (group == null) {
      if (section != null) {
        navTitle = section.getName();
      }
    } else if (section != null) {
      navTitle = "<a href=\"" + Section.getNewsViewerLink(group.getSectionId()) + "\">" + section.getName() + "</a> - <strong>" + group.getTitle() + "</strong>";
    }
    if (month != null) {
      navTitle += " - Архив " + year + ", " + DateUtil.getMonth(month);
    }
    return navTitle;
  }

}
