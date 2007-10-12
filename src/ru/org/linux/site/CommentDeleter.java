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

  private final PreparedStatement st1;
  private final PreparedStatement st2;
  private final PreparedStatement st3;
  private final PreparedStatement st4;

  public CommentDeleter(Connection db) throws SQLException {
    st1 = db.prepareStatement("UPDATE comments SET deleted='t' WHERE id=?");
    st2 = db.prepareStatement("INSERT INTO del_info (msgid, delby, reason) values(?,?,?)");
    st3 = db.prepareStatement("SELECT id FROM comments WHERE replyto=? AND NOT deleted FOR UPDATE");
    st4 = db.prepareStatement("UPDATE users SET score=score+? WHERE id=(SELECT userid FROM comments WHERE id=?)");
  }

  public String deleteComment(int msgid, String reason, User user, int scoreBonus) throws SQLException {
    st1.clearParameters();
    st2.clearParameters();

    st1.setInt(1, msgid);
    st2.setInt(1, msgid);
    st2.setInt(2, user.getId());
    st2.setString(3, reason);

    st4.setInt(1, scoreBonus);
    st4.setInt(2, msgid);

    st1.executeUpdate();
    st2.executeUpdate();
    st4.executeUpdate();

    logger.info("Удалено сообщение " + msgid + " пользователем " + user.getNick() + " по причине `" + reason + '\'');
    return "Сообщение " + msgid + " удалено";
  }

  public String deleteReplys(int msgid, User user, boolean score) throws SQLException {
    return deleteReplys(msgid, user, score, 0);
  }

  private String deleteReplys(int msgid, User user, boolean score, int depth) throws SQLException {
    List<Integer> replys = new ArrayList<Integer>();
    StringBuffer out = new StringBuffer();

    st3.setInt(1, msgid);

    ResultSet rs = st3.executeQuery();

    while (rs.next()) {
      int r = rs.getInt("id");
      replys.add(r);
    }

    rs.close();

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

  public void close() throws SQLException {
    st1.close();
    st2.close();
    st3.close();
  }
}
