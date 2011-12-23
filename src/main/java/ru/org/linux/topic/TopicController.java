/*
 * Copyright 1998-2010 Linux.org.ru
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
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.view.RedirectView;
import ru.org.linux.auth.AccessViolationException;
import ru.org.linux.site.Template;
import ru.org.linux.comment.*;
import ru.org.linux.group.Group;
import ru.org.linux.section.Section;
import ru.org.linux.site.*;
import ru.org.linux.spring.Configuration;
import ru.org.linux.user.IgnoreListDao;
import ru.org.linux.user.UserDao;
import ru.org.linux.user.User;
import ru.org.linux.util.LorURI;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.net.URLEncoder;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Controller
public class TopicController {
  @Autowired
  private TopicDao messageDao;

  @Autowired
  private CommentPrepareService prepareService;

  @Autowired
  private TopicPrepareService messagePrepareService;

  @Autowired
  private CommentDao commentDao;

  @Autowired
  private UserDao userDao;

  @Autowired
  private IgnoreListDao ignoreListDao;

  @Autowired
  private Configuration configuration;

  @RequestMapping("/forum/{group}/{id}")
  public ModelAndView getMessageNewForum(
    WebRequest webRequest,
    HttpServletRequest request,
    HttpServletResponse response,
    @RequestParam(value = "filter", required = false) String filter,
    @RequestParam(value = "cid" , required = false) Integer cid,
    @PathVariable("group") String groupName,
    @PathVariable("id") int msgid
  ) throws Exception {
    if(cid != null) {
      return jumpMessage(request, msgid, cid);
    }
    return getMessageNew(Section.SECTION_FORUM, webRequest, request, response, 0, filter, groupName, msgid);
  }

  @RequestMapping("/news/{group}/{id}")
  public ModelAndView getMessageNewNews(
    WebRequest webRequest,
    HttpServletRequest request,
    HttpServletResponse response,
    @RequestParam(value="filter", required=false) String filter,
    @RequestParam(value = "cid" , required = false) Integer cid,
    @PathVariable("group") String groupName,
    @PathVariable("id") int msgid
  ) throws Exception {
    if(cid != null) {
      return jumpMessage(request, msgid, cid);
    }
    return getMessageNew(Section.SECTION_NEWS, webRequest, request, response, 0, filter, groupName, msgid);
  }

  @RequestMapping("/polls/{group}/{id}")
  public ModelAndView getMessageNewPolls(
    WebRequest webRequest,
    HttpServletRequest request,
    HttpServletResponse response,
    @RequestParam(value="filter", required=false) String filter,
    @RequestParam(value = "cid" , required = false) Integer cid,
    @PathVariable("group") String groupName,
    @PathVariable("id") int msgid
  ) throws Exception {
    if(cid != null) {
      return jumpMessage(request, msgid, cid);
    }
    return getMessageNew(
      Section.SECTION_POLLS,
      webRequest,
      request,
      response,
      0,
      filter,
      groupName,
      msgid);
  }

  @RequestMapping("/gallery/{group}/{id}")
  public ModelAndView getMessageNewGallery(
    WebRequest webRequest,
    HttpServletRequest request,
    HttpServletResponse response,
    @RequestParam(value="filter", required=false) String filter,
    @RequestParam(value = "cid" , required = false) Integer cid,
    @PathVariable("group") String groupName,
    @PathVariable("id") int msgid
  ) throws Exception {
    if(cid != null) {
      return jumpMessage(request, msgid, cid);
    }
    return getMessageNew(Section.SECTION_GALLERY, webRequest, request, response, 0, filter, groupName, msgid);
  }

  @RequestMapping("/forum/{group}/{id}/page{page}")
  public ModelAndView getMessageNewForumPage(
    WebRequest webRequest,
    HttpServletRequest request,
    HttpServletResponse response,
    @RequestParam(value="filter", required=false) String filter,
    @PathVariable("group") String groupName,
    @PathVariable("id") int msgid,
    @PathVariable("page") int page
  ) throws Exception {
    return getMessageNew(Section.SECTION_FORUM, webRequest, request, response, page, filter, groupName, msgid);
  }

  @RequestMapping("/news/{group}/{id}/page{page}")
  public ModelAndView getMessageNewNewsPage(
    WebRequest webRequest,
    HttpServletRequest request,
    HttpServletResponse response,
    @RequestParam(value="filter", required=false) String filter,
    @RequestParam(value = "cid" , required = false) Integer cid,
    @PathVariable("group") String groupName,
    @PathVariable("id") int msgid,
    @PathVariable("page") int page
  ) throws Exception {
    return getMessageNew(Section.SECTION_NEWS, webRequest, request, response, page, filter, groupName, msgid);
  }

  @RequestMapping("/polls/{group}/{id}/page{page}")
  public ModelAndView getMessageNewPollsPage(
    WebRequest webRequest,
    HttpServletRequest request,
    HttpServletResponse response,
    @RequestParam(value="filter", required=false) String filter,
    @RequestParam(value = "cid" , required = false) Integer cid,
    @PathVariable("group") String groupName,
    @PathVariable("id") int msgid,
    @PathVariable("page") int page
  ) throws Exception {
    return getMessageNew(Section.SECTION_POLLS, webRequest, request, response, page, filter, groupName, msgid);
  }

  @RequestMapping("/gallery/{group}/{id}/page{page}")
  public ModelAndView getMessageNewGalleryPage(
    WebRequest webRequest,
    HttpServletRequest request,
    HttpServletResponse response,
    @RequestParam(value="filter", required=false) String filter,
    @PathVariable("group") String groupName,
    @PathVariable("id") int msgid,
    @PathVariable("page") int page
  ) throws Exception {
    return getMessageNew(Section.SECTION_GALLERY, webRequest, request, response, page, filter, groupName, msgid);
  }

  public ModelAndView getMessageNew(
    int section,
    WebRequest webRequest,
    HttpServletRequest request,
    HttpServletResponse response,
    int page,
    String filter,
    String groupName,
    int msgid) throws Exception {
    Topic message = messageDao.getById(msgid);
    Template tmpl = Template.getTemplate(request);
    User user = null;
    if(tmpl.isSessionAuthorized()) {
      user = tmpl.getCurrentUser();
    }
    PreparedTopic preparedMessage = messagePrepareService.prepareTopicForView(message, false, request.isSecure(), user);
    Group group = preparedMessage.getGroup();

    if (!group.getUrlName().equals(groupName) || group.getSectionId() != section) {
      return new ModelAndView(new RedirectView(message.getLink()));
    }

    return getMessage(webRequest, request, response, preparedMessage, group, page, filter);
  }

  /**
   * Оставлено для старых ссылок /view-message.jsp
   * @param msgid id топика
   * @param page страница топика
   * @param lastmod параметр для кэширования
   * @param filter фильтр
   * @param output ?
   * @return вовзращает редирект на новый код
   * @throws Exception если получится
   */
  @RequestMapping("/view-message.jsp")
  public ModelAndView getMessageOld(
    @RequestParam("msgid") int msgid,
    @RequestParam(value="page", required=false) Integer page,
    @RequestParam(value="lastmod", required=false) Long lastmod,
    @RequestParam(value="filter", required=false) String filter,
    @RequestParam(required=false) String output
  ) throws Exception {
    Topic message = messageDao.getById(msgid);

    StringBuilder link = new StringBuilder(message.getLink());

    StringBuilder params = new StringBuilder();

    if (page!=null) {
      link.append("/page").append(page);
    }

    if (lastmod!=null && !message.isExpired()) {
      params.append("?lastmod=").append(message.getLastModified().getTime());
    }

    if (filter!=null) {
      if (params.length()==0) {
        params.append('?');
      } else {
        params.append('&');
      }
      params.append("filter=").append(filter);
    }

    if (output!=null) {
      if (params.length()==0) {
        params.append('?');
      } else {
        params.append('&');
      }
      params.append("output=").append(output);
    }

    link.append(params);

    return new ModelAndView(new RedirectView(link.toString()));
  }

  private ModelAndView getMessage(
    WebRequest webRequest,
    HttpServletRequest request,
    HttpServletResponse response,
    PreparedTopic preparedMessage,
    Group group,
    int page,
    String filter
  ) throws Exception {
    Topic message = preparedMessage.getMessage();

    Template tmpl = Template.getTemplate(request);

    Map<String, Object> params = new HashMap<String, Object>();

    params.put("showAdsense", !tmpl.isSessionAuthorized() || !tmpl.getProf().isHideAdsense());

    params.put("page", page);

    boolean showDeleted = request.getParameter("deleted") != null;
    if (showDeleted) {
      page = -1;
    }

    boolean rss = request.getParameter("output") != null && "rss".equals(request.getParameter("output"));

    if (showDeleted && !"POST".equals(request.getMethod())) {
      return new ModelAndView(new RedirectView(message.getLink()));
    }

    if (page == -1 && !tmpl.isSessionAuthorized()) {
      return new ModelAndView(new RedirectView(message.getLink()));
    }

    if (showDeleted) {
      if (!tmpl.isSessionAuthorized()) {
        throw new BadInputException("Вы уже вышли из системы");
      }
    }

    params.put("showDeleted", showDeleted);

    User currentUser = tmpl.getCurrentUser();

    if (message.isExpired() && showDeleted && !tmpl.isModeratorSession()) {
      throw new MessageNotFoundException(message.getId(), "нельзя посмотреть удаленные комментарии в устаревших темах");
    }

    checkView(message, tmpl, currentUser);

    params.put("group", group);

    if (group.getCommentsRestriction() == -1 && !Template.isSessionAuthorized(request.getSession())) {
      throw new AccessViolationException("Это сообщение нельзя посмотреть");
    }

    if (!tmpl.isSessionAuthorized()) { // because users have IgnoreList and memories
      String etag = getEtag(message, tmpl);
      response.setHeader("Etag", etag);

      if (request.getHeader("If-None-Match") != null) {
        if (etag.equals(request.getHeader("If-None-Match"))) {
          response.setStatus(HttpServletResponse.SC_NOT_MODIFIED);
          return null;
        }
      } else if (checkLastModified(webRequest, message)) {
        return null;
      }
    }

    params.put("message", message);
    params.put("preparedMessage", preparedMessage);

    params.put("messageMenu", messagePrepareService.getMessageMenu(preparedMessage, currentUser));

    if (message.isExpired()) {
      response.setDateHeader("Expires", System.currentTimeMillis() + 30 * 24 * 60 * 60 * 1000L);
    }

    CommentList comments = commentDao.getCommentList(message, showDeleted);

    params.put("comments", comments);

    Set<Integer> ignoreList = null;

    if (currentUser != null) {
      ignoreList = ignoreListDao.get(currentUser);
    }

    int filterMode = CommentFilter.FILTER_IGNORED;

    if (!tmpl.getProf().isShowAnonymous()) {
      filterMode += CommentFilter.FILTER_ANONYMOUS;
    }

    if (ignoreList == null || ignoreList.isEmpty()) {
      filterMode = filterMode & ~CommentFilter.FILTER_IGNORED;
    }

    int defaultFilterMode = filterMode;

    if (filter != null) {
      filterMode = CommentFilter.parseFilterChain(filter);
      if (ignoreList != null && filterMode == CommentFilter.FILTER_ANONYMOUS) {
        filterMode += CommentFilter.FILTER_IGNORED;
      }
    }

    params.put("filterMode", filterMode);
    params.put("defaultFilterMode", defaultFilterMode);

    if (!rss) {
      if (ignoreList==null || ignoreList.isEmpty()) {
        params.put("prevMessage", messageDao.getPreviousMessage(message, null));
        params.put("nextMessage", messageDao.getNextMessage(message, null));
      } else {
        params.put("prevMessage", messageDao.getPreviousMessage(message, currentUser));
        params.put("nextMessage", messageDao.getNextMessage(message, currentUser));
      }

      Set<Integer> hideSet = CommentList.makeHideSet(userDao, comments, filterMode, ignoreList);

      CommentFilter cv = new CommentFilter(comments);

      boolean reverse = tmpl.getProf().isShowNewFirst();
      int offset = 0;
      int limit = 0;
      int messages = tmpl.getProf().getMessages();

      if (page != -1) {
        limit = messages;
        offset = messages * page;
      }

      List<Comment> commentsFiltred = cv.getComments(reverse, offset, limit, hideSet);

      List<PreparedComment> commentsPrepared = prepareService.prepareCommentList(comments, commentsFiltred, request.isSecure());

      params.put("commentsPrepared", commentsPrepared);
    } else {
      CommentFilter cv = new CommentFilter(comments);

      List<Comment> commentsFiltred = cv.getComments(true, 0, TopicRssHelper.RSS_DEFAULT, null);

      List<PreparedComment> commentsPrepared = prepareService.prepareCommentListRSS(comments, commentsFiltred, request.isSecure());

      params.put("commentsPrepared", commentsPrepared);
      LorURI lorURI = new LorURI(configuration.getMainURI(), configuration.getMainUrl());
      params.put("mainURL", lorURI.fixScheme(request.isSecure()));
    }

    return new ModelAndView(rss ? "view-message-rss" : "view-message", params);
  }

  private static void checkView(Topic message, Template tmpl, User currentUser) throws MessageNotFoundException {
    if (tmpl.isModeratorSession()) {
      return;
    }

    if (message.isDeleted()) {
      if (message.isExpired()) {
        throw new MessageNotFoundException(message.getId(), "нельзя посмотреть устаревшие удаленные сообщения");
      }

      if (!tmpl.isSessionAuthorized()) {
        throw new MessageNotFoundException(message.getId(), "Сообщение удалено");
      }

      if (currentUser.getId() == message.getUid()) {
        return;
      }

      if (currentUser.getScore() < User.VIEW_DELETED_SCORE) {
        throw new MessageNotFoundException(message.getId(), "Сообщение удалено");
      }
    }
  }

  private static boolean checkLastModified(WebRequest webRequest, Topic message) {
    try {
      return webRequest.checkNotModified(message.getLastModified().getTime());
    } catch (IllegalArgumentException ignored) {
      return false;
    }
  }

  private static String getEtag(Topic message, Template tmpl) {
    String nick = tmpl.getNick();

    String userAddon = nick!=null?('-' +nick):"";

    if (!tmpl.isUsingDefaultProfile()) {
      userAddon+=tmpl.getProf().getTimestamp();
    }

    return "msg-"+message.getMessageId()+ '-' +message.getLastModified().getTime()+userAddon;
  }

  private ModelAndView jumpMessage(
          HttpServletRequest request,
          int msgid,
          int cid) throws Exception {
    Template tmpl = Template.getTemplate(request);
    Topic topic = messageDao.getById(msgid);
    String redirectUrl = topic.getLink();
    StringBuffer options = new StringBuffer();

    StringBuilder hash = new StringBuilder();

    CommentList comments = commentDao.getCommentList(topic, false);
    CommentNode node = comments.getNode(cid);
    if (node == null) {
      throw new MessageNotFoundException(cid, "Сообщение #" + cid + " было удалено или не существует");
    }

    int pagenum = comments.getCommentPage(node.getComment(), tmpl);

    if (pagenum > 0) {
      redirectUrl = topic.getLinkPage(pagenum);
    }

    if (!topic.isExpired() && topic.getPageCount(tmpl.getProf().getMessages()) - 1 == pagenum) {
      if (options.length() > 0) {
        options.append('&');
      }
      options.append("lastmod=");
      options.append(topic.getLastModified().getTime());
    }

    hash.append("#comment-");
    hash.append(cid);

    if (options.length() > 0) {
      return new ModelAndView(new RedirectView(redirectUrl + '?' + options + hash));
    } else {
      return new ModelAndView(new RedirectView(redirectUrl + hash));
    }
  }


  @RequestMapping(value = "/jump-message.jsp", method = {RequestMethod.GET, RequestMethod.HEAD})
  public ModelAndView jumpMessage(
          HttpServletRequest request,
          @RequestParam int msgid,
          @RequestParam(required = false) Integer page,
          @RequestParam(required = false) String nocache,
          @RequestParam(required = false) Integer cid
  ) throws Exception {
    Template tmpl = Template.getTemplate(request);

    Topic topic = messageDao.getById(msgid);

    String redirectUrl = topic.getLink();
    StringBuffer options = new StringBuffer();

    if (page != null) {
      redirectUrl = topic.getLinkPage(page);
    }

    if (nocache != null) {
      options.append("nocache=");
      options.append(URLEncoder.encode(nocache));
    }

    StringBuilder hash = new StringBuilder();

    if (cid != null) {
      CommentList comments = commentDao.getCommentList(topic, false);
      CommentNode node = comments.getNode(cid);
      if (node == null) {
        throw new MessageNotFoundException(cid, "Сообщение #" + cid + " было удалено или не существует");
      }

      int pagenum = comments.getCommentPage(node.getComment(), tmpl);

      if (pagenum > 0) {
        redirectUrl = topic.getLinkPage(pagenum);
      }

      if (!topic.isExpired() && topic.getPageCount(tmpl.getProf().getMessages()) - 1 == pagenum) {
        if (options.length() > 0) {
          options.append('&');
        }
        options.append("lastmod=");
        options.append(topic.getLastModified().getTime());
      }

      hash.append("#comment-");
      hash.append(cid);
    }

    if (options.length() > 0) {
      return new ModelAndView(new RedirectView(redirectUrl + '?' + options + hash));
    } else {
      return new ModelAndView(new RedirectView(redirectUrl + hash));
    }
  }
}
