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

import com.google.common.base.Strings;
import org.apache.commons.lang.WordUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.View;
import org.springframework.web.servlet.view.RedirectView;
import org.springframework.web.util.UriComponentsBuilder;
import org.springframework.web.util.UriTemplate;
import ru.org.linux.group.Group;
import ru.org.linux.group.GroupDao;
import ru.org.linux.group.GroupNotFoundException;
import ru.org.linux.section.Section;
import ru.org.linux.section.SectionNotFoundException;
import ru.org.linux.section.SectionService;
import ru.org.linux.site.ScriptErrorException;
import ru.org.linux.site.Template;
import ru.org.linux.tag.TagService;
import ru.org.linux.user.*;
import ru.org.linux.util.*;

import javax.annotation.Nullable;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.*;

@Controller
public class TopicListController {
  private static final UriTemplate TAG_URI_TEMPLATE = new UriTemplate("/tag/{tag}");
  private static final UriTemplate TAGS_URI_TEMPLATE = new UriTemplate("/tags/{tag}");

  @Autowired
  private SectionService sectionService;

  @Autowired
  private TopicListService topicListService;

  @Autowired
  private TopicPrepareService prepareService;

  @Autowired
  private TagService tagService;

  @Autowired
  private UserTagService userTagService;

  // TODO: здесь должен быть сервис, а не DAO
  @Autowired
  private GroupDao groupDao;

  // TODO: здесь должен быть сервис, а не DAO
  @Autowired
  private UserDao userDao;

  @RequestMapping(value = "/view-news.jsp", method = {RequestMethod.GET, RequestMethod.HEAD}, params = {"tag"})
  public View tagFeedOld(
          TopicListRequest topicListForm
  ) {
    return new RedirectView(tagListUrl(topicListForm.getTag()));
  }

  public static String tagListUrl(String tag) {
    return TAG_URI_TEMPLATE.expand(tag).toString();
  }

  public static String tagsUrl(char letter) {
    return TAGS_URI_TEMPLATE.expand(letter).toString();
  }

