package ru.org.linux.site;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

public class CommentDeleter {
  private static final Logger logger = Logger.getLogger("ru.org.linux");

  private final PreparedStatement deleteComment;
  private final PreparedStatement insertDelinfo;
  private final PreparedStatement replysForComment;
  private final PreparedStatement updateScore;

  public CommentDeleter(Connection db) throws SQLException {
    deleteComment = db.prepareStatement("UPDATE comments SET deleted='t' WHERE id=?");
    insertDelinfo = db.prepareStatement("INSERT INTO del_info (msgid, delby, reason) values(?,?,?)");
    replysForComment = db.prepareStatement("SELECT id FROM comments WHERE replyto=? AND NOT deleted FOR UPDATE");
    updateScore = db.prepareStatement("UPDATE users SET score=score+? WHERE id=(SELECT userid FROM comments WHERE id=?)");
  }

  public String deleteComment(int msgid, String reason, User user, int scoreBonus) throws SQLException, ScriptErrorException {
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
    return "Сообщение " + msgid + " удалено";
  }

  public String deleteReplys(int msgid, User user, boolean score) throws SQLException, ScriptErrorException {
    return deleteReplys(msgid, user, score, 0);
  }

  private String deleteReplys(int msgid, User user, boolean score, int depth) throws SQLException, ScriptErrorException {
    List<Integer> replys = getReplys(msgid);

    StringBuilder out = new StringBuilder();

    for (Integer r : replys) {
      out.append(deleteReplys(r, user, score, depth+1));
      out.append("Удаляем ответ ").append(r).append("<br>");
      switch (depth) {
        case 0:
          if (score) {
            deleteComment(r, "7.1 Ответ на некорректное сообщение (авто, уровень 0)", user, -2);
          } else {
            deleteComment(r, "7.1 Ответ на некорректное сообщение (авто)", user, 0);
          }
          break;
        case 1:
          if (score) {
            deleteComment(r, "7.1 Ответ на некорректное сообщение (авто, уровень 1)", user, -1);
          } else {
            deleteComment(r, "7.1 Ответ на некорректное сообщение (авто)", user, 0);
          }
          break;
        default:
          deleteComment(r, "7.1 Ответ на некорректное сообщение (авто, уровень >1)", user, 0);
          break;
      }
    }

    return out.toString();
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
