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

import com.google.common.collect.ImmutableSet;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.view.RedirectView;
import ru.org.linux.auth.AccessViolationException;
import ru.org.linux.auth.IPBlockDao;
import ru.org.linux.auth.IPBlockInfo;
import ru.org.linux.comment.*;
import ru.org.linux.group.Group;
import ru.org.linux.section.Section;
import ru.org.linux.section.SectionScrollModeEnum;
import ru.org.linux.section.SectionService;
import ru.org.linux.site.BadInputException;
import ru.org.linux.site.MessageNotFoundException;
import ru.org.linux.site.Template;
import ru.org.linux.spring.Configuration;
import ru.org.linux.user.IgnoreListDao;
import ru.org.linux.user.User;
import ru.org.linux.util.LorURL;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.net.URLEncoder;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Controller
public class TopicController {
  public static final int RSS_DEFAULT = 20;
  @Autowired
  private SectionService sectionService;

  @Autowired
  private TopicDao messageDao;

  @Autowired
  private CommentPrepareService prepareService;

  @Autowired
  private TopicPrepareService messagePrepareService;

  @Autowired
  private CommentService commentService;

  @Autowired
  private IgnoreListDao ignoreListDao;

  @Autowired
  private Configuration configuration;

  @Autowired
  private IPBlockDao ipBlockDao;

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

  private ModelAndView getMessageNew(
    int section,
    WebRequest webRequest,
    HttpServletRequest request,
    HttpServletResponse response,
    int page,
    String filter,
    String groupName,
    int msgid) throws Exception {
    Topic topic = messageDao.getById(msgid);
    Template tmpl = Template.getTemplate(request);

    PreparedTopic preparedMessage = messagePrepareService.prepareTopic(topic, request.isSecure(), tmpl.getCurrentUser());
    Group group = preparedMessage.getGroup();

    if (!group.getUrlName().equals(groupName) || group.getSectionId() != section) {
      return new ModelAndView(new RedirectView(topic.getLink()));
    }

    Map<String, Object> params = new HashMap<String, Object>();

    boolean showDeleted = request.getParameter("deleted") != null;
    if (showDeleted) {
      page = -1;
    }

    boolean rss = request.getParameter("output") != null && "rss".equals(request.getParameter("output"));

    if (rss && topic.isExpired()) {
      throw new MessageNotFoundException(topic.getId(), "no more comments");
    }

    if (showDeleted && !"POST".equals(request.getMethod())) {
      return new ModelAndView(new RedirectView(topic.getLink()));
    }

    if (page == -1 && !tmpl.isSessionAuthorized()) {
      return new ModelAndView(new RedirectView(topic.getLink()));
    }

    if (showDeleted) {
      if (!tmpl.isSessionAuthorized()) {
        throw new BadInputException("Вы уже вышли из системы");
      }
    }

    params.put("showDeleted", showDeleted);

    User currentUser = tmpl.getCurrentUser();

    if (topic.isExpired() && showDeleted && !tmpl.isModeratorSession()) {
      throw new MessageNotFoundException(topic.getId(), "нельзя посмотреть удаленные комментарии в устаревших темах");
    }

    checkView(topic, tmpl, currentUser);

    if (group.getCommentsRestriction() == -1 && !tmpl.isSessionAuthorized()) {
      throw new AccessViolationException("Это сообщение нельзя посмотреть");
    }

    params.put("message", topic);
    params.put("preparedMessage", preparedMessage);

    if (topic.isExpired()) {
      response.setDateHeader("Expires", System.currentTimeMillis() + 30 * 24 * 60 * 60 * 1000L);
    }

    CommentList comments = commentService.getCommentList(topic, showDeleted);

    if (!rss) {
      params.put("page", page);
      params.put("group", group);
      params.put("showAdsense", !tmpl.isSessionAuthorized() || !tmpl.getProf().isHideAdsense());

      if (!tmpl.isSessionAuthorized()) { // because users have IgnoreList and memories
        String etag = getEtag(topic, tmpl);
        response.setHeader("Etag", etag);

        if (request.getHeader("If-None-Match") != null) {
          if (etag.equals(request.getHeader("If-None-Match"))) {
            response.setStatus(HttpServletResponse.SC_NOT_MODIFIED);
            return null;
          }
        } else if (checkLastModified(webRequest, topic)) {
          return null;
        }
      }

      params.put("messageMenu", messagePrepareService.getTopicMenu(preparedMessage, currentUser));

      Set<Integer> ignoreList = null;
      boolean emptyIgnoreList = true;

      if (currentUser != null) {
        ignoreList = ignoreListDao.get(currentUser);
        emptyIgnoreList = ignoreList.isEmpty();
      }

      int filterMode = CommentFilter.FILTER_IGNORED;

      if (!tmpl.getProf().isShowAnonymous()) {
        filterMode += CommentFilter.FILTER_ANONYMOUS;
      }

      if (emptyIgnoreList) {
        filterMode &= ~CommentFilter.FILTER_IGNORED;
      }

      int defaultFilterMode = filterMode;

      if (filter != null) {
        filterMode = CommentFilter.parseFilterChain(filter);
        if (!emptyIgnoreList && filterMode == CommentFilter.FILTER_ANONYMOUS) {
          filterMode += CommentFilter.FILTER_IGNORED;
        }
      }

      params.put("comments", comments);

      params.put("pages", topic.getPageCount(tmpl.getProf().getMessages()));
      params.put("filterMode", CommentFilter.toString(filterMode));
      params.put("defaultFilterMode", CommentFilter.toString(defaultFilterMode));

      loadTopicScroller(params, topic, currentUser, !emptyIgnoreList);

      Set<Integer> hideSet = commentService.makeHideSet(comments, filterMode, ignoreList);

      CommentFilter cv = new CommentFilter(comments);

      boolean reverse = tmpl.getProf().isShowNewFirst();

      List<Comment> commentsFiltred = cv.getCommentsForPage(reverse, page, tmpl.getProf().getMessages(), hideSet);
      List<Comment> commentsFull = cv.getCommentsForPage(reverse, page, tmpl.getProf().getMessages(), ImmutableSet.<Integer>of());

      params.put("unfilteredCount", commentsFull.size());

      List<PreparedComment> commentsPrepared = prepareService.prepareCommentList(
              comments,
              commentsFiltred,
              request.isSecure(),
              tmpl,
              topic
      );

      params.put("commentsPrepared", commentsPrepared);

      IPBlockInfo ipBlockInfo = ipBlockDao.getBlockInfo(request.getRemoteAddr());
      params.put("ipBlockInfo", ipBlockInfo);
    } else {
      CommentFilter cv = new CommentFilter(comments);

      List<Comment> commentsFiltred = cv.getCommentsForPage(true, 0, RSS_DEFAULT, ImmutableSet.<Integer>of());

      List<PreparedRSSComment> commentsPrepared = prepareService.prepareCommentListRSS(commentsFiltred, request.isSecure());

      params.put("commentsPrepared", commentsPrepared);
      LorURL lorURL = new LorURL(configuration.getMainURI(), configuration.getMainUrl());
      params.put("mainURL", lorURL.fixScheme(request.isSecure()));
    }

    return new ModelAndView(rss ? "view-message-rss" : "view-message", params);
  }

