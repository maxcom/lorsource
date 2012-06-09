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
import ru.org.linux.spring.dao.MessageText;
import ru.org.linux.spring.dao.MsgbaseDao;
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

  private PreparedComment prepareComment(Comment comment, CommentList comments, boolean secure, boolean rss) throws UserNotFoundException {
    MessageText messageText = msgbaseDao.getMessageText(comment.getId());

    return prepareComment(messageText, comment, comments, secure, rss);
  }

  private PreparedComment prepareComment(MessageText messageText, Comment comment, CommentList comments, boolean secure, boolean rss) throws UserNotFoundException {
    User author = userDao.getUserCached(comment.getUserid());
    String processedMessage;

    if(!rss) {
      processedMessage = prepareCommentText(messageText, secure);
    } else {
      processedMessage = prepareCommentTextRSS(messageText, secure);
    }

    User replyAuthor;
    if (comment.getReplyTo()!=0 && comments!=null) {
      CommentNode replyNode = comments.getNode(comment.getReplyTo());

      if (replyNode!=null) {
        Comment reply = replyNode.getComment();
        replyAuthor = userDao.getUserCached(reply.getUserid());
      } else {
        replyAuthor = null;
      }
    } else {
      replyAuthor = null;
    }
    return new PreparedComment(comment, author, processedMessage, replyAuthor);
  }

  public PreparedComment prepareComment(Comment comment, boolean secure) throws UserNotFoundException {
    return prepareComment(comment, null, secure, false);
  }

  public PreparedComment prepareComment(Comment comment, String message, boolean secure) throws UserNotFoundException {
    User author = userDao.getUserCached(comment.getUserid());
    String processedMessage = lorCodeService.parseComment(message, secure);

    return new PreparedComment(comment, author, processedMessage, null);
  }

  public List<PreparedComment> prepareCommentListRSS(CommentList comments, List<Comment> list, boolean secure) throws UserNotFoundException {
    List<PreparedComment> commentsPrepared = new ArrayList<PreparedComment>(list.size());
    for (Comment comment : list) {
      commentsPrepared.add(prepareComment(comment, comments, secure, true));
    }
    return commentsPrepared;
  }

  public List<PreparedComment> prepareCommentList(CommentList comments, List<Comment> list, boolean secure) throws UserNotFoundException {
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

      commentsPrepared.add(prepareComment(text, comment, comments, secure, false));
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
