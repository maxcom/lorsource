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

import java.net.URLEncoder;
import java.sql.Connection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.jdbc.support.JdbcUtils;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.view.RedirectView;
import org.springframework.beans.factory.annotation.Autowired;

import ru.org.linux.site.*;

@Controller
public class MessageController {
  private SectionStore sectionStore;

  @Autowired
  public void setSectionStore(SectionStore sectionStore) {
    this.sectionStore = sectionStore;
  }

  @RequestMapping("/forum/{group}/{id}")
  public ModelAndView getMessageNewForum(
    WebRequest webRequest,
    HttpServletRequest request,
    HttpServletResponse response,
    @RequestParam(value="filter", required=false) String filter,
    @PathVariable("group") String groupName,
    @PathVariable("id") int msgid
  ) throws Exception {
    return getMessageNew(Section.SECTION_FORUM, webRequest, request, response, 0, filter, groupName, msgid, null);
  }

  @RequestMapping("/news/{group}/{id}")
  public ModelAndView getMessageNewNews(
    WebRequest webRequest,
    HttpServletRequest request,
    HttpServletResponse response,
    @RequestParam(value="filter", required=false) String filter,
    @PathVariable("group") String groupName,
    @PathVariable("id") int msgid
  ) throws Exception {
    return getMessageNew(Section.SECTION_NEWS, webRequest, request, response, 0, filter, groupName, msgid, null);
  }

