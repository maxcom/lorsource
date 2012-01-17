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

package ru.org.linux.comment;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import ru.org.linux.spring.dao.MessageText;
import ru.org.linux.spring.dao.MsgbaseDao;
import ru.org.linux.user.User;
import ru.org.linux.user.UserDao;
import ru.org.linux.user.UserNotFoundException;
import ru.org.linux.util.bbcode.LorCodeService;

import java.util.ArrayList;
import java.util.List;

@Service
public class CommentPrepareService {
  @Autowired
  private UserDao userDao;

  @Autowired
  private CommentDao commentDao;

  @Autowired
  private LorCodeService lorCodeService;

  @Autowired
  private MsgbaseDao msgbaseDao;

  public PreparedComment prepareCommentRSS(Comment comment, CommentList comments, boolean secure) throws UserNotFoundException {
    return prepareComment(comment, comments, secure, true);
  }

  public PreparedComment prepareComment(Comment comment, CommentList comments, boolean secure) throws UserNotFoundException {
    return prepareComment(comment, comments, secure, false);
  }

  public PreparedComment prepareComment(Comment comment, CommentList comments, boolean secure, boolean rss) throws UserNotFoundException {
    User author = userDao.getUserCached(comment.getUserid());
    String processedMessage;
    if(!rss) {
      processedMessage = prepareCommentText(comment.getId(), secure);
    } else {
      processedMessage = prepareCommentTextRSS(comment.getId(), secure);
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
    return prepareComment(comment, (CommentList)null, secure);
  }

  public PreparedComment prepareComment(Comment comment, String message, boolean secure) throws UserNotFoundException {
    User author = userDao.getUserCached(comment.getUserid());
    String processedMessage = lorCodeService.parseComment(message, secure);

    return new PreparedComment(comment, author, processedMessage, null);
  }

  public List<PreparedComment> prepareCommentListRSS(CommentList comments, List<Comment> list, boolean secure) throws UserNotFoundException {
    List<PreparedComment> commentsPrepared = new ArrayList<PreparedComment>(list.size());
    for (Comment comment : list) {
      commentsPrepared.add(prepareCommentRSS(comment, comments, secure));
    }
    return commentsPrepared;
  }

  public List<PreparedComment> prepareCommentList(CommentList comments, List<Comment> list, boolean secure) throws UserNotFoundException {
    List<PreparedComment> commentsPrepared = new ArrayList<PreparedComment>(list.size());
    for (Comment comment : list) {
      commentsPrepared.add(prepareComment(comment, comments, secure));
    }
    return commentsPrepared;
  }

  /**
   * Получить html представление текста комментария
   *
   * @param id id комментария
   * @param secure https соединение?
   * @return строку html комментария
   */
  public String prepareCommentText(int id, final boolean secure) {
    MessageText messageText = msgbaseDao.getMessageText(id);

    if (messageText.isLorcode()) {
      return lorCodeService.parseComment(messageText.getText(), secure);
    } else {
      return "<p>" + messageText.getText() + "</p>";
    }
  }

  /**
   * Получить RSS представление текста комментария
   *
   * @param id id комментария
   * @param secure https соединение?
   * @return строку html комментария
   */
  public String prepareCommentTextRSS(int id, final boolean secure) {
    MessageText messageText = msgbaseDao.getMessageText(id);

    if (messageText.isLorcode()) {
      return lorCodeService.parseCommentRSS(messageText.getText(), secure);
    } else {
      return "<p>" + messageText.getText() + "</p>";
    }
  }
}
