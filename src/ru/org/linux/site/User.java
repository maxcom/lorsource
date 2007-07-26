package ru.org.linux.site;

import java.net.URLEncoder;
import java.sql.*;
import java.util.Date;

import javax.servlet.http.HttpSession;

import ru.org.linux.util.StringUtil;

public class User {
  public static final int ANONYMOUS_LEVEL_SCORE = 50;

  private String nick;
  private final int id;
  private boolean canmod;
  private boolean candel;
  private boolean anonymous;
  private final boolean blocked;
  private final String password;
  private final int score;
  private final int maxScore;
  private final String photo;

  private final boolean activated;

  public User(Connection con, String name) throws SQLException, UserNotFoundException {
    if (name == null) {
      throw new NullPointerException();
    }
    nick = name;

    PreparedStatement st = con.prepareStatement("SELECT id,candel,canmod,passwd,blocked,score,max_score,activated,photo FROM users where nick=?");
    st.setString(1, name);

    ResultSet rs = st.executeQuery();

    if (!rs.next()) {
      throw new UserNotFoundException(name);
    }

    id = rs.getInt("id");
    canmod = rs.getBoolean("canmod");
    candel = rs.getBoolean("candel");
    activated = rs.getBoolean("activated");
    blocked = rs.getBoolean("blocked");
    score = rs.getInt("score");
    maxScore = rs.getInt("max_score");
    String pwd = rs.getString("passwd");
    if (pwd == null) {
      pwd = "";
    }
    anonymous = "".equals(pwd);
    password = pwd;

    photo=rs.getString("photo");

    rs.close();
    st.close();
  }

  public User(Connection con, int id) throws SQLException, UserNotFoundException {
    this.id = id;

    PreparedStatement st = con.prepareStatement("SELECT nick,score, max_score, candel,canmod,passwd,blocked,activated,photo FROM users where id=?");
    st.setInt(1, id);

    ResultSet rs = st.executeQuery();

    if (!rs.next()) {
      throw new UserNotFoundException(id);
    }

    nick = rs.getString("nick");
    canmod = rs.getBoolean("canmod");
    blocked = rs.getBoolean("blocked");
    candel = rs.getBoolean("candel");
    activated = rs.getBoolean("activated");
    String pwd = rs.getString("passwd");
    score = rs.getInt("score");
    maxScore = rs.getInt("max_score");
    if (pwd == null) {
      pwd = "";
    }
    password = pwd;
    anonymous = "".equals(pwd);
    photo=rs.getString("photo");

    rs.close();
    st.close();
  }

  public int getId() {
    return id;
  }

  public String getNick() {
    return nick;
  }

  public void checkPassword(String password) throws BadPasswordException {
    if (blocked) {
      throw new BadPasswordException(nick);
    }

    if (!password.equals(this.password)) {
      throw new BadPasswordException(nick);
    }
  }

  public void checkAnonymous() throws AccessViolationException {
    if (anonymous || blocked) {
      throw new AccessViolationException("Anonymous user");
    }
  }

  public void checkBlocked() throws AccessViolationException {
    if (blocked) {
      throw new AccessViolationException("Blocked user");
    }

    if (!activated) {
      throw new AccessViolationException("Not activated user");
    }
  }


  public void checkCommit() throws AccessViolationException {
    if (anonymous || blocked) {
      throw new AccessViolationException("Commit access denied for anonymous user");
    }
    if (!canmod) {
      throw new AccessViolationException("Commit access denied for user " + nick + " (" + id + ") ");
    }
  }

  public boolean isBlocked() {
    return blocked;
  }

  public void checkDelete() throws AccessViolationException {
    if (anonymous || blocked) {
      throw new AccessViolationException("Delete access denied for anonymous user");
    }
    if (!candel) {
      throw new AccessViolationException("Delete access denied for user " + nick + " (" + id + ") ");
    }
  }

  public boolean canModerate() {
    return canmod;
  }

  public boolean isAnonymous() {
    return anonymous;
  }

  public String getMD5(String base) {
    return StringUtil.md5hash(base + password);
  }

  public String getActivationCode(String base) {
    return StringUtil.md5hash(base + ":" + nick +":" + password);
  }

  public int getScore() {
    if (isAnonymous()) {
      return 0;
    } else {
      return score;
    }
  }

  public int getMaxScore() {
    if (isAnonymous()) {
      return 0;
    } else {
      return maxScore;
    }
  }