  @RequestMapping("/polls/{group}/{id}")
  public ModelAndView getMessageNewPolls(
    WebRequest webRequest,
    HttpServletRequest request,
    HttpServletResponse response,
    @RequestParam(value="filter", required=false) String filter,
    @PathVariable("group") String groupName,
    @PathVariable("id") int msgid,
    @RequestParam(value="highlight", required=false) Set<Integer> highlight
  ) throws Exception {
    return getMessageNew(
      Section.SECTION_POLLS,
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
    @RequestParam(value="filter", required=false) String filter,
    @PathVariable("group") String groupName,
    @PathVariable("id") int msgid
  ) throws Exception {
    return getMessageNew(Section.SECTION_GALLERY, webRequest, request, response, 0, filter, groupName, msgid, null);
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
    return getMessageNew(Section.SECTION_FORUM, webRequest, request, response, page, filter, groupName, msgid, null);
  }

  @RequestMapping("/news/{group}/{id}/page{page}")
  public ModelAndView getMessageNewNewsPage(
    WebRequest webRequest,
    HttpServletRequest request,
    HttpServletResponse response,
    @RequestParam(value="filter", required=false) String filter,
    @PathVariable("group") String groupName,
    @PathVariable("id") int msgid,
    @PathVariable("page") int page
  ) throws Exception {
    return getMessageNew(Section.SECTION_NEWS, webRequest, request, response, page, filter, groupName, msgid, null);
  }

  @RequestMapping("/polls/{group}/{id}/page{page}")
  public ModelAndView getMessageNewPollsPage(
    WebRequest webRequest,
    HttpServletRequest request,
    HttpServletResponse response,
    @RequestParam(value="filter", required=false) String filter,
    @PathVariable("group") String groupName,
    @PathVariable("id") int msgid,
    @PathVariable("page") int page
  ) throws Exception {
    return getMessageNew(Section.SECTION_POLLS, webRequest, request, response, page, filter, groupName, msgid, null);
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
    return getMessageNew(Section.SECTION_GALLERY, webRequest, request, response, page, filter, groupName, msgid, null);
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
    Set<Integer> highlight) throws Exception {
    Connection db = null;
    try {
      db = LorDataSource.getConnection();

      Message message = new Message(db, msgid);
      PreparedMessage preparedMessage = new PreparedMessage(db, message, true);
      Group group = preparedMessage.getGroup();

      if (!group.getUrlName().equals(groupName) || group.getSectionId() != section) {
        return new ModelAndView(new RedirectView(message.getLink()));
      }

      return getMessage(db, webRequest, request, response, preparedMessage, group, page, filter, highlight);
    } finally {
      JdbcUtils.closeConnection(db);
    }
  }

  @RequestMapping("/view-message.jsp")
  public ModelAndView getMessageOld(
    @RequestParam("msgid") int msgid,
    @RequestParam(value="page", required=false) Integer page,
    @RequestParam(value="lastmod", required=false) Long lastmod,
    @RequestParam(value="filter", required=false) String filter,
    @RequestParam(required=false) String output
  ) throws Exception {
    Connection db = null;

    try {
      db = LorDataSource.getConnection();

      Message message = new Message(db, sectionStore, msgid);

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
    } finally {
      if (db!=null) {
        db.close();
      }
    }
  }

  private ModelAndView getMessage(
    Connection db,
    WebRequest webRequest,
    HttpServletRequest request,
    HttpServletResponse response,
    PreparedMessage preparedMessage,
    Group group,
    int page,
    String filter,
    Set<Integer> highlight
  ) throws Exception {
    Message message = preparedMessage.getMessage();

    Template tmpl = Template.getTemplate(request);

    Map<String, Object> params = new HashMap<String, Object>();

    params.put("showAdsense", !tmpl.isSessionAuthorized() || !tmpl.getProf().getBoolean(DefaultProfile.HIDE_ADSENSE));

    params.put("page", page);

    params.put("highlight", highlight);

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

    tmpl.initCurrentUser(db);
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
    params.put("messageMenu", new MessageMenu(db, preparedMessage, currentUser));

    if (message.isExpired()) {
      response.setDateHeader("Expires", System.currentTimeMillis() + 30 * 24 * 60 * 60 * 1000L);
    }

    CommentList comments = CommentList.getCommentList(db, message, showDeleted);

    params.put("comments", comments);

    Map<Integer, String> ignoreList = null;

    if (currentUser != null) {
      ignoreList = IgnoreList.getIgnoreList(db, currentUser.getId());
    }

    int filterMode = CommentFilter.FILTER_IGNORED;

    if (!tmpl.getProf().getBoolean("showanonymous")) {
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
      params.put("prevMessage", message.getPreviousMessage(db, sectionStore));
      params.put("nextMessage", message.getNextMessage(db, sectionStore));

      Set<Integer> hideSet = CommentList.makeHideSet(db, comments, filterMode, ignoreList);

      CommentFilter cv = new CommentFilter(comments);

      boolean reverse = tmpl.getProf().getBoolean("newfirst");
      int offset = 0;
      int limit = 0;
      int messages = tmpl.getProf().getInt("messages");

      if (page != -1) {
        limit = messages;
        offset = messages * page;
      }

      List<Comment> commentsFiltred = cv.getComments(reverse, offset, limit, hideSet);

      List<PreparedComment> commentsPrepared = PreparedComment.prepare(db, comments, commentsFiltred);

      params.put("commentsPrepared", commentsPrepared);
    } else {
      CommentFilter cv = new CommentFilter(comments);

      List<Comment> commentsFiltred = cv.getComments(true, 0, MessageTable.RSS_DEFAULT, null);

      List<PreparedComment> commentsPrepared = PreparedComment.prepare(db, comments, commentsFiltred);

      params.put("commentsPrepared", commentsPrepared);
    }

    return new ModelAndView(rss ? "view-message-rss" : "view-message", params);
  }

  private static void checkView(Message message, Template tmpl, User currentUser) throws MessageNotFoundException {
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

  private static boolean checkLastModified(WebRequest webRequest, Message message) {
    try {
      return webRequest.checkNotModified(message.getLastModified().getTime());
    } catch (IllegalArgumentException ignored) {
      return false;
    }
  }

  private static String getEtag(Message message, Template tmpl) {
    String nick = tmpl.getNick();

    String userAddon = nick!=null?('-' +nick):"";

    if (!tmpl.isUsingDefaultProfile()) {
      userAddon+=tmpl.getProf().getLong(Profile.SYSTEM_TIMESTAMP);
    }

    return "msg-"+message.getMessageId()+ '-' +message.getLastModified().getTime()+userAddon;
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

    Connection db = null;
    try {
      db = LorDataSource.getConnection();

      Message topic = new Message(db, msgid);

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
        CommentList comments = CommentList.getCommentList(db, topic, false);
        CommentNode node = comments.getNode(cid);
        if (node == null) {
          throw new MessageNotFoundException(cid, "Сообщение #" + cid + " было удалено или не существует");
        }

        int pagenum = comments.getCommentPage(node.getComment(), tmpl);

        if (pagenum > 0) {
          redirectUrl = topic.getLinkPage(pagenum);
        }

        if (!topic.isExpired() && topic.getPageCount(tmpl.getProf().getInt("messages")) - 1 == pagenum) {
          if (options.length()>0) {
            options.append('&');
          }
          options.append("lastmod=");
          options.append(topic.getLastModified().getTime());
        }

        hash.append("#comment-");
        hash.append(cid);
      }

      if (options.length()>0) {
        return new ModelAndView(new RedirectView(redirectUrl + '?'+ options+hash));
      } else {
        return new ModelAndView(new RedirectView(redirectUrl+hash));
      }
    } finally {
      if (db != null) {
        db.close();
      }
    }
  }
}
