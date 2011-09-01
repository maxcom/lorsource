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

package ru.org.linux.site;

import ru.org.linux.spring.dao.UserDao;
import ru.org.linux.util.bbcode.ParserResult;
import ru.org.linux.util.bbcode.ParserUtil;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class PreparedComment {
  private final Comment comment;
  private final User author;
  private final String processedMessage;
  private final User replyAuthor;

  public PreparedComment(Comment comment, User author, String processedMessage, User replyAuthor) {
    this.comment = comment;
    this.author = author;
    this.processedMessage = processedMessage;
    this.replyAuthor = replyAuthor;
  }

  @Deprecated
  private PreparedComment(Connection db, PreparedStatement pst, CommentList comments, Comment comment) throws UserNotFoundException, SQLException {
    this.comment = comment;

    author = User.getUserCached(db, comment.getUserid());

    processedMessage = getProcessedMessage(db, pst, comment);

    if (comment.getReplyTo()!=0 && comments!=null) {
      CommentNode replyNode = comments.getNode(comment.getReplyTo());

      if (replyNode!=null) {
        Comment reply = replyNode.getComment();

        replyAuthor = User.getUserCached(db, reply.getUserid());
      } else {
        replyAuthor = null;
      }
    } else {
      replyAuthor = null;
    }
  }

  private static PreparedStatement prepare(Connection db) throws SQLException {
    return db.prepareStatement("SELECT message, bbcode FROM msgbase WHERE id=?");
  }

  private static String getProcessedMessage(Connection db, PreparedStatement pst, Comment comment) throws SQLException {
    pst.setInt(1, comment.getId());
    ResultSet rs = pst.executeQuery();
    rs.next();
    String text = rs.getString(1);
    boolean bbcode = rs.getBoolean(2);

    rs.close();

    if (bbcode) {
      return ParserUtil.bb2xhtml(text, true, true, "", db);
    } else {
      return "<p>" + text;
    }
  }

  public static ParserResult getProcessedMessage(UserDao userDao, String message) {
    return ParserUtil.bb2xhtm(message, true, true, "", userDao);
  }

  public Comment getComment() {
    return comment;
  }

  public User getAuthor() {
    return author;
  }

  public String getProcessedMessage() {
    return processedMessage;
  }

  public User getReplyAuthor() {
    return replyAuthor;
  }

  @Deprecated
  public static List<PreparedComment> prepare(Connection db, CommentList comments, List<Comment> list) throws UserNotFoundException, SQLException {
    List<PreparedComment> commentsPrepared = new ArrayList<PreparedComment>(list.size());
    PreparedStatement pst = prepare(db);

    try {
      for (Comment comment : list) {
        commentsPrepared.add(new PreparedComment(db, pst, comments, comment));
      }

      return commentsPrepared;
    } finally {
      pst.close();
    }
  }
}
