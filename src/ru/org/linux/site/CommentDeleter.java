package ru.org.linux.site;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import ru.org.linux.logger.Logger;

public class CommentDeleter {
  private final PreparedStatement st1;
  private final PreparedStatement st2;
  private final PreparedStatement st3;
  private final PreparedStatement st4;
  private final Logger logger;

  public CommentDeleter(Connection db, Logger logger) throws SQLException {
    st1 = db.prepareStatement("UPDATE comments SET deleted='t' WHERE id=?");
    st2 = db.prepareStatement("INSERT INTO del_info (msgid, delby, reason) values(?,?,?)");
    st3 = db.prepareStatement("SELECT id FROM comments WHERE replyto=? AND NOT deleted");
    st4 = db.prepareStatement("UPDATE users SET score=score+? WHERE id=(SELECT userid FROM comments WHERE id=?)");
    this.logger = logger;
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

    logger.notice("delete", "Удалено сообщение " + msgid + " пользователем " + user.getNick() + " по причине `" + reason + '\'');
    return "Сообщение " + msgid + " удалено";
  }

  public String deleteReplys(int msgid, User user, boolean score) throws SQLException {
    return deleteReplys(msgid, user, score, 0);
  }

  public String deleteReplys(int msgid, User user, boolean score, int depth) throws SQLException {
    List replys = new ArrayList();
    StringBuffer out = new StringBuffer();

    st3.setInt(1, msgid);

    ResultSet rs = st3.executeQuery();

    while (rs.next()) {
      int r = rs.getInt("id");
      replys.add(new Integer(r));
    }

    rs.close();

    for (Iterator i = replys.iterator(); i.hasNext();) {
      int r = ((Integer) i.next()).intValue();
      out.append(deleteReplys(r, user, score, depth++));
      out.append("Удаляем ответ " + r + "<br>");
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
