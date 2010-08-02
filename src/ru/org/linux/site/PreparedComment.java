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

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

import org.javabb.bbcode.BBCodeProcessor;

public class PreparedComment {
  private final Comment comment;
  private final User author;
  private final String processedMessage;
  private final User replyAuthor;

  public PreparedComment(Connection db, CommentList comments, Comment comment) throws UserNotFoundException, SQLException {
    this.comment = comment;

    this.author = User.getUserCached(db, comment.getUserid());

    processedMessage = getProcessedMessage(db, comment);

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

  public PreparedComment(Connection db, Comment comment, String message) throws UserNotFoundException, SQLException {
    this.comment = comment;

    this.author = User.getUserCached(db, comment.getUserid());

    processedMessage = getProcessedMessage(db, message);

    replyAuthor = null;
  }

  private static String getProcessedMessage(Connection db, Comment comment) throws SQLException {
    Statement st = db.createStatement();
    ResultSet rs = st.executeQuery("SELECT message, bbcode FROM msgbase WHERE id=" + comment.getId());
    rs.next();
    String text = rs.getString("message");
    boolean bbcode = rs.getBoolean("bbcode");

    rs.close();
    st.close();

    if (bbcode) {
      BBCodeProcessor proc = new BBCodeProcessor();
      return proc.preparePostText(db, text);
    } else {
      return "<p>"+text;
    }
  }

  private static String getProcessedMessage(Connection db, String message) throws SQLException {
    BBCodeProcessor proc = new BBCodeProcessor();
    return proc.preparePostText(db, message);
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

  public static List<PreparedComment> prepare(Connection db, CommentList comments, List<Comment> list) throws UserNotFoundException, SQLException {
    List<PreparedComment> commentsPrepared = new ArrayList<PreparedComment>(list.size());

    for (Comment comment: list) {
      commentsPrepared.add(new PreparedComment(db, comments, comment));
    }

    return commentsPrepared;
  }
}
