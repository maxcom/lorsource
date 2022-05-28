/*
 * Copyright 1998-2022 Linux.org.ru
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

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
import ru.org.linux.edithistory.EditHistoryService;
import ru.org.linux.edithistory.EditInfoSummary;
import ru.org.linux.group.Group;
import ru.org.linux.group.GroupDao;
import ru.org.linux.markup.MessageTextService;
import ru.org.linux.paginator.PagesInfo;
import ru.org.linux.search.MoreLikeThisService;
import ru.org.linux.search.MoreLikeThisTopic;
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
import ru.org.linux.user.MemoriesDao;
import ru.org.linux.user.Profile;
import ru.org.linux.user.User;
import scala.Option;
import scala.concurrent.Future;
import scala.concurrent.duration.Deadline;
import scala.concurrent.duration.Duration;
import scala.concurrent.duration.FiniteDuration;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.*;
import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;

import static ru.org.linux.edithistory.EditHistoryObjectTypeEnum.TOPIC;

@Controller
public class TopicController {
  private static final int RSS_DEFAULT = 20;
  private static final FiniteDuration MoreLikeThisTimeout = Duration.apply(500, TimeUnit.MILLISECONDS);

  private final static Logger logger = LoggerFactory.getLogger(TopicController.class);
  public static final org.joda.time.Duration JUMP_MIN_DURATION = org.joda.time.Duration.standardDays(30);

  @Autowired
  private SectionService sectionService;

  @Autowired
  private TopicDao messageDao;

  @Autowired
  private CommentPrepareService prepareService;

  @Autowired
  private TopicPrepareService topicPrepareService;

  @Autowired
  private CommentReadService commentService;

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
  private MessageTextService textService;

  @Autowired
  private MemoriesDao memoriesDao;

  @Autowired
  private EditHistoryService editHistoryService;

  @Autowired
  private GroupDao groupDao;

  @RequestMapping("/{section:(?:forum)|(?:news)|(?:polls)|(?:gallery)}/{group}/{id}")
  public ModelAndView getMessageNewMain(
          WebRequest webRequest,
          HttpServletRequest request,
          HttpServletResponse response,
          @RequestParam(value = "filter", required = false) String filter,
          @RequestParam(value = "cid", required = false) Integer cid,
          @RequestParam(value = "skipdeleted", required = false, defaultValue = "false") boolean skipDeleted,
          @PathVariable("section") String sectionName,
          @PathVariable("group") String groupName,
          @PathVariable("id") int msgid
  ) {
    if (cid != null) {
      return jumpMessage(request, msgid, cid, skipDeleted);
    }

    Section section = sectionService.getSectionByName(sectionName);

    boolean rss = request.getParameter("output") != null && "rss".equals(request.getParameter("output"));

    if (rss) {
      return getMessageRss(section, request, response, groupName, msgid);
    } else {
      return getMessage(section, webRequest, request, response, 0, filter, groupName, msgid, 0);
    }
  }

  @RequestMapping("/{section:(?:forum)|(?:news)|(?:polls)|(?:gallery)}/{group}/{id}/page{page}")
  public ModelAndView getMessageNewPage(
          WebRequest webRequest,
          HttpServletRequest request,
          HttpServletResponse response,
          @RequestParam(value = "filter", required = false) String filter,
          @PathVariable("section") String sectionName,
          @PathVariable("group") String groupName,
          @PathVariable("id") int msgid,
          @PathVariable("page") int page
  ) {
    Section section = sectionService.getSectionByName(sectionName);

    return getMessage(section, webRequest, request, response, page, filter, groupName, msgid, 0);
  }

  @RequestMapping("/{section:(?:forum)|(?:news)|(?:polls)|(?:gallery)}/{group}/{id}/thread/{threadRoot}")
  public ModelAndView getMessageThread(
          WebRequest webRequest,
          HttpServletRequest request,
          HttpServletResponse response,
          @RequestParam(value = "filter", required = false) String filter,
          @RequestParam(value = "skipdeleted", required = false, defaultValue = "false") boolean skipDeleted,
          @PathVariable("section") String sectionName,
          @PathVariable("group") String groupName,
          @PathVariable("id") int msgid,
          @PathVariable("threadRoot") int threadRoot
  ) {
    Section section = sectionService.getSectionByName(sectionName);

    return getMessage(section, webRequest, request, response, 0, filter, groupName, msgid, threadRoot);
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

    if (filterMode != defaultFilterMode) {
      base = base.filter(filterMode);
    }

    List<String> out = new ArrayList<>();

    for (int i = 0; i < topic.getPageCount(messagesPerPage); i++) {
      out.add(base.page(i).build());
    }

    return new PagesInfo(out, currentPage);
  }

  private ModelAndView getMessage(
          Section section,
          WebRequest webRequest,
          HttpServletRequest request,
          HttpServletResponse response,
          int page,
          String filter,
          String groupName,
          int msgid,
          int threadRoot) {

    Deadline deadline = MoreLikeThisTimeout.fromNow();

    Topic topic = messageDao.getById(msgid);
    List<TagRef> tags = topicTagService.getTagRefs(topic);

    Future<List<List<MoreLikeThisTopic>>> moreLikeThis = moreLikeThisService.searchSimilar(topic, tags);

    MessageText messageText = msgbaseDao.getMessageText(topic.getId());
    String plainText = textService.extractPlainText(messageText);

    Template tmpl = Template.getTemplate(request);

    PreparedTopic preparedMessage = topicPrepareService.prepareTopic(
            topic,
            tags,
            tmpl.getCurrentUser(),
            messageText
    );

    Map<String, Object> params = new HashMap<>();

    if (tmpl.isSessionAuthorized()) {
      Option<EditInfoSummary> editInfoSummary = editHistoryService.editInfoSummary(topic.getId(), TOPIC);

      if (editInfoSummary.nonEmpty()) {
        params.put("editInfo", topicPrepareService.prepareEditInfo(editInfoSummary.get()));
      }
    }

    Group group = preparedMessage.getGroup();

    if (!group.getUrlName().equals(groupName) || group.getSectionId() != section.getId()) {
      return new ModelAndView(new RedirectView(topic.getLink()));
    }

    boolean showDeleted = request.getParameter("deleted") != null;
    if (showDeleted) {
      page = -1;
    }

    User currentUser = AuthUtil.getCurrentUser();

    permissionService.checkView(group, topic, currentUser, preparedMessage.getAuthor(), showDeleted);

    if (!tmpl.isModeratorSession()) {
      if (showDeleted && !"POST".equals(request.getMethod())) {
        return new ModelAndView(new RedirectView(topic.getLink()));
      }
    }

    if (page == -1 && !showDeleted) {
      return new ModelAndView(new RedirectView(topic.getLink()));
    }

    int pages = topic.getPageCount(tmpl.getProf().getMessages());

    if (page >= pages && page > 0) {
      if (pages == 0) {
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

    params.put("message", topic);
    params.put("preparedMessage", preparedMessage);

    if (topic.isExpired()) {
      response.setDateHeader("Expires", System.currentTimeMillis() + 30 * 24 * 60 * 60 * 1000L);
    }

    params.put("ogDescription", MessageTextService.trimPlainText(plainText, 250, true));

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

    params.put("messageMenu", topicPrepareService.getTopicMenu(
            preparedMessage,
            currentUser,
            tmpl.getProf(),
            true
    ));

    params.put("memoriesInfo", memoriesDao.getTopicInfo(topic.getId(), currentUser));

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

    CommentList comments = getCommentList(topic, group, showDeleted);

    Set<Integer> hideSet = commentService.makeHideSet(comments, filterMode, ignoreList);

    CommentFilter cv = new CommentFilter(comments);

    List<Comment> commentsFiltered;
    int unfilteredCount;

    if (threadRoot!=0) {
      commentsFiltered = cv.getCommentsSubtree(threadRoot, hideSet);
      unfilteredCount = cv.getCommentsSubtree(threadRoot, ImmutableSet.of()).size();
      params.put("threadMode", true);
      params.put("threadRoot", threadRoot);
    } else {
      commentsFiltered = cv.getCommentsForPage(false, page, tmpl.getProf().getMessages(), hideSet);
      unfilteredCount = cv.getCommentsForPage(false, page, tmpl.getProf().getMessages(), ImmutableSet.of()).size();
    }

    params.put("unfilteredCount", unfilteredCount);

    List<PreparedComment> commentsPrepared = prepareService.prepareCommentList(
            comments,
            commentsFiltered,
            topic,
            hideSet,
            tmpl.getCurrentUser(),
            tmpl.getProf()
    );

    params.put("commentsPrepared", commentsPrepared);

    if (comments.getList().isEmpty()) {
      params.put("lastCommentId", 0);
    } else {
      params.put("lastCommentId", comments.getList().get(comments.getList().size() - 1).getId());
    }

    IPBlockInfo ipBlockInfo = ipBlockDao.getBlockInfo(request.getRemoteAddr());
    params.put("ipBlockInfo", ipBlockInfo);

    params.put("modes", MessageTextService.postingModeSelector(tmpl.getCurrentUser(), tmpl.getFormatMode()));

    CommentRequest add = new CommentRequest();
    add.setMode(tmpl.getFormatMode());
    params.put("add", add);

    if (pages > 1 && !showDeleted && threadRoot == 0 && !comments.getList().isEmpty()) {
      params.put("pages", buildPages(topic, tmpl.getProf().getMessages(), filterMode, defaultFilterMode, page));
    }

    params.put("moreLikeThisGetter", (Callable<List<List<MoreLikeThisTopic>>>) () ->
            moreLikeThisService.resultsOrNothing(topic, moreLikeThis, deadline)
    );

    params.put("showDeletedButton", permissionService.allowViewDeletedComments(topic, currentUser) && !showDeleted);

    params.put("dateJumps", prepareService.buildDateJumpSet(commentsFiltered, JUMP_MIN_DURATION));

    return new ModelAndView("view-message", params);
  }

  private CommentList getCommentList(Topic topic, Group group, boolean showDeleted) {
    CommentList comments;
    if (permissionService.getPostscore(group, topic) == TopicPermissionService.POSTSCORE_HIDE_COMMENTS && !showDeleted) {
      comments = new CommentList(ImmutableList.of(), 0);
    } else {
      comments = commentService.getCommentList(topic, showDeleted);
    }
    return comments;
  }

  private ModelAndView getMessageRss(
          Section section,
          HttpServletRequest request,
          HttpServletResponse response,
          String groupName,
          int msgid) {
    Topic topic = messageDao.getById(msgid);
    Template tmpl = Template.getTemplate(request);

    Map<String, Object> params = new HashMap<>();

    List<TagRef> tags = topicTagService.getTagRefs(topic);

    MessageText messageText = msgbaseDao.getMessageText(topic.getId());

    PreparedTopic preparedMessage = topicPrepareService.prepareTopic(
            topic,
            tags,
            tmpl.getCurrentUser(),
            messageText
    );

    Group group = preparedMessage.getGroup();

    if (!group.getUrlName().equals(groupName) || group.getSectionId() != section.getId()) {
      return new ModelAndView(new RedirectView(topic.getLink() + "?output=rss"));
    }

    if (topic.isExpired()) {
      throw new MessageNotFoundException(topic.getId(), "no more comments");
    }

    User currentUser = AuthUtil.getCurrentUser();

    permissionService.checkView(group, topic, currentUser, preparedMessage.getAuthor(),false);

    params.put("message", topic);
    params.put("preparedMessage", preparedMessage);

    if (topic.isExpired()) {
      response.setDateHeader("Expires", System.currentTimeMillis() + 30 * 24 * 60 * 60 * 1000L);
    }

    CommentList comments = getCommentList(topic, group, false);

    CommentFilter cv = new CommentFilter(comments);

    List<Comment> commentsFiltered = cv.getCommentsForPage(true, 0, RSS_DEFAULT, ImmutableSet.of());

    List<PreparedRSSComment> commentsPrepared = prepareService.prepareCommentListRSS(commentsFiltered);

    params.put("commentsPrepared", commentsPrepared);
    params.put("mainURL", siteConfig.getSecureUrl());

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

    boolean topScroller;
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
   *
   * @param msgid   id топика
   * @param page    страница топика
   * @param lastmod параметр для кэширования
   * @param filter  фильтр
   * @param output  ?
   * @return вовзращает редирект на новый код
   */

  @RequestMapping("/view-message.jsp")
  public ModelAndView getMessageOld(
          @RequestParam("msgid") int msgid,
          @RequestParam(value = "page", required = false) Integer page,
          @RequestParam(value = "lastmod", required = false) Long lastmod,
          @RequestParam(value = "filter", required = false) String filter,
          @RequestParam(required = false) String output
  ) {
    Topic topic = messageDao.getById(msgid);

    StringBuilder link = new StringBuilder(topic.getLink());

    StringBuilder params = new StringBuilder();

    if (page != null) {
      link.append("/page").append(page);
    }

    if (lastmod != null && !topic.isExpired()) {
      params.append("?lastmod=").append(topic.getLastModified().getTime());
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

  private static boolean checkLastModified(WebRequest webRequest, Topic message) {
    try {
      return webRequest.checkNotModified(message.getLastModified().getTime());
    } catch (IllegalArgumentException ignored) {
      return false;
    }
  }

  private static String getEtag(Topic message) {
    return "msg-" + message.getId() + '-' + message.getLastModified().getTime();
  }

  private ModelAndView jumpMessage(
          HttpServletRequest request,
          int msgid,
          int cid, boolean skipDeleted) {
    Template tmpl = Template.getTemplate(request);
    Topic topic = messageDao.getById(msgid);
    Group group = groupDao.getGroup(topic.getGroupId());

    CommentList comments = getCommentList(topic, group, false);
    CommentNode node = comments.getNode(cid);

    if (node == null && skipDeleted) {
      ImmutableList<Comment> list = comments.getList();

      if (list.isEmpty()) {
        return new ModelAndView(new RedirectView(topic.getLink()));
      }

      Comment c = list.stream().filter(v -> v.getId() > cid).findFirst().orElse(list.get(list.size() - 1));

      node = comments.getNode(c.getId());
    }

    boolean deleted = false;

    if (node == null && tmpl.isModeratorSession()) {
      comments = getCommentList(topic, group, true);
      node = comments.getNode(cid);
      deleted = true;
    }

    if (node == null) {
      throw new MessageNotFoundException(topic, cid, "Сообщение #" + cid + " было удалено или не существует");
    }

    int pagenum = deleted ? 0 : comments.getCommentPage(node.getComment(), tmpl.getProf());

    TopicLinkBuilder redirectUrl =
            TopicLinkBuilder
                    .pageLink(topic, pagenum)
                    .lastmod(tmpl.getProf().getMessages())
                    .comment(node.getComment().getId());

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
  ) {
    if (cid != null) {
      return jumpMessage(request, msgid, cid, false);
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
    logger.debug("Not found", ex);

    if (ex.getTopic() != null) {
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
