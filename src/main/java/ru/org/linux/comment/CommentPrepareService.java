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

package ru.org.linux.comment;

import com.google.common.base.Strings;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import org.springframework.stereotype.Service;
import ru.org.linux.markup.MessageTextService;
import ru.org.linux.site.ApiDeleteInfo;
import ru.org.linux.site.DeleteInfo;
import ru.org.linux.spring.dao.DeleteInfoDao;
import ru.org.linux.spring.dao.MessageText;
import ru.org.linux.spring.dao.MsgbaseDao;
import ru.org.linux.spring.dao.UserAgentDao;
import ru.org.linux.topic.Topic;
import ru.org.linux.topic.TopicPermissionService;
import ru.org.linux.user.*;

import javax.annotation.Nullable;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class CommentPrepareService {
  private final MessageTextService textService;
  private final MsgbaseDao msgbaseDao;
  private final TopicPermissionService topicPermissionService;
  private final UserService userService;
  private final DeleteInfoDao deleteInfoDao;
  private final UserAgentDao userAgentDao;
  private final RemarkDao remarkDao;

  public CommentPrepareService(MessageTextService textService, MsgbaseDao msgbaseDao,
                               TopicPermissionService topicPermissionService, UserService userService,
                               DeleteInfoDao deleteInfoDao, UserAgentDao userAgentDao, RemarkDao remarkDao) {
    this.textService = textService;
    this.msgbaseDao = msgbaseDao;
    this.topicPermissionService = topicPermissionService;
    this.userService = userService;
    this.deleteInfoDao = deleteInfoDao;
    this.userAgentDao = userAgentDao;
    this.remarkDao = remarkDao;
  }

  private PreparedComment prepareComment(
          MessageText messageText,
          User author,
          Optional<String> remark,
          Comment comment,
          Optional<CommentList> comments,
          Profile profile,
          Topic topic,
          Set<Integer> hideSet,
          Set<Integer> samePageComments,
          @Nullable User currentUser) {
    String processedMessage = textService.renderCommentText(messageText, !topicPermissionService.followAuthorLinks(author));

    ReplyInfo replyInfo = null;
    boolean deletable = false;
    boolean editable = false;
    int answerCount;
    String answerLink;
    boolean answerSamepage = false;

    if (comments.isPresent()) {
      if (comment.getReplyTo() != 0) {
        CommentNode replyNode = comments.get().getNode(comment.getReplyTo());

        boolean replyDeleted = replyNode == null;
        if (replyDeleted) {
          // ответ на удаленный комментарий
          replyInfo = new ReplyInfo(comment.getReplyTo(), true);
        } else {
          Comment reply = replyNode.getComment();

          boolean samePage = samePageComments.contains(reply.getId());

          String replyAuthor = userService.getUserCached(reply.getUserid()).getNick();

          replyInfo = new ReplyInfo(
                  reply.getId(),
                  replyAuthor,
                  Strings.emptyToNull(reply.getTitle().trim()),
                  reply.getPostdate(),
                  samePage,
                  false
          );
        }
      }

      CommentNode node = comments.get().getNode(comment.getId());
      List<CommentNode> replysFiltered = node.childs().stream().filter(commentNode ->
              !hideSet.contains(commentNode.getComment().getId())
      ).collect(Collectors.toList());

      answerCount = replysFiltered.size();

      if (answerCount > 1) {
        answerLink = topic.getLink()+"/thread/" + comment.getId()+"#comments";
      } else if (answerCount == 1) {
        answerLink = topic.getLink()+"?cid=" + replysFiltered.get(0).getComment().getId();
        answerSamepage = samePageComments.contains(replysFiltered.get(0).getComment().getId());
      } else {
        answerLink = null;
      }

      deletable = topicPermissionService.isCommentDeletableNow(comment, currentUser, topic, node.hasAnswers());
      editable = topicPermissionService.isCommentEditableNow(comment, currentUser, node.hasAnswers(), topic, messageText.markup());
    } else {
      answerCount = 0;
      answerLink = null;
    }

    Userpic userpic = null;

    if (profile.isShowPhotos()) {
      userpic = userService.getUserpic(author, profile.getAvatarMode(), false);
    }

    ApiUserRef ref = userService.ref(author, currentUser);

    Optional<DeleteInfo> deleteInfo = loadDeleteInfo(comment);
    Optional<ApiDeleteInfo> apiDeleteInfo = deleteInfo.map(i ->
            new ApiDeleteInfo(userService.getUserCached(i.getUserid()).getNick(), i.getReason())
    );

    EditSummary editSummary = loadEditSummary(comment);

    String postIP = null;
    String userAgent = null;

    if (currentUser!=null && currentUser.isModerator()) {
      postIP = comment.getPostIP();
      userAgent = userAgentDao.getUserAgentById(comment.getUserAgentId());
    }

    boolean undeletable = topicPermissionService.isUndeletable(topic, comment, currentUser, deleteInfo);

    return new PreparedComment(comment, ref, processedMessage, replyInfo,
            deletable, editable, remark.orElse(null), userpic, apiDeleteInfo.orElse(null), editSummary,
            postIP, userAgent, comment.getUserAgentId(), undeletable, answerCount, answerLink, answerSamepage);
  }

  private Optional<DeleteInfo> loadDeleteInfo(Comment comment) {
    if (comment.isDeleted()) {
      return deleteInfoDao.getDeleteInfo(comment.getId());
    } else {
      return Optional.empty();
    }
  }

  private EditSummary loadEditSummary(Comment comment) {
    EditSummary editSummary = null;

    if (comment.getEditCount()>0) {
      editSummary = new EditSummary(
              userService.getUserCached(comment.getEditorId()).getNick(),
              comment.getEditDate(),
              comment.getEditCount()
      );
    }

    return editSummary;
  }

  private PreparedRSSComment prepareRSSComment(MessageText messageText, Comment comment) {
    User author = userService.getUserCached(comment.getUserid());

    String processedMessage = textService.renderTextRSS(messageText);

    return new PreparedRSSComment(comment, author, processedMessage);
  }

  public PreparedComment prepareCommentForReplyto(Comment comment, @Nullable User currentUser, Profile profile, Topic topic) {
    MessageText messageText = msgbaseDao.getMessageText(comment.getId());
    User author = userService.getUserCached(comment.getUserid());

    return prepareComment(messageText, author, Optional.empty(), comment, Optional.empty(), profile, topic, ImmutableSet.of(), ImmutableSet.of(), currentUser);
  }

  /**
   * Подготовить комментарий для последующего редактирования
   * в комментарии не используется автор и не важно существует ли ответ и автор ответа
   * @param comment Редактируемый комментарий
   * @param message Тело комментария
   * @return подготовленный коментарий
   */
  public PreparedComment prepareCommentForEdit(Comment comment, MessageText message) {
    User author = userService.getUserCached(comment.getUserid());
    String processedMessage = textService.renderCommentText(message, false);

    ApiUserRef ref = userService.ref(author, null);

    return new PreparedComment(
        comment,
        ref,
        processedMessage,
        null, // reply
        false, // deletable
        false, // editable
        null,   // Remark
        null, // userpic
        null,
        null,
        null,
        null,
            0,
            false, 0, null, false);
  }

  public List<PreparedRSSComment> prepareCommentListRSS(List<Comment> list) {
    List<PreparedRSSComment> commentsPrepared = new ArrayList<>(list.size());
    for (Comment comment : list) {
      MessageText messageText = msgbaseDao.getMessageText(comment.getId());

      commentsPrepared.add(prepareRSSComment(messageText, comment));
    }
    return commentsPrepared;
  }

  private Map<Integer, User> loadUsers(Iterable<Integer> userIds) {
    ImmutableMap.Builder<Integer, User> builder = ImmutableMap.builder();

    for (User user : userService.getUsersCachedJava(ImmutableSet.copyOf(userIds))) {
      builder.put(user.getId(), user);
    }

    return builder.build();
  }

  public List<PreparedComment> prepareCommentList(
          CommentList comments,
          List<Comment> list,
          Topic topic,
          Set<Integer> hideSet,
          @Nullable User currentUser,
          Profile profile) {
    if (list.isEmpty()) {
      return ImmutableList.of();
    }

    Map<Integer, MessageText> texts = msgbaseDao.getMessageText(list.stream().map(Comment::getId).collect(Collectors.toList()));

    Map<Integer, User> users = loadUsers(list.stream().map(Comment::getUserid).collect(Collectors.toList()));

    Map<Integer, Remark> remarks;

    if (currentUser!=null) {
      remarks = remarkDao.getRemarks(currentUser, users.values());
    } else {
      remarks = ImmutableMap.of();
    }

    Set<Integer> samePageComments = list.stream().map(Comment::getId).collect(Collectors.toSet());

    return list.stream().map(comment -> {
      MessageText text = texts.get(comment.getId());

      User author = users.get(comment.getUserid());

      Optional<Remark> remark = Optional.ofNullable(remarks.get(author.getId()));

      return prepareComment(text, author, remark.map(Remark::getText), comment, Optional.of(comments), profile, topic,
              hideSet, samePageComments, currentUser);
    }).collect(Collectors.toList());
  }

  public List<PreparedCommentsListItem> prepareCommentsList(List<CommentsListItem> comments) {
    Map<Integer, User> users = loadUsers(comments.stream().map(CommentsListItem::getAuthorId).collect(Collectors.toList()));
    Map<Integer, MessageText> texts = msgbaseDao.getMessageText(comments.stream().map(CommentsListItem::getCommentId).collect(Collectors.toList()));

    return comments.stream().map(comment -> {
      User author = users.get(comment.authorId());

      String plainText = textService.extractPlainText(texts.get(comment.getCommentId()));
      String textPreview = MessageTextService.trimPlainText(plainText, 250, false);

      return new PreparedCommentsListItem(comment, author, textPreview);
    }).collect(Collectors.toList());
  }
}
