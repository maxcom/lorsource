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
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

public class CommentDeleter {
  private static final Log logger = LogFactory.getLog(CommentDeleter.class);

  private final PreparedStatement deleteComment;
  private final PreparedStatement insertDelinfo;
  private final PreparedStatement replysForComment;
  private final PreparedStatement updateScore;

  public CommentDeleter(Connection db) throws SQLException {
    deleteComment = db.prepareStatement("UPDATE comments SET deleted='t' WHERE id=?");
    insertDelinfo = db.prepareStatement("INSERT INTO del_info (msgid, delby, reason, deldate) values(?,?,?, CURRENT_TIMESTAMP)");
    replysForComment = db.prepareStatement("SELECT id FROM comments WHERE replyto=? AND NOT deleted FOR UPDATE");
    updateScore = db.prepareStatement("UPDATE users SET score=score+? WHERE id=(SELECT userid FROM comments WHERE id=?)");
  }

  public void deleteComment(int msgid, String reason, User user, int scoreBonus) throws SQLException, ScriptErrorException  {
    doDeleteComment(msgid, reason, user, scoreBonus);
  }

  private void doDeleteComment(int msgid, String reason, User user, int scoreBonus) throws SQLException, ScriptErrorException {
    if (!getReplys(msgid).isEmpty()) {
      throw new ScriptErrorException("Нельзя удалить комментарий с ответами");
    }

    deleteComment.clearParameters();
    insertDelinfo.clearParameters();

    deleteComment.setInt(1, msgid);
    insertDelinfo.setInt(1, msgid);
    insertDelinfo.setInt(2, user.getId());
    insertDelinfo.setString(3, reason+" ("+scoreBonus+')');

    updateScore.setInt(1, scoreBonus);
    updateScore.setInt(2, msgid);

    deleteComment.executeUpdate();
    insertDelinfo.executeUpdate();
    updateScore.executeUpdate();

    logger.info("Удалено сообщение " + msgid + " пользователем " + user.getNick() + " по причине `" + reason + '\'');
  }

  public List<Integer> deleteReplys(int msgid, User user, boolean score) throws SQLException, ScriptErrorException {
    return deleteReplys(msgid, user, score, 0);
  }

  private List<Integer> deleteReplys(int msgid, User user, boolean score, int depth) throws SQLException, ScriptErrorException {
    List<Integer> replys = getReplys(msgid);

    List<Integer> deleted = new LinkedList<Integer>();

    for (Integer r : replys) {
      deleted.addAll(deleteReplys(r, user, score, depth+1));
      deleted.add(r);

      switch (depth) {
        case 0:
          if (score) {
            doDeleteComment(r, "7.1 Ответ на некорректное сообщение (авто, уровень 0)", user, -2);
          } else {
            doDeleteComment(r, "7.1 Ответ на некорректное сообщение (авто)", user, 0);
          }
          break;
        case 1:
          if (score) {
            doDeleteComment(r, "7.1 Ответ на некорректное сообщение (авто, уровень 1)", user, -1);
          } else {
            doDeleteComment(r, "7.1 Ответ на некорректное сообщение (авто)", user, 0);
          }
          break;
        default:
          doDeleteComment(r, "7.1 Ответ на некорректное сообщение (авто, уровень >1)", user, 0);
          break;
      }
    }

    return deleted;
  }

  public List<Integer> getReplys(int msgid) throws SQLException {
    List<Integer> replys = new ArrayList<Integer>();

    replysForComment.setInt(1, msgid);

    ResultSet rs = replysForComment.executeQuery();

    while (rs.next()) {
      int r = rs.getInt("id");
      replys.add(r);
    }

    rs.close();
    return replys;
  }

  public void close() throws SQLException {
    deleteComment.close();
    insertDelinfo.close();
    replysForComment.close();
  }
}