  @RequestMapping(value = "/tag/{tag}", method = {RequestMethod.GET, RequestMethod.HEAD})
  public ModelAndView tagFeed(
    HttpServletRequest request,
    HttpServletResponse response,
    @PathVariable String tag,
    @RequestParam(value = "offset", defaultValue = "0") int offset,
    @RequestParam(value = "section", defaultValue = "0") int sectionId
  ) throws Exception {
    Section section;

    if (sectionId!=0) {
      section = sectionService.getSection(sectionId);
    } else {
      section = null;
    }

    TopicListRequest topicListForm = new TopicListRequest();

    topicListForm.setTag(tag);
    topicListForm.setOffset(offset);
    topicListForm.setSection(sectionId);

    ModelAndView modelAndView = mainTopicsFeedHandler(request, topicListForm, response, null);

    modelAndView.addObject("tag", tag);
    modelAndView.addObject("section", sectionId);
    modelAndView.addObject("offset", offset);
    modelAndView.addObject("sectionList", sectionService.getSectionList());

    Template tmpl = Template.getTemplate(request);

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

    if (sectionId==0) {
      modelAndView.addObject("ptitle", WordUtils.capitalize(tag));
    } else {
      modelAndView.addObject("ptitle", WordUtils.capitalize(tag)+" ("+section.getName()+")");
    }

    modelAndView.setViewName("tag-topics");

    List messages = (List) modelAndView.getModel().get("messages");

    if (offset<200 && messages.size()==20) {
      modelAndView.addObject("nextLink", buildTagUri(tag, sectionId, offset+20));
    }

    if (offset>=20) {
      modelAndView.addObject("prevLink", buildTagUri(tag, sectionId, offset-20));
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

  private ModelAndView mainTopicsFeedHandler(
    HttpServletRequest request,
    TopicListRequest topicListForm,
    HttpServletResponse response,
    @Nullable Group group
  ) throws Exception {
    Section section = null;
    if (topicListForm.getSection() != null && topicListForm.getSection()!=0) {
      section = sectionService.getSection(topicListForm.getSection());
    }

    checkRequestConditions(section, group, topicListForm);
    Template tmpl = Template.getTemplate(request);

    ModelAndView modelAndView = new ModelAndView("view-news");

    modelAndView.addObject("group", group);

    modelAndView.addObject("url", "view-news.jsp");
    if (section != null) {
      modelAndView.addObject("section", section);

      if (Strings.isNullOrEmpty(topicListForm.getTag())) {
        modelAndView.addObject("archiveLink", section.getArchiveLink());
      }
    }

    setExpireHeaders(response, topicListForm);

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

    if (section != null && Strings.isNullOrEmpty(topicListForm.getTag())) {
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
    ModelAndView modelAndView = mainTopicsFeedHandler(request, topicListForm, response, null);

    modelAndView.addObject("ptitle", calculatePTitle(sectionService.getSection(Section.SECTION_GALLERY), topicListForm));
    modelAndView.addObject("url", "/gallery/");

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
    ModelAndView modelAndView = mainTopicsFeedHandler(request, topicListForm, response, null);

    modelAndView.addObject("ptitle", calculatePTitle(sectionService.getSection(Section.SECTION_FORUM), topicListForm));

    modelAndView.addObject("url", "/forum/lenta");

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
    ModelAndView modelAndView = mainTopicsFeedHandler(request, topicListForm, response, null);

    modelAndView.addObject("url", "/polls/");
    modelAndView.addObject("params", null);
    modelAndView.addObject("ptitle", calculatePTitle(sectionService.getSection(Section.SECTION_POLLS), topicListForm));

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
    ModelAndView modelAndView = mainTopicsFeedHandler(request, topicListForm, response, null);

    modelAndView.addObject("url", "/news/");
    modelAndView.addObject("ptitle", calculatePTitle(sectionService.getSection(Section.SECTION_NEWS), topicListForm));

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
    @PathVariable String section,
    @PathVariable int year,
    @PathVariable int month,
    HttpServletResponse response
  ) throws Exception {
    TopicListRequest topicListForm = new TopicListRequest();

    Section sectionObject = sectionService.getSectionByName(section);
    topicListForm.setSection(sectionObject.getId());
    topicListForm.setYear(year);
    topicListForm.setMonth(month);

    ModelAndView modelAndView = mainTopicsFeedHandler(request, topicListForm, response, null);

    modelAndView.addObject("ptitle", calculatePTitle(sectionObject, topicListForm));
    modelAndView.addObject("url", "/gallery/archive/" + year + '/' + month + '/');

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
    if (topicListForm.getSection() != null && topicListForm.getSection() != 0) {
      section = sectionService.getSection(topicListForm.getSection());
    }

    if (topicListForm.getTag() != null) {
      tagService.checkTag(topicListForm.getTag());
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
    queryString.add("tag", topicListForm.getTag());
    modelAndView.addObject("params", queryString.toString());

    prepareTopicsForPlainOrRss(request, modelAndView, topicListForm, messages);

    if (!rss) {
      modelAndView.setViewName("user-topics");
    }

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
  @RequestMapping(value="/people/{nick}/favs", params="!output")
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

    List<Topic> messages = topicListService.getUserTopicsFeed(user, topicListForm.getOffset(), true, false);
    prepareTopicsForPlainOrRss(request, modelAndView, topicListForm, messages);

    modelAndView.setViewName("user-topics");

    return modelAndView;
  }

  @RequestMapping(value = "/people/{nick}/tracked", params="!output")
  public ModelAndView showUserWatches(
    HttpServletRequest request,
    TopicListRequest topicListForm,
    @PathVariable String nick,
    HttpServletResponse response
  ) throws Exception {
    setExpireHeaders(response, topicListForm);

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
  ) {
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
      prepareService.prepareMessagesForUser(
              messages,
              request.isSecure(),
              tmpl.getCurrentUser(),
              tmpl.getProf(),
              false
      )
    );

    List<TopicListDto.DeletedTopic> deleted = topicListService.getDeletedTopicsFeed(sectionId);

    modelAndView.addObject("deletedTopics", deleted);
    modelAndView.addObject("sections", sectionService.getSectionList());

    return modelAndView;
  }

  /**
   * @param topicListForm
   * @return
   * @throws Exception
   * @deprecated
   */
  @Deprecated
  @RequestMapping(value = "/view-news.jsp", params = {"section", "!tag"})
  public View oldLink(
    TopicListRequest topicListForm,
    @RequestParam(value="group", defaultValue = "0") int groupId
  ) throws Exception {
    StringBuilder redirectLink = new StringBuilder();

    Section section = sectionService.getSection(topicListForm.getSection());

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

    URLUtil.QueryString queryString = new URLUtil.QueryString();
    queryString.add("offset", topicListForm.getOffset());

    String queryStr = queryString.toString();
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
    @RequestParam(value="group", defaultValue = "0") int groupId
  ) throws Exception {

    final String[] filterValues = {"all", "notalks", "tech"};
    final Set<String> filterValuesSet = new HashSet<>(Arrays.asList(filterValues));

    if (topicListForm.getFilter() != null && !filterValuesSet.contains(topicListForm.getFilter())) {
      throw new UserErrorException("Некорректное значение filter");
    }
    boolean notalks = topicListForm.getFilter() != null && "notalks".equals(topicListForm.getFilter());
    boolean tech = topicListForm.getFilter() != null && "tech".equals(topicListForm.getFilter());


    if (topicListForm.getSection() == null) {
      topicListForm.setSection(1);
    }

    String userAgent = request.getHeader("User-Agent");
    final boolean feedBurner = userAgent != null && userAgent.contains("FeedBurner");

    if (topicListForm.getSection() == 1 &&
            groupId == 0 && !notalks && !tech && !feedBurner
      && request.getParameter("noredirect") == null) {
      return new ModelAndView(new RedirectView("http://feeds.feedburner.com/org/LOR"));
    }

    Section section = sectionService.getSection(topicListForm.getSection());
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
        prepareService.prepareMessagesForUser(
                messages,
                request.isSecure(),
                tmpl.getCurrentUser(),
                tmpl.getProf(),
                false
        )
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
    modelAndView.addObject("user", user);
    return user;
  }

  private ModelAndView group(
    HttpServletRequest request,
    TopicListRequest topicListForm,
    String groupName,
    HttpServletResponse response
  ) throws Exception {
    Section section = sectionService.getSection(topicListForm.getSection());

    Group group = groupDao.getGroup(section, groupName);

    ModelAndView modelAndView = mainTopicsFeedHandler(
      request,
      topicListForm,
      response,
      group
    );

    StringBuilder ptitle = new StringBuilder();

    ptitle.append(section.getName());
    if (group != null) {
      ptitle.append(" - ").append(group.getTitle());
    }

    modelAndView.addObject("ptitle", ptitle.toString());

    modelAndView.addObject("url", group.getUrl());

    return modelAndView;
  }

  /**
   * @param response
   * @param topicListForm
   */
  private static void setExpireHeaders(
          HttpServletResponse response,
          TopicListRequest topicListForm
  ) {
    if (topicListForm.getMonth() == null) {
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
      tagService.checkTag(topicListForm.getTag());
    }
    if (section != null && group != null && group.getSectionId() != section.getId()) {
      throw new ScriptErrorException("группа #" + group.getId() + " не принадлежит разделу #" + section.getId());
    }
  }

  /**
   *
   * @param section
   * @param topicListForm
   * @return
   * @throws BadDateException
   */
  private static String calculatePTitle(Section section, TopicListRequest topicListForm)
    throws BadDateException {
    StringBuilder ptitle = new StringBuilder();

    if (topicListForm.getMonth() == null) {
      if (section != null) {
        ptitle.append(section.getName());

        if (topicListForm.getTag() != null) {
          ptitle.append(" - ").append(WordUtils.capitalize(topicListForm.getTag()));
        }
      } else {
        ptitle.append(topicListForm.getTag());
      }
    } else {
      ptitle.append("Архив: ").append(section.getName());

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
  private static String calculateNavTitle(Section section, Group group, TopicListRequest topicListForm)
    throws BadDateException, SectionNotFoundException {

    StringBuilder navTitle = new StringBuilder();

    if (!Strings.isNullOrEmpty(topicListForm.getTag())) {
      navTitle.append(WordUtils.capitalize(topicListForm.getTag()));

      if (section!=null) {
        navTitle.append(" - ");
        navTitle.append(section.getName());
      }
    } else if (group == null) {
      if (section != null) {
        navTitle.setLength(0);
        navTitle.append(section.getName());
      }
    } else if (section != null) {
      navTitle.setLength(0);
      navTitle
        .append("<a href=\"")
        .append(section.getNewsViewerLink())
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

  @ExceptionHandler({UserNotFoundException.class, GroupNotFoundException.class})
  @ResponseStatus(HttpStatus.NOT_FOUND)
  public ModelAndView handleNotFoundException() {
    return new ModelAndView("errors/code404");
  }
}
