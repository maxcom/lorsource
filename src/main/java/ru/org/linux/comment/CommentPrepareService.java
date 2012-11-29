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

package ru.org.linux.comment;

import com.google.common.base.Function;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import ru.org.linux.site.Template;
import ru.org.linux.spring.Configuration;
import ru.org.linux.spring.dao.MessageText;
import ru.org.linux.spring.dao.MsgbaseDao;
import ru.org.linux.topic.Topic;
import ru.org.linux.topic.TopicPermissionService;
import ru.org.linux.user.Remark;
import ru.org.linux.user.User;
import ru.org.linux.user.UserDao;
import ru.org.linux.user.UserNotFoundException;
import ru.org.linux.util.bbcode.LorCodeService;

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
  private MsgbaseDao msgbaseDao;

  @Autowired
  private CommentService commentService;

  @Autowired
  private TopicPermissionService topicPermissionService;

  private PreparedComment prepareComment(Comment comment, CommentList comments, boolean secure, boolean rss) throws UserNotFoundException {
    MessageText messageText = msgbaseDao.getMessageText(comment.getId());
    return prepareComment(messageText, comment, comments, secure, rss, null, null);
  }


  private PreparedComment prepareComment(MessageText messageText, Comment comment, CommentList comments,
                                         boolean secure, boolean rss, Template tmpl, Topic topic) throws UserNotFoundException {
    User author = userDao.getUserCached(comment.getUserid());
    String processedMessage;

    if(!rss) {
      processedMessage = prepareCommentText(messageText, secure, !topicPermissionService.followAuthorLinks(author));
    } else {
      processedMessage = prepareCommentTextRSS(messageText, secure);
    }

    User replyAuthor = null;
    Comment reply = null;
    int replyPage = 0;
    boolean haveAnswers = false;
    boolean deletable = false;
    boolean editable = false;
    boolean samePage = false;

    if (comments != null) {
      if (comment.getReplyTo() != 0) {
        CommentNode replyNode = comments.getNode(comment.getReplyTo());

        if (replyNode!=null) {
          reply = replyNode.getComment();
          if(tmpl != null) {
            replyPage = comments.getCommentPage(reply, tmpl);
          }
          replyAuthor = userDao.getUserCached(reply.getUserid());
        }

        samePage = comments.getCommentPage(comment, tmpl) == replyPage;
      }

      haveAnswers = comments.getNode(comment.getId()).isHaveAnswers();

      if(tmpl != null && topic != null) {
        final boolean authored = author.getNick().equals(tmpl.getNick());
        final long currentTimestamp = comment.getPostdate().getTime();

        final User currentUser = tmpl.getCurrentUser();
        final Configuration config = tmpl.getConfig();
        deletable = topicPermissionService.isCommentDeletableNow(
            tmpl.isModeratorSession(),
            topic.isExpired(),
            authored,
            haveAnswers,
            currentTimestamp
        );

        if(currentUser != null) {
          editable = topicPermissionService.isCommentEditableNow(
              tmpl.isModeratorSession(),
              config.isModeratorAllowedToEditComments(),
              config.isCommentEditingAllowedIfAnswersExists(),
              config.getCommentScoreValueForEditing(),
              currentUser.getScore(),
              authored,
              haveAnswers,
              tmpl.getConfig().getCommentExpireMinutesForEdit(),
              currentTimestamp
              );
        }
      }
    }

    Remark remark = null;
    if(tmpl != null && tmpl.getCurrentUser() != null ){
      remark = userDao.getRemark(tmpl.getCurrentUser(), author);
    }

    return new PreparedComment(comment, author, processedMessage, replyAuthor, haveAnswers,
        reply, replyPage, deletable, editable, remark, samePage);
  }

  public PreparedComment prepareCommentForReplayto(Comment comment, boolean secure) throws UserNotFoundException {
    return prepareComment(comment, null, secure, false);
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

    return new PreparedComment(
        comment,
        author,
        processedMessage,
        null, // comments
        commentService.isHaveAnswers(comment),
        null,  // reply
        0,     // replyPage
        false, // deletable
        false, // editable
        null,   // Remark
        false);
  }

  public List<PreparedComment> prepareCommentListRSS(CommentList comments, List<Comment> list, boolean secure) throws UserNotFoundException {
    List<PreparedComment> commentsPrepared = new ArrayList<PreparedComment>(list.size());
    for (Comment comment : list) {
      commentsPrepared.add(prepareComment(comment, comments, secure, true));
    }
    return commentsPrepared;
  }

  public List<PreparedComment> prepareCommentList(CommentList comments, List<Comment> list, boolean secure,
                                                  Template tmpl, Topic topic) throws UserNotFoundException {
    if (list.isEmpty()) {
      return ImmutableList.of();
    }

    Map<Integer, MessageText> texts = msgbaseDao.getMessageText(
            Lists.newArrayList(
                    Iterables.transform(list, new Function<Comment, Integer>() {
                      @Override
                      public Integer apply(Comment comment) {
                        return comment.getId();
                      }
                    })
            )
    );

    List<PreparedComment> commentsPrepared = new ArrayList<PreparedComment>(list.size());
    for (Comment comment : list) {
      MessageText text = texts.get(comment.getId());

      commentsPrepared.add(prepareComment(text, comment, comments, secure, false, tmpl, topic));
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
