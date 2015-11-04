/*
 * Copyright 1998-2015 Linux.org.ru
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

import org.joda.time.DateTime;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.View;
import org.springframework.web.servlet.view.RedirectView;
import ru.org.linux.group.Group;
import ru.org.linux.group.GroupDao;
import ru.org.linux.group.GroupNotFoundException;
import ru.org.linux.section.Section;
import ru.org.linux.section.SectionNotFoundException;
import ru.org.linux.section.SectionService;
import ru.org.linux.site.ScriptErrorException;
import ru.org.linux.site.Template;
import ru.org.linux.user.UserErrorException;
import ru.org.linux.util.BadDateException;
import ru.org.linux.util.DateUtil;
import ru.org.linux.util.ServletParameterException;
import ru.org.linux.util.ServletParameterMissingException;

import javax.annotation.Nullable;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.net.URLEncoder;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Controller
public class TopicListController {
  @Autowired
  private SectionService sectionService;

  @Autowired
  private TopicListService topicListService;

  @Autowired
  private TopicPrepareService prepareService;

  // TODO: здесь должен быть сервис, а не DAO
  @Autowired
  private GroupDao groupDao;

  private ModelAndView mainTopicsFeedHandler(
    Section section,
    HttpServletRequest request,
    TopicListRequest topicListForm,
    HttpServletResponse response,
    @Nullable Group group
  ) throws Exception {
    checkRequestConditions(section, group, topicListForm);
    Template tmpl = Template.getTemplate(request);

    ModelAndView modelAndView = new ModelAndView("view-news");

    modelAndView.addObject("group", group);

    modelAndView.addObject("url", "view-news.jsp");
    modelAndView.addObject("section", section);

    modelAndView.addObject("archiveLink", section.getArchiveLink());

    setExpireHeaders(response, topicListForm.getYear(), topicListForm.getMonth());

    modelAndView.addObject("navtitle", calculateNavTitle(section, group, topicListForm));

    topicListForm.setOffset(
      topicListService.fixOffset(topicListForm.getOffset())
    );

    List<Topic> messages = topicListService.getTopicsFeed(
      section,
      group,
      null,
      topicListForm.getOffset(),
      topicListForm.getYear(),
      topicListForm.getMonth(),
      20
    );

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

    modelAndView.addObject("offsetNavigation", topicListForm.getMonth() == null);

    return modelAndView;
  }

  @RequestMapping("/gallery/")
  public ModelAndView gallery(
    HttpServletRequest request,
    TopicListRequest topicListForm,
    HttpServletResponse response
  ) throws Exception {
    Section section = sectionService.getSection(Section.SECTION_GALLERY);

    ModelAndView modelAndView = mainTopicsFeedHandler(section, request, topicListForm, response, null);

    modelAndView.addObject("ptitle", calculatePTitle(section, topicListForm));
    modelAndView.addObject("url", "/gallery/");
    modelAndView.addObject("rssLink", "section-rss.jsp?section=3");

    return modelAndView;
  }

  @RequestMapping("/forum/lenta")
  public ModelAndView forum(
    HttpServletRequest request,
    TopicListRequest topicListForm,
    HttpServletResponse response
  ) throws Exception {
    Section section = sectionService.getSection(Section.SECTION_FORUM);

    ModelAndView modelAndView = mainTopicsFeedHandler(section, request, topicListForm, response, null);
    modelAndView.addObject("ptitle", calculatePTitle(section, topicListForm));
    modelAndView.addObject("url", "/forum/lenta");
    modelAndView.addObject("rssLink", "section-rss.jsp?section=2");

    return modelAndView;
  }

  @RequestMapping("/polls/")
  public ModelAndView polls(
    HttpServletRequest request,
    TopicListRequest topicListForm,
    HttpServletResponse response
  ) throws Exception {
    Section section = sectionService.getSection(Section.SECTION_POLLS);
    ModelAndView modelAndView = mainTopicsFeedHandler(section, request, topicListForm, response, null);

    modelAndView.addObject("url", "/polls/");
    modelAndView.addObject("params", null);
    modelAndView.addObject("ptitle", calculatePTitle(section, topicListForm));

    return modelAndView;
  }

  @RequestMapping("/news/")
  public ModelAndView news(
    HttpServletRequest request,
    TopicListRequest topicListForm,
    HttpServletResponse response
  ) throws Exception {
    Section section = sectionService.getSection(Section.SECTION_NEWS);
    ModelAndView modelAndView = mainTopicsFeedHandler(section, request, topicListForm, response, null);

    modelAndView.addObject("url", "/news/");
    modelAndView.addObject("ptitle", calculatePTitle(section, topicListForm));
    modelAndView.addObject("rssLink", "section-rss.jsp?section=1");

    return modelAndView;
  }

  @RequestMapping("/gallery/{group}")
  public ModelAndView galleryGroup(
    HttpServletRequest request,
    TopicListRequest topicListForm,
    @PathVariable("group") String groupName,
    HttpServletResponse response
  ) throws Exception {
    Section section = sectionService.getSection(Section.SECTION_GALLERY);

    return group(section, request, topicListForm, groupName, response);
  }

  @RequestMapping("/news/{group}")
  public ModelAndView newsGroup(
    HttpServletRequest request,
    TopicListRequest topicListForm,
    @PathVariable("group") String groupName,
    HttpServletResponse response
  ) throws Exception {
    Section section = sectionService.getSection(Section.SECTION_NEWS);

    return group(section, request, topicListForm, groupName, response);
  }

  @RequestMapping("/polls/{group}")
  public ModelAndView pollsGroup(
    HttpServletRequest request,
    TopicListRequest topicListForm,
    @PathVariable("group") String groupName,
    HttpServletResponse response
  ) throws Exception {
    Section section = sectionService.getSection(Section.SECTION_POLLS);

    return group(section, request, topicListForm, groupName, response);
  }

  @RequestMapping("/{section}/archive/{year}/{month}")
  public ModelAndView galleryArchive(
    HttpServletRequest request,
    @PathVariable String section,
    @PathVariable int year,
    @PathVariable int month,
    HttpServletResponse response
  ) throws Exception {
    TopicListRequest topicListForm = new TopicListRequest();

    Section sectionObject = sectionService.getSectionByName(section);

    if (sectionObject.isPremoderated()) {
      topicListForm.setYear(year);
      topicListForm.setMonth(month);

      ModelAndView modelAndView = mainTopicsFeedHandler(sectionObject, request, topicListForm, response, null);

      modelAndView.addObject("ptitle", calculatePTitle(sectionObject, topicListForm));

      return modelAndView;
    } else {
      return new ModelAndView(new RedirectView(sectionObject.getSectionLink()));
    }
  }

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

  @Deprecated
  @RequestMapping(value = "/view-news.jsp", params = {"section", "!tag"})
  public View oldLink(
    TopicListRequest topicListForm,
    @RequestParam("section") int sectionId,
    @RequestParam(value="group", defaultValue = "0") int groupId
  ) throws Exception {
    Section section = sectionService.getSection(sectionId);

    StringBuilder redirectLink = new StringBuilder();

    redirectLink.append(section.getNewsViewerLink());

    if (topicListForm.getYear() != null && topicListForm.getMonth() != null) {
      redirectLink
        .append(topicListForm.getYear())
        .append('/')
        .append(topicListForm.getMonth());
    } else if (groupId>0) {
      Group group = groupDao.getGroup(groupId);
      redirectLink
        .append(group.getUrlName())
        .append('/');
    }

    String queryStr = topicListForm.getOffset() == null ?
        "" :
        URLEncoder.encode("offset=" + topicListForm.getOffset(), "UTF-8");

    if (!queryStr.isEmpty()) {
      redirectLink
        .append('?')
        .append(queryStr);
    }

    return new RedirectView(redirectLink.toString());
  }

  @RequestMapping("/section-rss.jsp")
  public ModelAndView showRSS(
    HttpServletRequest request,
    TopicListRequest topicListForm,
    @RequestParam(value="section", defaultValue = "1") int sectionId,
    @RequestParam(value="group", defaultValue = "0") int groupId,
    @RequestParam(value="filter", required = false) String filter
  ) throws Exception {

    final String[] filterValues = {"all", "notalks", "tech"};
    final Set<String> filterValuesSet = new HashSet<>(Arrays.asList(filterValues));

    if (filter != null && !filterValuesSet.contains(filter)) {
      throw new UserErrorException("Некорректное значение filter");
    }
    boolean notalks = filter != null && "notalks".equals(filter);
    boolean tech = filter != null && "tech".equals(filter);

    Section section = sectionService.getSection(sectionId);
    String ptitle = section.getName();

    Group group = null;
    if (groupId != 0) {
      group = groupDao.getGroup(groupId);
      ptitle += " - " + group.getTitle();
    }

    checkRequestConditions(section, group, topicListForm);

    ModelAndView modelAndView = new ModelAndView("section-rss");

    modelAndView.addObject("group", group);
    modelAndView.addObject("section", section);
    modelAndView.addObject("ptitle", ptitle);

    DateTime fromDate = DateTime.now().minusMonths(3);

    List<Topic> messages = topicListService.getRssTopicsFeed(section, group, fromDate.toDate(), notalks, tech);

    modelAndView.addObject("messages", prepareService.prepareMessages(messages, request.isSecure()));
    return modelAndView;
  }

  private ModelAndView group(
    Section section,
    HttpServletRequest request,
    TopicListRequest topicListForm,
    String groupName,
    HttpServletResponse response
  ) throws Exception {
    Group group = groupDao.getGroup(section, groupName);

    ModelAndView modelAndView = mainTopicsFeedHandler(
      section,
      request,
      topicListForm,
      response,
      group
    );

    modelAndView.addObject("ptitle", section.getName() + " - " + group.getTitle());

    modelAndView.addObject("url", group.getUrl());

    return modelAndView;
  }

  public static void setExpireHeaders(
          HttpServletResponse response,
          Integer year, Integer month
  ) {
    if (month == null) {
      response.setDateHeader("Expires", System.currentTimeMillis() + 60 * 1000);
      response.setDateHeader("Last-Modified", System.currentTimeMillis());
    } else {
      DateTime endOfMonth = new DateTime(year, month, 1, 0, 0).plusMonths(1);

      if (endOfMonth.isBeforeNow()) {
        response.setDateHeader("Last-Modified", endOfMonth.getMillis());
      } else {
        response.setDateHeader("Expires", System.currentTimeMillis() + 60 * 1000);
        response.setDateHeader("Last-Modified", System.currentTimeMillis());
      }
    }
  }

  private void checkRequestConditions(Section section, Group group, TopicListRequest topicListForm)
    throws Exception {
    if (topicListForm.getMonth() != null && topicListForm.getYear() == null) {
      throw new ServletParameterMissingException("year");
    }
    if (section == null) {
      throw new ServletParameterException("section or tag required");
    }
    if (group != null && group.getSectionId() != section.getId()) {
      throw new ScriptErrorException("группа #" + group.getId() + " не принадлежит разделу #" + section.getId());
    }
  }

  private static String calculatePTitle(Section section, TopicListRequest topicListForm)
    throws BadDateException {

    if (topicListForm.getMonth() == null) {
      return section.getName();
    } else {
      return "Архив: " + section.getName()
              + ", " +
              topicListForm.getYear() +
              ", " +
              DateUtil.getMonth(topicListForm.getMonth());
    }
  }

  private static String calculateNavTitle(Section section, Group group, TopicListRequest topicListForm)
    throws BadDateException, SectionNotFoundException {

    StringBuilder navTitle = new StringBuilder();

    if (group == null) {
      navTitle.setLength(0);
      navTitle.append(section.getName());
    } else {
      navTitle.setLength(0);
      navTitle
              .append(section.getName())
              .append(" «")
              .append(group.getTitle())
              .append("»");
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

  @ExceptionHandler({GroupNotFoundException.class, SectionNotFoundException.class})
  @ResponseStatus(HttpStatus.NOT_FOUND)
  public ModelAndView handleNotFoundException() {
    return new ModelAndView("errors/code404");
  }
}