  public static String getStars(int score, int maxScore) {
    StringBuffer out = new StringBuffer();

    if (score < 0) {
      score = 0;
    }
    if (score >= 600) {
      score = 599;
    }
    if (maxScore < 0) {
      maxScore = 0;
    }
    if (maxScore >= 600) {
      maxScore = 599;
    }

    if (maxScore < score) {
      maxScore = score;
    }

    int stars = (int) Math.floor(score / 100.0);
    int greyStars = (int) Math.floor(maxScore / 100.0) - stars;

    for (int i = 0; i < stars; i++) {
      out.append("<img src=\"/img/normal-star.gif\" alt=\"*\">");
    }

    for (int i = 0; i < greyStars; i++) {
      out.append("<img src=\"/img/grey-star.gif\" alt=\"#\">");
    }

    return out.toString();
  }

  public String getStatus() {
    if (score < ANONYMOUS_LEVEL_SCORE) {
      return "анонимный";
    } else if (score < 100 && maxScore < 100) {
      return "новый пользователь";
    } else {
      return getStars(score, maxScore);
    }
  }

  /**
   * @param dbconn already opened database connection
   * @param nick   username
   * @param lTime
   */
  public static void updateUserLastlogin(Connection dbconn, String nick, Date lTime)
    throws SQLException {

    // update lastlogin time in database
    String sSql = "UPDATE users SET lastlogin=? WHERE nick=?";
    PreparedStatement pst = dbconn.prepareStatement(sSql);
    pst.setTimestamp(1, new Timestamp(lTime.getTime()));
    pst.setString(2, nick);
    pst.executeUpdate();
    pst.close();
    // getLogger().notice("template" , "User "+nick+" logged in.");
  }

  public boolean isBlockable() {
    if (id==2) {
      return false;
    }

    if (canModerate()) {
      return false;
    }               

    return maxScore<100;
  }

  public static String getUserInfoLine(Template tmpl, User user, Timestamp postdate) {
    return getUserInfoLine(tmpl, user.getNick(), user.getScore(), user.getMaxScore(), postdate);
  }

  public static String getUserInfoLine(Template tmpl, String nick, int userScore, int userMaxScore, Timestamp postdate) {
    StringBuffer out = new StringBuffer();

    out.append("<i>").append(nick).append(' ');

    if (!"anonymous".equals(nick)) {
      out.append(User.getStars(userScore, userMaxScore)).append(' ');

      if (tmpl.isModeratorSession())
        out.append("(Score: ").append(userScore).append(" MaxScore: ").append(userMaxScore).append(") ");
    }

    out.append("(<a href=\"whois.jsp?nick=").append(URLEncoder.encode(nick)).append("\">*</a>)");

    if (postdate!=null) {
      out.append(" (").append(Template.dateFormat.format(postdate)).append(")</i>");
    }

    return out.toString();
  }

  public String getCommitInfoLine(Timestamp postdate, Timestamp commitDate) {
    StringBuffer out = new StringBuffer();

    out.append("<i>Проверено: ").append(getNick()).append(" (<a href=\"whois.jsp?nick=").append(URLEncoder.encode(getNick())).append("\">*</a>)");
    if (commitDate!=null && !commitDate.equals(postdate))
      out.append(' ').append(Template.dateFormat.format(commitDate));
    out.append("</i>");

    return out.toString();
  }

  public static User getCurrentUser(Connection db, HttpSession session) throws SQLException, UserNotFoundException {
    if (!Template.isSessionAuthorized(session)) {
      return null;
    }

    return new User(db, (String) session.getAttribute("nick"));
  }

  public boolean isActivated() {
    return activated;
  }

  public String getPhoto() {
    return photo;
  }

  public void block(Connection db) throws SQLException {
    Statement st = null;

    try {
      st = db.createStatement();
      st.executeUpdate("UPDATE users SET blocked='t' WHERE id=" + id);
    } finally {
      if (st!=null) {
        st.close();
      }
    }
  }

  public String deleteAllComments(Connection db, User moderator) throws SQLException {
    Statement st = null;
    ResultSet rs = null;
    CommentDeleter deleter = null;

    StringBuilder out = new StringBuilder();

    try {
      deleter = new CommentDeleter(db);

      st = db.createStatement();

      rs = st.executeQuery("SELECT id FROM comments WHERE userid="+id+" AND not deleted ORDER BY id DESC FOR update");

      while(rs.next()) {
        int msgid = rs.getInt("id");

        out.append("Сообщение #").append(msgid).append("<br>");

        out.append(deleter.deleteReplys(msgid, moderator, true));
        out.append(deleter.deleteComment(msgid, "4.7 Flood (auto)", moderator, -20));

        out.append("<br>");
      }
    } finally {
      if (deleter!=null) {
        deleter.close();
      }

      if (rs!=null) {
        rs.close();
      }

      if (st!=null) {
        st.close();
      }
    }

    return out.toString();
  }
}
