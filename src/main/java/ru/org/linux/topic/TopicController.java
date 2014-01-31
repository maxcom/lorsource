/*
 * Copyright 1998-2013 Linux.org.ru
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
import org.elasticsearch.action.ListenableActionFuture;
import org.elasticsearch.action.search.SearchResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.view.RedirectView;
import ru.org.linux.auth.AuthUtil;
import ru.org.linux.auth.IPBlockDao;
import ru.org.linux.auth.IPBlockInfo;
import ru.org.linux.comment.*;
import ru.org.linux.group.Group;
import ru.org.linux.paginator.PagesInfo;
import ru.org.linux.search.MoreLikeThisService;
import ru.org.linux.section.Section;
import ru.org.linux.section.SectionScrollModeEnum;
import ru.org.linux.section.SectionService;
import ru.org.linux.site.BadInputException;
import ru.org.linux.site.MessageNotFoundException;
import ru.org.linux.site.Template;
import ru.org.linux.spring.SiteConfig;
import ru.org.linux.spring.dao.MessageText;
import ru.org.linux.spring.dao.MsgbaseDao;
import ru.org.linux.tag.TagRef;
import ru.org.linux.user.IgnoreListDao;
import ru.org.linux.user.Profile;
import ru.org.linux.user.User;
import ru.org.linux.util.LorURL;
import ru.org.linux.util.bbcode.LorCodeService;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.*;

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
  private SiteConfig siteConfig;

  @Autowired
  private IPBlockDao ipBlockDao;

  @Autowired
  private TopicPermissionService permissionService;

  @Autowired
  private MoreLikeThisService moreLikeThisService;

  @Autowired
  private TopicTagService topicTagService;

  @Autowired
  private MsgbaseDao msgbaseDao;

  @Autowired
  private LorCodeService lorCodeService;

  @RequestMapping("/{section:(?:forum)|(?:news)|(?:polls)|(?:gallery)}/{group}/{id}")
  public ModelAndView getMessageNewMain(
    WebRequest webRequest,
    HttpServletRequest request,
    HttpServletResponse response,
    @RequestParam(value = "filter", required = false) String filter,
    @RequestParam(value = "cid" , required = false) Integer cid,
    @PathVariable("section") String sectionName,
    @PathVariable("group") String groupName,
    @PathVariable("id") int msgid
  ) throws Exception {
    if(cid != null) {
      return jumpMessage(request, msgid, cid);
    }

    Section section = sectionService.getSectionByName(sectionName);

    boolean rss = request.getParameter("output") != null && "rss".equals(request.getParameter("output"));

    if (rss) {
      return getMessageRss(section, request, response, groupName, msgid);
    } else {
      return getMessageNew(section, webRequest, request, response, 0, filter, groupName, msgid);
    }
  }

  @RequestMapping("/{section:(?:forum)|(?:news)|(?:polls)|(?:gallery)}/{group}/{id}/page{page}")
  public ModelAndView getMessageNewPage(
    WebRequest webRequest,
    HttpServletRequest request,
    HttpServletResponse response,
    @RequestParam(value="filter", required=false) String filter,
    @PathVariable("section") String sectionName,
    @PathVariable("group") String groupName,
    @PathVariable("id") int msgid,
    @PathVariable("page") int page
  ) throws Exception {
    Section section = sectionService.getSectionByName(sectionName);

    return getMessageNew(section, webRequest, request, response, page, filter, groupName, msgid);
  }

  private static int getDefaultFilter(Profile prof, boolean emptyIgnoreList) {
    int filterMode = CommentFilter.FILTER_IGNORED;

    if (!prof.isShowAnonymous()) {
      filterMode += CommentFilter.FILTER_ANONYMOUS;
    }

    if (emptyIgnoreList) {
      filterMode &= ~CommentFilter.FILTER_IGNORED;
    }

    return filterMode;
  }

  private static PagesInfo buildPages(Topic topic, int messagesPerPage, int filterMode, int defaultFilterMode, int currentPage) {
    TopicLinkBuilder base = TopicLinkBuilder.baseLink(topic).lastmod(messagesPerPage);

    if (filterMode!=defaultFilterMode) {
      base = base.filter(filterMode);
    }

    List<String> out = new ArrayList<>();

    for (int i=0; i<topic.getPageCount(messagesPerPage); i++) {
      out.add(base.page(i).build());
    }

    return new PagesInfo(out, currentPage);
  }

  private ModelAndView getMessageNew(
    Section section,
    WebRequest webRequest,
    HttpServletRequest request,
    HttpServletResponse response,
    int page,
    String filter,
    String groupName,
    int msgid) throws Exception {
    Topic topic = messageDao.getById(msgid);
    Template tmpl = Template.getTemplate(request);

    Map<String, Object> params = new HashMap<>();

    List<TagRef> tags = topicTagService.getTagRefs(topic);

    ListenableActionFuture<SearchResponse> moreLikeThis = null;

    MessageText messageText = msgbaseDao.getMessageText(topic.getId());
    String plainText = lorCodeService.extractPlainText(messageText);

    if (tmpl.getCurrentUser() != null) {
      moreLikeThis = moreLikeThisService.search(topic, tags, plainText);
    }

    PreparedTopic preparedMessage = messagePrepareService.prepareTopic(
            topic,
            tags,
            request.isSecure(),
            tmpl.getCurrentUser(),
            messageText
    );

    Group group = preparedMessage.getGroup();

    if (!group.getUrlName().equals(groupName) || group.getSectionId() != section.getId()) {
      return new ModelAndView(new RedirectView(topic.getLink()));
    }

    boolean showDeleted = request.getParameter("deleted") != null;
    if (showDeleted) {
      page = -1;
    }

    if (!tmpl.isModeratorSession()) {
      if (showDeleted && !"POST".equals(request.getMethod())) {
        return new ModelAndView(new RedirectView(topic.getLink()));
      }
    }

    if (page == -1 && !tmpl.isSessionAuthorized()) {
      return new ModelAndView(new RedirectView(topic.getLink()));
    }

    int pages = topic.getPageCount(tmpl.getProf().getMessages());

    if (page >= pages && (page > 0 || pages > 0)) {
      if (pages==0) {
        return new ModelAndView(new RedirectView(topic.getLink()));
      } else {
        return new ModelAndView(new RedirectView(topic.getLinkPage(pages - 1)));
      }
    }

    if (showDeleted) {
      if (!tmpl.isSessionAuthorized()) {
        throw new BadInputException("Вы уже вышли из системы");
      }
    }

    params.put("showDeleted", showDeleted);

    User currentUser = AuthUtil.getCurrentUser();

    permissionService.checkView(group, topic, currentUser, showDeleted);

    params.put("message", topic);
    params.put("preparedMessage", preparedMessage);

    if (topic.isExpired()) {
      response.setDateHeader("Expires", System.currentTimeMillis() + 30 * 24 * 60 * 60 * 1000L);
    }

    CommentList comments = commentService.getCommentList(topic, showDeleted);

    if (messageText.isLorcode()) {
      params.put("ogDescription", lorCodeService.trimPlainText(plainText, 250, true));
    }

    params.put("page", page);
    params.put("group", group);
    params.put("showAdsense", !tmpl.isSessionAuthorized() || !tmpl.getProf().isHideAdsense());

    if (!tmpl.isSessionAuthorized()) { // because users have IgnoreList and memories
      String etag = getEtag(topic);
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

    params.put("messageMenu", messagePrepareService.getTopicMenu(
            preparedMessage,
            currentUser,
            request.isSecure(),
            tmpl.getProf(),
            true
    ));

    Set<Integer> ignoreList;

    if (currentUser != null) {
      ignoreList = ignoreListDao.get(currentUser);
    } else {
      ignoreList = ImmutableSet.of();
    }

    int defaultFilterMode = getDefaultFilter(tmpl.getProf(), ignoreList.isEmpty());
    int filterMode;

    if (filter != null) {
      filterMode = CommentFilter.parseFilterChain(filter);

      if (!ignoreList.isEmpty() && filterMode == CommentFilter.FILTER_ANONYMOUS) {
        filterMode += CommentFilter.FILTER_IGNORED;
      }
    } else {
      filterMode = defaultFilterMode;
    }

    params.put("filterMode", CommentFilter.toString(filterMode));
    params.put("defaultFilterMode", CommentFilter.toString(defaultFilterMode));

    loadTopicScroller(params, topic, currentUser, !ignoreList.isEmpty());

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

    if (pages>1 && !showDeleted) {
      params.put("pages", buildPages(topic, tmpl.getProf().getMessages(), filterMode, defaultFilterMode, page));
    }

    if (moreLikeThis!=null && tmpl.getCurrentUser()!=null && tmpl.getCurrentUser().getScore()>=400) {
      params.put("moreLikeThis", moreLikeThisService.resultsOrNothing(moreLikeThis));
    }

    return new ModelAndView("view-message", params);
  }

  private ModelAndView getMessageRss(
          Section section,
          HttpServletRequest request,
          HttpServletResponse response,
          String groupName,
          int msgid) throws Exception {
    Topic topic = messageDao.getById(msgid);
    Template tmpl = Template.getTemplate(request);

    Map<String, Object> params = new HashMap<>();

    List<TagRef> tags = topicTagService.getTagRefs(topic);

    MessageText messageText = msgbaseDao.getMessageText(topic.getId());

    PreparedTopic preparedMessage = messagePrepareService.prepareTopic(
            topic,
            tags,
            request.isSecure(),
            tmpl.getCurrentUser(),
            messageText
    );

    Group group = preparedMessage.getGroup();

    if (!group.getUrlName().equals(groupName) || group.getSectionId() != section.getId()) {
      return new ModelAndView(new RedirectView(topic.getLink()));
    }

    if (topic.isExpired()) {
      throw new MessageNotFoundException(topic.getId(), "no more comments");
    }

    User currentUser = AuthUtil.getCurrentUser();

    permissionService.checkView(group, topic, currentUser, false);

    params.put("message", topic);
    params.put("preparedMessage", preparedMessage);

    if (topic.isExpired()) {
      response.setDateHeader("Expires", System.currentTimeMillis() + 30 * 24 * 60 * 60 * 1000L);
    }

    CommentList comments = commentService.getCommentList(topic, false);

    CommentFilter cv = new CommentFilter(comments);

    List<Comment> commentsFiltred = cv.getCommentsForPage(true, 0, RSS_DEFAULT, ImmutableSet.<Integer>of());

    List<PreparedRSSComment> commentsPrepared = prepareService.prepareCommentListRSS(commentsFiltred, request.isSecure());

    params.put("commentsPrepared", commentsPrepared);
    LorURL lorURL = new LorURL(siteConfig.getMainURI(), siteConfig.getMainUrl());
    params.put("mainURL", lorURL.fixScheme(request.isSecure()));

    return new ModelAndView("view-message-rss", params);
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

  private static boolean checkLastModified(WebRequest webRequest, Topic message) {
    try {
      return webRequest.checkNotModified(message.getLastModified().getTime());
    } catch (IllegalArgumentException ignored) {
      return false;
    }
  }

  private static String getEtag(Topic message) {
    return "msg-"+message.getId()+ '-' +message.getLastModified().getTime();
  }

  private ModelAndView jumpMessage(
          HttpServletRequest request,
          int msgid,
          int cid) throws Exception {
    Template tmpl = Template.getTemplate(request);
    Topic topic = messageDao.getById(msgid);

    CommentList comments = commentService.getCommentList(topic, false);
    CommentNode node = comments.getNode(cid);

    boolean deleted = false;

    if (node==null && tmpl.isModeratorSession()) {
      comments = commentService.getCommentList(topic, true);
      node = comments.getNode(cid);
      deleted = true;
    }

    if (node == null) {
      throw new MessageNotFoundException(topic, cid, "Сообщение #" + cid + " было удалено или не существует");
    }

    int pagenum = deleted?0:comments.getCommentPage(node.getComment(), tmpl.getProf());

    TopicLinkBuilder redirectUrl =
            TopicLinkBuilder
                    .pageLink(topic, pagenum)
                    .lastmod(tmpl.getProf().getMessages())
                    .comment(cid);

    if (deleted) {
      redirectUrl = redirectUrl.showDeleted();
    }

    if (tmpl.isSessionAuthorized() && !deleted) {
      Set<Integer> ignoreList = ignoreListDao.get(tmpl.getCurrentUser());

      Set<Integer> hideSet = commentService.makeHideSet(
              comments,
              getDefaultFilter(tmpl.getProf(), ignoreList.isEmpty()),
              ignoreList
      );

      if (hideSet.contains(node.getComment().getId())) {
        redirectUrl = redirectUrl.filter(CommentFilter.FILTER_NONE);
      }
    }

    return new ModelAndView(new RedirectView(redirectUrl.build()));
  }

  @RequestMapping(value = "/jump-message.jsp", method = {RequestMethod.GET, RequestMethod.HEAD})
  public ModelAndView jumpMessage(
          HttpServletRequest request,
          @RequestParam int msgid,
          @RequestParam(required = false) Integer page,
          @RequestParam(required = false) Integer cid
  ) throws Exception {
    if (cid!=null) {
      return jumpMessage(request, msgid, cid);
    }

    Topic topic = messageDao.getById(msgid);

    TopicLinkBuilder builder;

    if (page != null) {
      builder = TopicLinkBuilder.pageLink(topic, page);
    } else {
      builder = TopicLinkBuilder.baseLink(topic);
    }

    return new ModelAndView(new RedirectView(builder.build()));
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
