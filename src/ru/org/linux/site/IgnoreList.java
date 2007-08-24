package ru.org.linux.site;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Hashtable;
import java.util.Map;

public class IgnoreList {
  private final int userId;
  
  private boolean activated = true;
  private Map<Integer, String> ignoreList;

  public static Map<Integer, String> getIgnoreListHash(Connection db, String nick) throws SQLException {
    PreparedStatement pst = db.prepareStatement("SELECT a.ignored,b.nick FROM ignore_list a, users b WHERE a.userid=(SELECT id FROM users WHERE nick=?) AND b.id=a.ignored");
    pst.clearParameters();
    pst.setString(1, nick);
    ResultSet rs = pst.executeQuery();
    Map<Integer, String> cignored = new Hashtable<Integer, String>();
    while(rs.next()) {
      cignored.put(rs.getInt("ignored"),rs.getString("nick"));
    }
    return cignored;
  }

  public IgnoreList(Connection db, int userId) throws SQLException {
    if (userId<1)
	  throw new SQLException("Incorrect user ID");  
    this.userId = userId;
    ignoreList = new Hashtable<Integer, String>();
    PreparedStatement pst = db.prepareStatement("SELECT a.id,a.nick FROM users a, ignore_list b WHERE b.userid=? AND a.id=b.ignored ORDER BY a.nick ASC");
    pst.clearParameters();
    pst.setInt(1, userId); 
    ResultSet rst = pst.executeQuery();
    while (rst.next()) {
      ignoreList.put(rst.getInt("id"),rst.getString("nick"));
    }
  }

  public Map<Integer, String> getIgnoreList() {
    return ignoreList;
  }

  public boolean getActivated() {
    return activated;
  }

  public void setActivated(boolean activated) {
    this.activated = activated;
  }

  public void addNick(Connection db, String nick) throws SQLException, UserNotFoundException {
    User user = User.getUser(db, nick);

    int id = user.getId();
    if (!ignoreList.containsKey(id)) {
      PreparedStatement addPst = db.prepareStatement("INSERT INTO ignore_list (userid,ignored) VALUES(?,?)");
      addPst.clearParameters();
      addPst.setInt(1, userId);
      addPst.setInt(2, id);
      addPst.executeUpdate();
      ignoreList.put(id, nick);
    }
  }

  public boolean removeNick(Connection db, int uid) throws SQLException {
    if (!ignoreList.containsKey(uid))
      return false;
    PreparedStatement pst = db.prepareStatement("DELETE FROM ignore_list WHERE userid=? AND ignored=?"); 
    pst.clearParameters();
    pst.setInt(1,userId);
    pst.setInt(2,uid);
    boolean r = pst.executeUpdate() == 1;

    if (r) {
      ignoreList.remove(uid);
    }

    return r;
  }
}
