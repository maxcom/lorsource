package ru.org.linux.site;

import java.io.Serializable;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class DeleteInfo implements Serializable {
  private final String nick;
  private final int userid;
  private final String reason;

  private DeleteInfo(String nick, int userid, String reason) {
    this.nick = nick;
    this.reason = reason;
    this.userid = userid;
  }

  public static DeleteInfo getDeleteInfo(Connection db, int msgid) throws SQLException {
    PreparedStatement pst = null;
    try {
      pst = db.prepareStatement("SELECT nick,reason,users.id as userid FROM del_info,users WHERE msgid=? AND users.id=del_info.delby");

      pst.setInt(1, msgid);

      ResultSet rs = pst.executeQuery();

      if (!rs.next()) {
        return null;
      }

      return new DeleteInfo(rs.getString("nick"), rs.getInt("userid"), rs.getString("reason"));
    } finally {
      if (pst!=null) {
        pst.close();
      }
    }
  }

  public String getNick() {
    return nick;
  }

  public int getUserid() {
    return userid;
  }

  public String getReason() {
    return reason;
  }
}
