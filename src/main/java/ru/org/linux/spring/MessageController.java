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

package ru.org.linux.spring;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.view.RedirectView;
import ru.org.linux.dao.*;
import ru.org.linux.dto.*;
import ru.org.linux.exception.AccessViolationException;
import ru.org.linux.exception.BadInputException;
import ru.org.linux.exception.MessageNotFoundException;
import ru.org.linux.site.*;
import ru.org.linux.util.LorURI;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.net.URLEncoder;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Controller
public class MessageController {
  private static final Log logger = LogFactory.getLog(MessageController.class);
  @Autowired
  private MessageDao messageDao;

  @Autowired
  private PrepareService prepareService;

  @Autowired
  private CommentDao commentDao;

  @Autowired
  private UserDao userDao;

  @Autowired
  private IgnoreListDao ignoreListDao;

  @Autowired
  private SectionDao sectionDao;

  @Autowired
  private GroupDao groupDao;

  @Autowired
  private Configuration configuration;

  @RequestMapping("/forum/{group}/{id}")
  public ModelAndView getMessageNewForum(
      WebRequest webRequest,
      HttpServletRequest request,
      HttpServletResponse response,
      @RequestParam(value = "filter", required = false) String filter,
      @RequestParam(value = "cid", required = false) Integer cid,
      @PathVariable("group") String groupName,
      @PathVariable("id") int msgid
  ) throws Exception {
    if (cid != null) {
      return jumpMessage(request, SectionDto.SECTION_FORUM, groupName, msgid, cid);
    }
    return getMessageNew(SectionDto.SECTION_FORUM, webRequest, request, response, 0, filter, groupName, msgid, null);
  }

  @RequestMapping("/news/{group}/{id}")
  public ModelAndView getMessageNewNews(
      WebRequest webRequest,
      HttpServletRequest request,
      HttpServletResponse response,
      @RequestParam(value = "filter", required = false) String filter,
      @RequestParam(value = "cid", required = false) Integer cid,
      @PathVariable("group") String groupName,
      @PathVariable("id") int msgid
  ) throws Exception {
    if (cid != null) {
      return jumpMessage(request, SectionDto.SECTION_NEWS, groupName, msgid, cid);
    }
    return getMessageNew(SectionDto.SECTION_NEWS, webRequest, request, response, 0, filter, groupName, msgid, null);
  }

  @RequestMapping("/polls/{group}/{id}")
  public ModelAndView getMessageNewPolls(
      WebRequest webRequest,
      HttpServletRequest request,
      HttpServletResponse response,
      @RequestParam(value = "filter", required = false) String filter,
      @RequestParam(value = "cid", required = false) Integer cid,
      @PathVariable("group") String groupName,
      @PathVariable("id") int msgid,
      @RequestParam(value = "highlight", required = false) Set<Integer> highlight
  ) throws Exception {
    if (cid != null) {
      return jumpMessage(request, SectionDto.SECTION_POLLS, groupName, msgid, cid);
    }
    return getMessageNew(
        SectionDto.SECTION_POLLS,
        webRequest,
        request,
        response,
        0,
        filter,
        groupName,
        msgid, highlight);
  }

  @RequestMapping("/gallery/{group}/{id}")
  public ModelAndView getMessageNewGallery(
      WebRequest webRequest,
      HttpServletRequest request,
      HttpServletResponse response,
      @RequestParam(value = "filter", required = false) String filter,
      @RequestParam(value = "cid", required = false) Integer cid,
      @PathVariable("group") String groupName,
      @PathVariable("id") int msgid
  ) throws Exception {
    if (cid != null) {
      return jumpMessage(request, SectionDto.SECTION_GALLERY, groupName, msgid, cid);
    }
    return getMessageNew(SectionDto.SECTION_GALLERY, webRequest, request, response, 0, filter, groupName, msgid, null);
  }

  @RequestMapping("/forum/{group}/{id}/page{page}")
  public ModelAndView getMessageNewForumPage(
      WebRequest webRequest,
      HttpServletRequest request,
      HttpServletResponse response,
      @RequestParam(value = "filter", required = false) String filter,
      @PathVariable("group") String groupName,
      @PathVariable("id") int msgid,
      @PathVariable("page") int page
  ) throws Exception {
    return getMessageNew(SectionDto.SECTION_FORUM, webRequest, request, response, page, filter, groupName, msgid, null);
  }

  @RequestMapping("/news/{group}/{id}/page{page}")
  public ModelAndView getMessageNewNewsPage(
      WebRequest webRequest,
      HttpServletRequest request,
      HttpServletResponse response,
      @RequestParam(value = "filter", required = false) String filter,
      @RequestParam(value = "cid", required = false) Integer cid,
      @PathVariable("group") String groupName,
      @PathVariable("id") int msgid,
      @PathVariable("page") int page
  ) throws Exception {
    return getMessageNew(SectionDto.SECTION_NEWS, webRequest, request, response, page, filter, groupName, msgid, null);
  }

