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
import ru.org.linux.spring.dao.MessageText;
import ru.org.linux.spring.dao.MsgbaseDao;
import ru.org.linux.topic.Topic;
import ru.org.linux.user.User;
import ru.org.linux.user.UserDao;
import ru.org.linux.user.UserNotFoundException;
import ru.org.linux.util.bbcode.LorCodeService;

import javax.annotation.Nullable;
import javax.validation.constraints.NotNull;
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

  private PreparedComment prepareComment(Comment comment, CommentList comments, boolean secure, boolean rss,
                                         Template tmpl, Topic topic) throws UserNotFoundException {
    MessageText messageText = msgbaseDao.getMessageText(comment.getId());
    return prepareComment(messageText, comment, comments, secure, rss, tmpl, topic);
  }

  /**
   * Проверяем можно ли редактировать комментарий на текущий момент
   * @param moderatorMode текущий пользователь можератор
   * @param moderatorAllowEditComments модертор может редактировать?
   * @param commentEditingAllowedIfAnswersExists можно ли редактировать если есть ответы?
   * @param commentScoreValueForEditing кол-во шкворца необходимое для редактирования
   * @param userScore кол-во шгкворца у текущего пользователя
   * @param authored является текущий пользователь автором комментария
   * @param haveAnswers есть у комменатрия ответы
   * @param commentExpireMinutesForEdit после скольки минут редактировать невкоем случае нельзя
   * @param commentTimestamp время создания комментария
   * @return результат
   */
  private boolean isEditableNow(boolean moderatorMode, boolean moderatorAllowEditComments, boolean commentEditingAllowedIfAnswersExists,
                                int commentScoreValueForEditing, int userScore,
                                boolean authored, boolean haveAnswers, int commentExpireMinutesForEdit, long commentTimestamp) {
    Boolean editable = moderatorMode && moderatorAllowEditComments;
    long nowTimestamp = new java.util.Date().getTime();
    if (!editable && authored) {

      boolean isbyMinutesEnable;
      if (commentExpireMinutesForEdit != 0) {
        long deltaTimestamp = commentExpireMinutesForEdit * 60 * 1000;

        isbyMinutesEnable = commentTimestamp + deltaTimestamp > nowTimestamp;
      } else {
        isbyMinutesEnable = true;
      }

      boolean isbyAnswersEnable = true;
      if (!commentEditingAllowedIfAnswersExists
        && haveAnswers) {
        isbyAnswersEnable = false;
      }

      boolean isByScoreEnable = true;
      if (commentScoreValueForEditing > userScore) {
        isByScoreEnable = false;
      }

      editable = isbyMinutesEnable & isbyAnswersEnable & isByScoreEnable;
    }
    return editable;
  }

  /**
   * Проверяем можно ли удалять комментарий на текущий момент
   * @param moderatorMode текущий пользователь модератор?
   * @param expired топик устарел(архивный)?
   * @param authored текущий пользьователь автор комментария?
   * @param haveAnswers у комментрия есть ответы?
   * @param commentTimestamp время создания комментария
   * @return резултат
   */
  private boolean isDeletableNow(boolean moderatorMode, boolean expired, boolean authored, boolean haveAnswers, long commentTimestamp ) {
    long nowTimestamp = new java.util.Date().getTime();
    return moderatorMode ||
        (!expired &&
         authored &&
         !haveAnswers &&
          nowTimestamp - commentTimestamp < DeleteCommentController.DELETE_PERIOD);
  }

  private PreparedComment prepareComment(MessageText messageText, Comment comment, CommentList comments,
                                         boolean secure, boolean rss, Template tmpl, Topic topic) throws UserNotFoundException {
    User author = userDao.getUserCached(comment.getUserid());
    String processedMessage;

    if(!rss) {
      processedMessage = prepareCommentText(messageText, secure);
    } else {
      processedMessage = prepareCommentTextRSS(messageText, secure);
    }

    User replyAuthor = null;
    Comment reply = null;
    int replyPage = 0;
    String topicPage = null;
    String replyTitle = null;
    boolean showLastMod = false;
    boolean haveAnswers = false;
    boolean deletable = false;
    boolean editable = false;

    if (comments != null) {
      if (comment.getReplyTo() != 0) {
        CommentNode replyNode = comments.getNode(comment.getReplyTo());

        if (replyNode!=null) {
          reply = replyNode.getComment();
          if(tmpl != null) {
            replyPage = comments.getCommentPage(reply, tmpl);
          }
          replyAuthor = userDao.getUserCached(reply.getUserid());
          replyTitle = reply.getTitle();
          if (replyTitle.trim().isEmpty()) {
            replyTitle = "комментарий";
          }
          if(topic != null) {
            showLastMod = (tmpl != null && !topic.isExpired() && replyPage==topic.getPageCount(tmpl.getProf().getMessages())-1);
            topicPage = topic.getLinkPage(replyPage);
          }
        }
      }


      haveAnswers = comments.getNode(comment.getId()).isHaveAnswers();
      if(tmpl != null && topic != null) {
        boolean authored = author.getNick().equals(tmpl.getNick());
        long currentTimestamp = comment.getPostdate().getTime();
        deletable = isDeletableNow(tmpl.isModeratorSession(), topic.isExpired(), authored, haveAnswers, currentTimestamp);
        editable = isEditableNow(
            tmpl.isModeratorSession(),
            tmpl.getConfig().isModeratorAllowedToEditComments(),
            tmpl.getConfig().isCommentEditingAllowedIfAnswersExists(),
            tmpl.getConfig().getCommentScoreValueForEditing(),
            tmpl.getCurrentUser().getScore(),
            authored,
            haveAnswers,
            tmpl.getConfig().getCommentExpireMinutesForEdit(),
            currentTimestamp
            );
      }
    }

    return new PreparedComment(comment, author, processedMessage, replyAuthor, haveAnswers,
        reply, replyPage, topicPage, replyTitle, showLastMod, deletable, editable);
  }

  public PreparedComment prepareCommentForReplayto(Comment comment, boolean secure) throws UserNotFoundException {
    return prepareComment(comment, null, secure, false, null, null);
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
    String processedMessage = lorCodeService.parseComment(message, secure);

    return new PreparedComment(
        comment,
        author,
        processedMessage,
        null, // comments
        commentService.isHaveAnswers(comment),
        null,  // reply
        0,     // replyPage
        null,  // topicPage
        null,  // replyTitle
        false, // showLastMode
        false, // deletable
        false  // editable
    );
  }

  public List<PreparedComment> prepareCommentListRSS(CommentList comments, List<Comment> list, boolean secure) throws UserNotFoundException {
    List<PreparedComment> commentsPrepared = new ArrayList<PreparedComment>(list.size());
    for (Comment comment : list) {
      commentsPrepared.add(prepareComment(comment, comments, secure, true, null, null));
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
  private String prepareCommentText(MessageText messageText, final boolean secure) {
    if (messageText.isLorcode()) {
      return lorCodeService.parseComment(messageText.getText(), secure);
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
