/*
 * Copyright 1998-2021 Linux.org.ru
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
import com.google.common.collect.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import ru.org.linux.markup.MessageTextService;
import ru.org.linux.site.ApiDeleteInfo;
import ru.org.linux.site.DeleteInfo;
import ru.org.linux.site.Template;
import ru.org.linux.spring.dao.DeleteInfoDao;
import ru.org.linux.spring.dao.MessageText;
import ru.org.linux.spring.dao.MsgbaseDao;
import ru.org.linux.spring.dao.UserAgentDao;
import ru.org.linux.topic.Topic;
import ru.org.linux.topic.TopicPermissionService;
import ru.org.linux.user.*;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class CommentPrepareService {
  @Autowired
  private UserDao userDao;

  @Autowired
  private MessageTextService textService;

  @Autowired
  private MsgbaseDao msgbaseDao;

  @Autowired
  private TopicPermissionService topicPermissionService;

  @Autowired
  private UserService userService;

  @Autowired
  private DeleteInfoDao deleteInfoDao;

  @Autowired
  private UserAgentDao userAgentDao;

  @Autowired
  private RemarkDao remarkDao;

  private PreparedComment prepareComment(
          @Nonnull Comment comment
  ) throws UserNotFoundException {
    MessageText messageText = msgbaseDao.getMessageText(comment.getId());
    User author = userDao.getUserCached(comment.getUserid());

    return prepareComment(messageText, author, null, comment, null, null, null, ImmutableSet.of());
  }

  private PreparedComment prepareComment(
          MessageText messageText,
          User author,
          @Nullable String remark,
          Comment comment,
          CommentList comments,
          Template tmpl,
          Topic topic,
          Set<Integer> hideSet) throws UserNotFoundException {
    String processedMessage = textService.renderCommentText(messageText, !topicPermissionService.followAuthorLinks(author));

    ReplyInfo replyInfo = null;
    boolean deletable = false;
    boolean editable = false;
    int answerCount;
    String answerLink;

    if (comments != null) {
      if (comment.getReplyTo() != 0) {
        CommentNode replyNode = comments.getNode(comment.getReplyTo());

        boolean replyDeleted = replyNode == null;
        if (replyDeleted) {
          // ответ на удаленный комментарий
          replyInfo = new ReplyInfo(comment.getReplyTo(), true);
        } else {
          Comment reply = replyNode.getComment();

          boolean samePage = false;

          if (tmpl != null) {
            int replyPage = comments.getCommentPage(reply, tmpl.getProf());
            samePage = comments.getCommentPage(comment, tmpl.getProf()) == replyPage;
          }

          String replyAuthor = userDao.getUserCached(reply.getUserid()).getNick();

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

      CommentNode node = comments.getNode(comment.getId());
      List<CommentNode> replysFiltered = node.childs().stream().filter(commentNode ->
              !hideSet.contains(commentNode.getComment().getId())
      ).collect(Collectors.toList());

      answerCount = replysFiltered.size();

      if (answerCount > 1 && tmpl.getCurrentUser()!=null && tmpl.getCurrentUser().isModerator()) {
        answerLink = topic.getLink()+"/thread/" + comment.getId()+"#comments";
      } else if (answerCount > 0) {
        answerLink = topic.getLink()+"?cid=" + replysFiltered.get(0).getComment().getId();
      } else {
        answerLink = null;
      }

      if (tmpl != null && topic != null) {
        final User currentUser = tmpl.getCurrentUser();

        deletable = topicPermissionService.isCommentDeletableNow(comment, currentUser, topic, node.hasAnswers());

        if (currentUser != null) {
          editable = topicPermissionService.isCommentEditableNow(comment, currentUser, node.hasAnswers(), topic,
                  messageText.markup());
        }
      }
    } else {
      answerCount = 0;
      answerLink = null;
    }

    Userpic userpic = null;

    if (tmpl != null && tmpl.getProf().isShowPhotos()) {
      userpic = userService.getUserpic(
              author,
              tmpl.getProf().getAvatarMode(),
              false
      );
    }

    ApiUserRef ref = userService.ref(author, tmpl!=null?tmpl.getCurrentUser():null);

    ApiDeleteInfo deleteInfo = loadDeleteInfo(comment);

    EditSummary editSummary = loadEditSummary(comment);

    String postIP = null;
    String userAgent = null;

    if (tmpl!=null && tmpl.isModeratorSession()) {
      postIP = comment.getPostIP();
      userAgent = userAgentDao.getUserAgentById(comment.getUserAgentId());
    }

    boolean undeletable = false;
    if (tmpl!=null) {
      DeleteInfo info = null;

      if (comment.isDeleted()) {
        info = deleteInfoDao.getDeleteInfo(comment.getId());
      }

      undeletable = topicPermissionService.isUndeletable(topic, comment, tmpl.getCurrentUser(), info);
    }

    return new PreparedComment(comment, ref, processedMessage, replyInfo,
            deletable, editable, remark, userpic, deleteInfo, editSummary,
            postIP, userAgent, undeletable, answerCount, answerLink);
  }

  private ApiDeleteInfo loadDeleteInfo(Comment comment) throws UserNotFoundException {
    ApiDeleteInfo deleteInfo = null;

    if (comment.isDeleted()) {
      DeleteInfo info = deleteInfoDao.getDeleteInfo(comment.getId());

      if (info!=null) {
        deleteInfo = new ApiDeleteInfo(
                userDao.getUserCached(info.getUserid()).getNick(),
                info.getReason()
        );
      }
    }

    return deleteInfo;
  }

  private EditSummary loadEditSummary(Comment comment) throws UserNotFoundException {
    EditSummary editSummary = null;

    if (comment.getEditCount()>0) {
      editSummary = new EditSummary(
              userDao.getUserCached(comment.getEditorId()).getNick(),
              comment.getEditDate(),
              comment.getEditCount()
      );
    }

    return editSummary;
  }

  private PreparedRSSComment prepareRSSComment(
          @Nonnull MessageText messageText,
          @Nonnull Comment comment
  ) throws UserNotFoundException {
    User author = userDao.getUserCached(comment.getUserid());

    String processedMessage = textService.renderTextRSS(messageText);

    return new PreparedRSSComment(comment, author, processedMessage);
  }

  public PreparedComment prepareCommentForReplyto(Comment comment) throws UserNotFoundException {
    return prepareComment(comment);
  }

  /**
   * Подготовить комментарий для последующего редактирования
   * в комментарии не используется автор и не важно существует ли ответ и автор ответа
   * @param comment Редактируемый комментарий
   * @param message Тело комментария
   * @return подготовленный коментарий
   */
  public PreparedComment prepareCommentForEdit(Comment comment, MessageText message) throws UserNotFoundException {
    User author = userDao.getUserCached(comment.getUserid());
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
            false, 0, null);
  }

  public List<PreparedRSSComment> prepareCommentListRSS(
          @Nonnull List<Comment> list
  ) throws UserNotFoundException {
    List<PreparedRSSComment> commentsPrepared = new ArrayList<>(list.size());
    for (Comment comment : list) {
      MessageText messageText = msgbaseDao.getMessageText(comment.getId());

      commentsPrepared.add(prepareRSSComment(messageText, comment));
    }
    return commentsPrepared;
  }

  private Map<Integer, User> loadUsers(Iterable<Integer> userIds) {
    ImmutableMap.Builder<Integer, User> builder = ImmutableMap.builder();

    for (User user : userService.getUsersCached(ImmutableSet.copyOf(userIds))) {
      builder.put(user.getId(), user);
    }

    return builder.build();
  }

  public List<PreparedComment> prepareCommentList(
          @Nonnull CommentList comments,
          @Nonnull List<Comment> list,
          @Nonnull Template tmpl,
          @Nonnull Topic topic,
          Set<Integer> hideSet) throws UserNotFoundException {
    if (list.isEmpty()) {
      return ImmutableList.of();
    }

    Map<Integer, MessageText> texts = msgbaseDao.getMessageText(Lists.transform(list, Comment::getId));

    Map<Integer, User> users = loadUsers(Iterables.transform(list, Comment::getUserid));
    User currentUser = tmpl.getCurrentUser();

    Map<Integer, Remark> remarks;

    if (currentUser!=null) {
      remarks = remarkDao.getRemarks(currentUser, users.values());
    } else {
      remarks = ImmutableMap.of();
    }

    return list.stream().map(comment -> {
      MessageText text = texts.get(comment.getId());

      User author = users.get(comment.getUserid());

      Remark remark = remarks.get(author.getId());

      String remarkText;

      if (remark!=null) {
        remarkText = remark.getText();
      } else {
        remarkText = null;
      }

      return prepareComment(text, author, remarkText, comment, comments, tmpl, topic, hideSet);
    }).collect(Collectors.toList());
  }
}