  @RequestMapping("/polls/{group}/{id}/page{page}")
  public ModelAndView getMessageNewPollsPage(
      WebRequest webRequest,
      HttpServletRequest request,
      HttpServletResponse response,
      @RequestParam(value = "filter", required = false) String filter,
      @RequestParam(value = "cid", required = false) Integer cid,
      @PathVariable("group") String groupName,
      @PathVariable("id") int msgid,
      @PathVariable("page") int page
  ) throws Exception {
    return getMessageNew(SectionDto.SECTION_POLLS, webRequest, request, response, page, filter, groupName, msgid, null);
  }

  @RequestMapping("/gallery/{group}/{id}/page{page}")
  public ModelAndView getMessageNewGalleryPage(
      WebRequest webRequest,
      HttpServletRequest request,
      HttpServletResponse response,
      @RequestParam(value = "filter", required = false) String filter,
      @PathVariable("group") String groupName,
      @PathVariable("id") int msgid,
      @PathVariable("page") int page
  ) throws Exception {
    return getMessageNew(SectionDto.SECTION_GALLERY, webRequest, request, response, page, filter, groupName, msgid, null);
  }

  public ModelAndView getMessageNew(
      int section,
      WebRequest webRequest,
      HttpServletRequest request,
      HttpServletResponse response,
      int page,
      String filter,
      String groupName,
      int msgid,
      Set<Integer> highlight)
      throws Exception {
    MessageDto messageDto = messageDao.getById(msgid);
    PreparedMessage preparedMessage = prepareService.prepareMessage(messageDto, false, request.isSecure());
    GroupDto groupDto = preparedMessage.getGroupDto();

    if (!groupDto.getUrlName().equals(groupName) || groupDto.getSectionId() != section) {
      return new ModelAndView(new RedirectView(messageDto.getLink()));
    }

    return getMessage(webRequest, request, response, preparedMessage, groupDto, page, filter, highlight);
  }

