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
import java.util.Map;
import java.util.Set;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.view.RedirectView;

import ru.org.linux.site.*;

@Controller
public class MessageController {
  @RequestMapping("/view-message.jsp")
  public ModelAndView getMessage(
    WebRequest webRequest,
    HttpServletRequest request,
    HttpServletResponse response,
    @RequestParam("msgid") int msgid,
    @RequestParam(value="page", required=false) Integer page,
    @RequestParam(value="filter", required=false) String filter
  ) throws Exception {
    Template tmpl = Template.getTemplate(request);

    Map<String, Object> params = new HashMap<String, Object>();

    params.put("showAdsense", !tmpl.isSessionAuthorized() || !tmpl.getProf().getBoolean(DefaultProfile.HIDE_ADSENSE));

    params.put("msgid", msgid);

    if (page!=null) {
      params.put("page", page);
    }

    boolean showDeleted = request.getParameter("deleted") != null;
    boolean rss = request.getParameter("output")!=null && "rss".equals(request.getParameter("output"));

    if (showDeleted && !"POST".equals(request.getMethod())) {
      return new ModelAndView(new RedirectView("view-message.jsp?msgid=" + msgid));
    }

    if (showDeleted) {
      if (!Template.isSessionAuthorized(request.getSession())) {
        throw new BadInputException("Вы уже вышли из системы");
      }
    }

    params.put("showDeleted", showDeleted);

    Connection db = null;

    try {
      db = LorDataSource.getConnection();

      Message message = new Message(db, msgid);

      if (message.isExpired() && showDeleted && !tmpl.isModeratorSession()) {
        throw new MessageNotFoundException(message.getId(), "нельзя посмотреть удаленные комментарии в устаревших темах");
      }

      if (message.isExpired() && message.isDeleted() && !tmpl.isModeratorSession()) {
        throw new MessageNotFoundException(message.getId(), "нельзя посмотреть устаревшие удаленные сообщения");
      }

      if (message.isDeleted() && !Template.isSessionAuthorized(request.getSession())) {
        throw new MessageNotFoundException(message.getId(), "Сообщение удалено");
      }

      if (new Group(db, message.getGroupId()).getCommentsRestriction()==-1 && !Template.isSessionAuthorized(request.getSession())) {
        throw new AccessViolationException("Это сообщение нельзя посмотреть");
      }

      String etag = getEtag(message, tmpl);
      response.setHeader("Etag", etag);

      if (request.getHeader("If-None-Match")!=null) {
        if (etag.equals(request.getHeader("If-None-Match"))) {
          response.setStatus(HttpServletResponse.SC_NOT_MODIFIED);
          return null;
        }
      } else if (webRequest.checkNotModified(message.getLastModified().getTime())) {
        return null;
      }

      params.put("message", message);

      if (message.isExpired()) {
        response.setDateHeader("Expires", System.currentTimeMillis() + 30 * 24 * 60 * 60 * 1000L);
      }

      params.put("prevMessage", message.getPreviousMessage(db));
      params.put("nextMessage", message.getNextMessage(db));

      CommentList comments = CommentList.getCommentList(db, message, showDeleted);

      params.put("comments", comments);

      String nick = Template.getNick(request.getSession());

      Map<Integer, String> ignoreList = null;

      if (nick!=null) {
        ignoreList = IgnoreList.getIgnoreList(db, nick);
      }

      int filterMode = CommentFilter.FILTER_IGNORED;

      if (!tmpl.getProf().getBoolean("showanonymous")) {
        filterMode += CommentFilter.FILTER_ANONYMOUS;
      }

      if (ignoreList==null || ignoreList.isEmpty()) {
        filterMode = filterMode & ~CommentFilter.FILTER_IGNORED;
      }

      int defaultFilterMode = filterMode;

      if (filter != null) {
        filterMode = CommentFilter.parseFilterChain(filter);
      }

      params.put("filterMode", filterMode);
      params.put("defaultFilterMode", defaultFilterMode);

      Set<Integer> hideSet = CommentList.makeHideSet(db, comments, filterMode, ignoreList);
      params.put("hideSet", hideSet);

      return new ModelAndView(rss?"view-message-rss":"view-message", params);
    } finally {
      if (db!=null) {
        db.close();
      }
    }
  }

  private String getEtag(Message message, Template tmpl) {
    String nick = tmpl.getNick();

    String userAddon = nick!=null?('-' +nick):"";

    if (!tmpl.isUsingDefaultProfile()) {
      userAddon+=tmpl.getProf().getLong(Profile.SYSTEM_TIMESTAMP);
    }

    return "msg-"+message.getMessageId()+ '-' +message.getLastModified().getTime()+userAddon;
  }

  @RequestMapping(value="/jump-message.jsp", method=RequestMethod.GET)
  public ModelAndView jumpMessage(
    HttpServletRequest request,
    @RequestParam int msgid,
    @RequestParam(required=false) Integer page,
    @RequestParam(required=false) String nocache,
    @RequestParam(required=false) Integer cid
  ) throws Exception {
    Template tmpl = Template.getTemplate(request);

    String redirectUrl = "/view-message.jsp?msgid=" + msgid;
    StringBuffer options = new StringBuffer();

    if (page != null) {
      options.append("&page=");
      options.append(URLEncoder.encode(page.toString()));
    }

    if (nocache != null) {
      options.append("&nocache=");
      options.append(URLEncoder.encode(nocache));
    }

    if (cid != null) {
      Connection db = null;

      try {
        db = LorDataSource.getConnection();
        Message topic = new Message(db, msgid);
        CommentList comments = CommentList.getCommentList(db, topic, false);
        CommentNode node = comments.getNode(cid);
        if (node == null) {
          throw new MessageNotFoundException(cid, "Сообщение #" + cid + " было удалено или не существует");
        }

        int pagenum = comments.getCommentPage(node.getComment(), tmpl);

        if (pagenum > 0) {
          options.append("&page=");
          options.append(pagenum);
        }

        if (!topic.isExpired() && topic.getPageCount(tmpl.getProf().getInt("messages"))-1==pagenum) {
          options.append("&lastmod=");
          options.append(topic.getLastModified().getTime());
        }

        options.append("#comment-");
        options.append(cid);
      } finally {
        if (db != null) {
          db.close();
        }
      }
    }

    return new ModelAndView(new RedirectView(redirectUrl + options.toString()));
  }
}