  private void loadTopicScroller(Map<String, Object> params, Topic topic, User currentUser, boolean useIgnoreList) {
    Topic prevMessage;
    Topic nextMessage;

    if (useIgnoreList) {
      prevMessage = messageDao.getPreviousMessage(topic, currentUser);
      nextMessage = messageDao.getNextMessage(topic, currentUser);
    } else {
      prevMessage = messageDao.getPreviousMessage(topic, null);
      nextMessage = messageDao.getNextMessage(topic, null);
    }

    params.put("prevMessage", prevMessage);
    params.put("nextMessage", nextMessage);

    Boolean topScroller;
    SectionScrollModeEnum sectionScroller = sectionService.getScrollMode(topic.getSectionId());

    if (prevMessage == null && nextMessage == null) {
      topScroller = false;
    } else {
      topScroller = sectionScroller != SectionScrollModeEnum.NO_SCROLL;
    }
    params.put("topScroller", topScroller);

    Boolean bottomScroller = sectionScroller != SectionScrollModeEnum.NO_SCROLL;
    params.put("bottomScroller", bottomScroller);
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
    Topic topic = messageDao.getById(msgid);

    StringBuilder link = new StringBuilder(topic.getLink());

    StringBuilder params = new StringBuilder();

    if (page!=null) {
      link.append("/page").append(page);
    }

    if (lastmod!=null && !topic.isExpired()) {
      params.append("?lastmod=").append(topic.getLastModified().getTime());
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
    StringBuilder options = new StringBuilder();

    StringBuilder hash = new StringBuilder();

    CommentList comments = commentService.getCommentList(topic, false);
    CommentNode node = comments.getNode(cid);
    if (node == null) {
      throw new MessageNotFoundException(topic, cid, "Сообщение #" + cid + " было удалено или не существует");
    }

    int pagenum = comments.getCommentPage(node.getComment(), tmpl.getProf());

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
    StringBuilder options = new StringBuilder();

    if (page != null) {
      redirectUrl = topic.getLinkPage(page);
    }

    if (nocache != null) {
      options.append("nocache=");
      options.append(URLEncoder.encode(nocache, "UTF-8"));
    }

    StringBuilder hash = new StringBuilder();

    if (cid != null) {
      CommentList comments = commentService.getCommentList(topic, false);
      CommentNode node = comments.getNode(cid);
      if (node == null) {
        throw new MessageNotFoundException(topic, cid, "Сообщение #" + cid + " было удалено или не существует");
      }

      int pagenum = comments.getCommentPage(node.getComment(), tmpl.getProf());

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

  @ExceptionHandler(MessageNotFoundException.class)
  @ResponseStatus(HttpStatus.NOT_FOUND)
  public ModelAndView handleMessageNotFoundException(MessageNotFoundException ex) {
    if(ex.getTopic() != null) {
      ModelAndView mav = new ModelAndView("errors/good-penguin");
      Topic topic = ex.getTopic();
      mav.addObject("msgTitle", "Ошибка: сообщения не существует");
      mav.addObject("msgHeader", "Сообщение удалено или не существует");
      
      mav.addObject("msgMessage",
          String.format("Сообщение %d в топике <a href=\"%s\">%s</a> удалено или не существует",
          ex.getId(), topic.getLink(), topic.getTitle()));
      return mav;
    } else {
      return new ModelAndView("errors/code404");
    }
  }

}
