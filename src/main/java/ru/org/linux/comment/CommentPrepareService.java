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

package ru.org.linux.comment;

import com.google.common.base.Function;
import com.google.common.base.Strings;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import ru.org.linux.msg.MsgDao;
import ru.org.linux.site.ApiDeleteInfo;
import ru.org.linux.site.DeleteInfo;
import ru.org.linux.site.Template;
import ru.org.linux.spring.dao.DeleteInfoDao;
import ru.org.linux.msg.MessageText;
import ru.org.linux.spring.dao.UserAgentDao;
import ru.org.linux.topic.Topic;
import ru.org.linux.topic.TopicPermissionService;
import ru.org.linux.user.*;
import ru.org.linux.util.bbcode.LorCodeService;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
public class CommentPrepareService {
  @Autowired
  private UserDao userDao;

  @Autowired
  private LorCodeService lorCodeService;

  @Autowired
  private MsgDao msgDao;

  @Autowired
  private TopicPermissionService topicPermissionService;

  @Autowired
  private UserService userService;

  @Autowired
  private DeleteInfoDao deleteInfoDao;

  @Autowired
  private UserAgentDao userAgentDao;

  private PreparedComment prepareComment(
          @Nonnull Comment comment,
          boolean secure
  ) throws UserNotFoundException {
    MessageText messageText = msgDao.getMessageText(comment.getId());
    return prepareComment(messageText, comment, null, secure, null, null);
  }

  private PreparedComment prepareComment(
          MessageText messageText,
          @Nonnull Comment comment,
          CommentList comments,
          boolean secure,
          Template tmpl,
          Topic topic
  ) throws UserNotFoundException {
    User author = userDao.getUserCached(comment.getUserid());

    String processedMessage = prepareCommentText(messageText, secure, !topicPermissionService.followAuthorLinks(author));

    ReplyInfo replyInfo = null;
    boolean deletable = false;
    boolean editable = false;

    if (comments != null) {
      if (comment.getReplyTo() != 0) {
        CommentNode replyNode = comments.getNode(comment.getReplyTo());

        if (replyNode!=null) {
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
                  samePage
          );
        }
      }

      boolean haveAnswers = comments.getNode(comment.getId()).isHaveAnswers();

      if (tmpl != null && topic != null) {
        final User currentUser = tmpl.getCurrentUser();

        deletable = topicPermissionService.isCommentDeletableNow(
            comment,
            currentUser,
            topic,
            haveAnswers
        );

        if (currentUser != null) {
          editable = topicPermissionService.isCommentEditableNow(
              comment,
              currentUser,
              haveAnswers,
              topic
          );
        }
      }
    }

    String remark = null;
    if(tmpl != null && tmpl.isSessionAuthorized() ){
      Remark remarkObject = userDao.getRemark(tmpl.getCurrentUser(), author);

      if (remarkObject!=null) {
        remark = remarkObject.getText();
      }
    }

    Userpic userpic = null;

    if (tmpl != null && tmpl.getProf().isShowPhotos()) {
      userpic = userService.getUserpic(
              author,
              secure,
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

    return new PreparedComment(comment, ref, processedMessage, replyInfo,
            deletable, editable, remark, userpic, deleteInfo, editSummary,
            postIP, userAgent);
  }

  private ApiDeleteInfo loadDeleteInfo(Comment comment) throws UserNotFoundException {
    ApiDeleteInfo deleteInfo = null;

    if (comment.isDeleted()) {
      DeleteInfo info = deleteInfoDao.getDeleteInfo(comment.getId());
      deleteInfo = new ApiDeleteInfo(
              userDao.getUserCached(info.getUserid()).getNick(),
              info.getReason()
      );
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
          @Nonnull Comment comment,
          boolean secure
  ) throws UserNotFoundException {
    User author = userDao.getUserCached(comment.getUserid());

    String processedMessage = prepareCommentTextRSS(messageText, secure);

    return new PreparedRSSComment(comment, author, processedMessage);
  }

  public PreparedComment prepareCommentForReplayto(Comment comment, boolean secure) throws UserNotFoundException {
    return prepareComment(comment, secure);
  }

  /**
   * Подготовить комментарий для последующего редактирования
   * в комментарии не используется автор и не важно существует ли ответ и автор ответа
   * @param comment Редактируемый комментарий
   * @param message Тело комментария
   * @param secure флаг защищенного соединения
   * @return подготовленный коментарий
   * @throws UserNotFoundException
   */
  public PreparedComment prepareCommentForEdit(Comment comment, String message, boolean secure) throws UserNotFoundException {
    User author = userDao.getUserCached(comment.getUserid());
    String processedMessage = lorCodeService.parseComment(message, secure, false);

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
        null);
  }

  public List<PreparedRSSComment> prepareCommentListRSS(
          @Nonnull List<Comment> list,
          boolean secure
  ) throws UserNotFoundException {
    List<PreparedRSSComment> commentsPrepared = new ArrayList<>(list.size());
    for (Comment comment : list) {
      MessageText messageText = msgDao.getMessageText(comment.getId());

      commentsPrepared.add(prepareRSSComment(messageText, comment, secure));
    }
    return commentsPrepared;
  }

  public List<PreparedComment> prepareCommentList(
          @Nonnull CommentList comments,
          @Nonnull List<Comment> list,
          boolean secure,
          @Nonnull Template tmpl,
          @Nonnull Topic topic
  ) throws UserNotFoundException {
    if (list.isEmpty()) {
      return ImmutableList.of();
    }

    Map<Integer, MessageText> texts = msgDao.getMessageText(
            Lists.newArrayList(
                    Iterables.transform(list, new Function<Comment, Integer>() {
                      @Override
                      public Integer apply(Comment comment) {
                        return comment.getId();
                      }
                    })
            )
    );

    List<PreparedComment> commentsPrepared = new ArrayList<>(list.size());
    for (Comment comment : list) {
      MessageText text = texts.get(comment.getId());

      commentsPrepared.add(prepareComment(text, comment, comments, secure, tmpl, topic));
    }
    return commentsPrepared;
  }

  /**
   * Получить html представление текста комментария
   *
   * @param messageText текст комментария
   * @param secure https соединение?
   * @return строку html комментария
   */
  private String prepareCommentText(MessageText messageText, final boolean secure, boolean nofollow) {
    if (messageText.isLorcode()) {
      return lorCodeService.parseComment(messageText.getText(), secure, nofollow);
    } else {
      return "<p>" + messageText.getText() + "</p>";
    }
  }

  /**
   * Получить RSS представление текста комментария
   *
   * @param messageText текст комментария
   * @param secure https соединение?
   * @return строку html комментария
   */
  private String prepareCommentTextRSS(MessageText messageText, final boolean secure) {
    return lorCodeService.prepareTextRSS(messageText.getText(), secure, messageText.isLorcode());
  }
}