  /**
   * Оставлено для старых ссылок /view-message.jsp
   *
   * @param msgid   id топика
   * @param page    страница топика
   * @param lastmod параметр для кэширования
   * @param filter  фильтр
   * @param output  ?
   * @return вовзращает редирект на новый код
   * @throws Exception если получится
   */
  @RequestMapping("/view-message.jsp")
  public ModelAndView getMessageOld(
      @RequestParam("msgid") int msgid,
      @RequestParam(value = "page", required = false) Integer page,
      @RequestParam(value = "lastmod", required = false) Long lastmod,
      @RequestParam(value = "filter", required = false) String filter,
      @RequestParam(required = false) String output
  ) throws Exception {
    MessageDto messageDto = messageDao.getById(msgid);

    StringBuilder link = new StringBuilder(messageDto.getLink());

    StringBuilder params = new StringBuilder();

    if (page != null) {
      link.append("/page").append(page);
    }

    if (lastmod != null && !messageDto.isExpired()) {
      params.append("?lastmod=").append(messageDto.getLastModified().getTime());
    }

    if (filter != null) {
      if (params.length() == 0) {
        params.append('?');
      } else {
        params.append('&');
      }
      params.append("filter=").append(filter);
    }

    if (output != null) {
      if (params.length() == 0) {
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
      PreparedMessage preparedMessage,
      GroupDto groupDto,
      int page,
      String filter,
      Set<Integer> highlight
  ) throws Exception {
    MessageDto messageDto = preparedMessage.getMessage();

    Template tmpl = Template.getTemplate(request);

    Map<String, Object> params = new HashMap<String, Object>();

    params.put("showAdsense", !tmpl.isSessionAuthorized() || !tmpl.getProf().isHideAdsense());

    params.put("page", page);

    params.put("highlight", highlight);

    boolean showDeleted = request.getParameter("deleted") != null;
    if (showDeleted) {
      page = -1;
    }

    boolean rss = request.getParameter("output") != null && "rss".equals(request.getParameter("output"));

    if (showDeleted && !"POST".equals(request.getMethod())) {
      return new ModelAndView(new RedirectView(messageDto.getLink()));
    }

    if (page == -1 && !tmpl.isSessionAuthorized()) {
      return new ModelAndView(new RedirectView(messageDto.getLink()));
    }

    if (showDeleted) {
      if (!tmpl.isSessionAuthorized()) {
        throw new BadInputException("Вы уже вышли из системы");
      }
    }

    params.put("showDeleted", showDeleted);

    UserDto currentUser = tmpl.getCurrentUser();

    if (messageDto.isExpired() && showDeleted && !tmpl.isModeratorSession()) {
      throw new MessageNotFoundException(messageDto.getId(), "нельзя посмотреть удаленные комментарии в устаревших темах");
    }

    checkView(messageDto, tmpl, currentUser);

    params.put("group", groupDto);

    if (groupDto.getCommentsRestriction() == -1 && !Template.isSessionAuthorized(request.getSession())) {
      throw new AccessViolationException("Это сообщение нельзя посмотреть");
    }

    if (!tmpl.isSessionAuthorized()) { // because users have IgnoreList and memories
      String etag = getEtag(messageDto, tmpl);
      response.setHeader("Etag", etag);

      if (request.getHeader("If-None-Match") != null) {
        if (etag.equals(request.getHeader("If-None-Match"))) {
          response.setStatus(HttpServletResponse.SC_NOT_MODIFIED);
          return null;
        }
      } else if (checkLastModified(webRequest, messageDto)) {
        return null;
      }
    }

    params.put("message", messageDto);
    params.put("preparedMessage", preparedMessage);

    params.put("messageMenu", prepareService.getMessageMenu(preparedMessage, currentUser));

    if (messageDto.isExpired()) {
      response.setDateHeader("Expires", System.currentTimeMillis() + 30 * 24 * 60 * 60 * 1000L);
    }

    CommentList comments = commentDao.getCommentList(messageDto, showDeleted);

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
      if (ignoreList == null || ignoreList.isEmpty()) {
        params.put("prevMessage", messageDao.getPreviousMessage(messageDto, null));
        params.put("nextMessage", messageDao.getNextMessage(messageDto, null));
      } else {
        params.put("prevMessage", messageDao.getPreviousMessage(messageDto, currentUser));
        params.put("nextMessage", messageDao.getNextMessage(messageDto, currentUser));
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

      List<CommentDto> commentsFiltred = cv.getComments(reverse, offset, limit, hideSet);

      List<PreparedComment> commentsPrepared = prepareService.prepareCommentList(comments, commentsFiltred, request.isSecure());

      params.put("commentsPrepared", commentsPrepared);
    } else {
      CommentFilter cv = new CommentFilter(comments);

      List<CommentDto> commentsFiltred = cv.getComments(true, 0, MessageTable.RSS_DEFAULT, null);

      List<PreparedComment> commentsPrepared = prepareService.prepareCommentListRSS(comments, commentsFiltred, request.isSecure());

      params.put("commentsPrepared", commentsPrepared);
      LorURI lorURI = new LorURI(configuration.getMainURI(), configuration.getMainUrl());
      params.put("mainURL", lorURI.fixScheme(request.isSecure()));
    }

    return new ModelAndView(rss ? "view-message-rss" : "view-message", params);
  }

  private static void checkView(MessageDto messageDto, Template tmpl, UserDto currentUser) throws MessageNotFoundException {
    if (tmpl.isModeratorSession()) {
      return;
    }

    if (messageDto.isDeleted()) {
      if (messageDto.isExpired()) {
        throw new MessageNotFoundException(messageDto.getId(), "нельзя посмотреть устаревшие удаленные сообщения");
      }

      if (!tmpl.isSessionAuthorized()) {
        throw new MessageNotFoundException(messageDto.getId(), "Сообщение удалено");
      }

      if (currentUser.getId() == messageDto.getUid()) {
        return;
      }

      if (currentUser.getScore() < UserDto.VIEW_DELETED_SCORE) {
        throw new MessageNotFoundException(messageDto.getId(), "Сообщение удалено");
      }
    }
  }

  private static boolean checkLastModified(WebRequest webRequest, MessageDto messageDto) {
    try {
      return webRequest.checkNotModified(messageDto.getLastModified().getTime());
    } catch (IllegalArgumentException ignored) {
      return false;
    }
  }

  private static String getEtag(MessageDto messageDto, Template tmpl) {
    String nick = tmpl.getNick();

    String userAddon = nick != null ? ('-' + nick) : "";

    if (!tmpl.isUsingDefaultProfile()) {
      userAddon += tmpl.getProf().getTimestamp();
    }

    return "msg-" + messageDto.getMessageId() + '-' + messageDto.getLastModified().getTime() + userAddon;
  }

  private ModelAndView jumpMessage(
      HttpServletRequest request,
      int sectionId,
      String groupName,
      int msgid,
      int cid) throws Exception {
    Template tmpl = Template.getTemplate(request);
    MessageDto topic = messageDao.getById(msgid);
    String redirectUrl = topic.getLink();
    StringBuffer options = new StringBuffer();
    SectionDto sectionDto = sectionDao.getSection(sectionId);
    GroupDto groupDto = groupDao.getGroup(sectionDto, groupName);

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

    MessageDto topic = messageDao.getById(msgid);

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
