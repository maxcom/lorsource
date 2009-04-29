/*
 * Copyright 1998-2009 Linux.org.ru
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

import java.io.Serializable;
import java.net.URLEncoder;
import java.sql.*;
import java.text.DateFormat;
import java.util.Date;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import com.danga.MemCached.MemCachedClient;
import org.apache.commons.codec.binary.Base64;

import ru.org.linux.util.StringUtil;

public class User implements Serializable {
  private static final int ANONYMOUS_LEVEL_SCORE = 50;

  private String nick;
  private final int id;
  private boolean canmod;
  private boolean candel;
  private boolean anonymous;
  private boolean corrector;
  private final boolean blocked;
  private final String password;
  private final int score;
  private final int maxScore;
  private final String photo;

  private final boolean activated;
  public static final int CORRECTOR_SCORE = 100;
  private static final int BLOCK_MAX_SCORE = 400;

  private static final int CACHE_MILLIS = 300*1000;
  private static final int BLOCK_SCORE = 200;
  public static final int MAX_NICK_LENGTH = 40;

  private User(Connection con, String name) throws SQLException, UserNotFoundException {
    if (name == null) {
      throw new NullPointerException();
    }
    nick = name;

    PreparedStatement st = con.prepareStatement("SELECT id,candel,canmod,corrector,passwd,blocked,score,max_score,activated,photo FROM users where nick=?");
    st.setString(1, name);

    ResultSet rs = st.executeQuery();

    if (!rs.next()) {
      throw new UserNotFoundException(name);
    }

    id = rs.getInt("id");
    canmod = rs.getBoolean("canmod");
    candel = rs.getBoolean("candel");
    corrector = rs.getBoolean("corrector");
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

  private User(Connection con, int id) throws SQLException, UserNotFoundException {
    this.id = id;

    PreparedStatement st = con.prepareStatement("SELECT nick,score, max_score, candel,canmod,corrector,passwd,blocked,activated,photo FROM users where id=?");
    st.setInt(1, id);

    ResultSet rs = st.executeQuery();

    if (!rs.next()) {
      throw new UserNotFoundException(id);
    }

    nick = rs.getString("nick");
    canmod = rs.getBoolean("canmod");
    corrector = rs.getBoolean("corrector");
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

  public boolean matchPassword(String password) {
    return password.equals(this.password);
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

    if (anonymous) {
      throw new AccessViolationException("Anonymous user - disabled"); 
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

  public boolean canCorrect() {
    return corrector && score>= CORRECTOR_SCORE;
  }

  public boolean isAnonymous() {
    return anonymous;
  }

  public String getMD5(String base) {
    return StringUtil.md5hash(base + password);
  }

  public String getActivationCode(String base) {
    return StringUtil.md5hash(base + ':' + nick + ':' + password);
  }

  public int getScore() {
    if (anonymous) {
      return 0;
    } else {
      return score;
    }
  }

  public int getMaxScore() {
    if (anonymous) {
      return 0;
    } else {
      return maxScore;
    }
  }

  public static String getStars(int score, int maxScore) {
    StringBuilder out = new StringBuilder();

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

    return (maxScore < BLOCK_MAX_SCORE) && (score < BLOCK_SCORE);
  }

  public String getCommitInfoLine(Timestamp postdate, Timestamp commitDate) {
    DateFormat dateFormat = DateFormats.createDefault();

    StringBuilder out = new StringBuilder();

    out.append("<i>Проверено: ").append(nick).append(" (<a href=\"whois.jsp?nick=").append(URLEncoder.encode(nick)).append("\">*</a>)");
    if (commitDate!=null && !commitDate.equals(postdate)) {
      out.append(' ').append(dateFormat.format(commitDate));
    }
    out.append("</i>");

    return out.toString();
  }

  public static User getCurrentUser(Connection db, HttpSession session) throws SQLException, UserNotFoundException {
    if (!Template.isSessionAuthorized(session)) {
      return null;
    }

    return getUser(db, (String) session.getAttribute("nick"));
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
      updateCache(db);
    } finally {
      if (st!=null) {
        st.close();
      }
    }
  }

  public String deleteAllComments(Connection db, User moderator) throws SQLException, ScriptErrorException {
    Statement st = null;
    ResultSet rs = null;
    CommentDeleter deleter = null;

    StringBuilder out = new StringBuilder();

    try {
      // Delete user topics
      PreparedStatement lock = db.prepareStatement("SELECT id FROM topics WHERE userid=? AND not deleted FOR UPDATE");
      PreparedStatement st1 = db.prepareStatement("UPDATE topics SET deleted='t',sticky='f' WHERE id=?");
      PreparedStatement st2 = db.prepareStatement("INSERT INTO del_info (msgid, delby, reason) values(?,?,?)");
      lock.setInt(1, id);
      st2.setInt(2, moderator.id);
      st2.setString(3,"Автоматически: удаление всех коментариев");
      ResultSet lockResult = lock.executeQuery(); // lock another delete on this row
      while (lockResult.next()) {
        int mid = lockResult.getInt("id");
        st1.setInt(1,mid);
        st2.setInt(1,mid);
        st1.executeUpdate();
        st2.executeUpdate();
      }
      st1.close();
      st2.close();
      lockResult.close();
      lock.close();

      // Delete user comments
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

  public static User getUser(Connection con, String name) throws SQLException, UserNotFoundException {
    MemCachedClient mcc = MemCachedSettings.getClient();

    User user = new User(con, name);

    String shortCacheId = "User?id="+ user.id;

    String cacheId = MemCachedSettings.getId(shortCacheId);

    mcc.set(cacheId, user, new Date(new Date().getTime() + CACHE_MILLIS));

    return user;
  }

  public static User getUser(Connection db, int id) throws SQLException, UserNotFoundException {
    return getUser(db, id, false);
  }

  public static User getUserCached(Connection db, int id) throws SQLException, UserNotFoundException {
    return getUser(db, id, true);
  }

  private static User getUser(Connection db, int id, boolean useCache) throws SQLException, UserNotFoundException {
    MemCachedClient mcc = MemCachedSettings.getClient();

    String shortCacheId = "User?id="+id;

    String cacheId = MemCachedSettings.getId(shortCacheId);

    User res = null;

    if (useCache) {
      res = (User) mcc.get(cacheId);
    }

    if (res==null) {
      res = new User(db, id);
      mcc.set(cacheId, res, new Date(new Date().getTime() + CACHE_MILLIS));
    }

    return res;
  }

  public String getSignature(boolean moderatorMode, Date postdate) {
    DateFormat dateFormat = DateFormats.createDefault();

    StringBuilder out = new StringBuilder();

    if (blocked) {
      out.append("<s>");
    }

    out.append(nick);

    if (blocked) {
      out.append("</s>");
    }

    out.append(' ');

    if (!"anonymous".equals(nick)) {
      out.append(getStars(score, maxScore)).append(' ');
        if (moderatorMode) {
          out.append("(Score: ").append(score).append(" MaxScore: ").append(maxScore).append(") ");
        }
    }

    out.append("(<a href=\"whois.jsp?nick=").append(URLEncoder.encode(nick)).append("\">*</a>) (").append(dateFormat.format(postdate)).append(')');

    return out.toString();
  }

  public boolean isAnonymousScore() {
    return anonymous || blocked || score<ANONYMOUS_LEVEL_SCORE;
  }

  public void acegiSecurityHack(HttpServletResponse response, HttpSession session) {
    String username = nick;
    String cookieName = "ACEGI_SECURITY_HASHED_REMEMBER_ME_COOKIE"; //ACEGI_SECURITY_HASHED_REMEMBER_ME_COOKIE_KEY;
    long tokenValiditySeconds = 1209600; // 14 days
    String key = "jam35Wiki"; // from applicationContext-acegi-security.xml
    long expiryTime = System.currentTimeMillis() + (tokenValiditySeconds * 1000);

    // construct token to put in cookie; format is:
    // username + ":" + expiryTime + ":" + Md5Hex(username + ":" +
    // expiryTime + ":" + password + ":" + key)

    String signatureValue = StringUtil.md5hash(username + ':' + expiryTime + ':' + password + ':' + key);
    String tokenValue = username + ':' + expiryTime + ':' + signatureValue;
    String tokenValueBase64 = new String(Base64.encodeBase64(tokenValue.getBytes()));

    // Add remember me cookie
    Cookie acegi = new Cookie(cookieName, tokenValueBase64);
    acegi.setMaxAge(new Long(expiryTime).intValue());
    acegi.setPath("/wiki");
    response.addCookie(acegi);

    // Remove ACEGI_SECURITY_CONTEXT and session
    session.removeAttribute("ACEGI_SECURITY_CONTEXT"); // if any
  }

  private void updateCache(Connection db) throws SQLException {
    try {
      getUser(db, id);
    } catch (UserNotFoundException e) {
      throw new RuntimeException(e);
    }
  }

  public void changeScore(Connection db, int delta) throws SQLException {
    PreparedStatement st = null;
    try {
      st = db.prepareStatement("UPDATE users SET score=score+? WHERE id=?");
      st.setInt(1, delta);
      st.setInt(2, id);
      st.executeUpdate();

      updateCache(db);
    } finally {
      if (st!=null) {
        st.close();
      }
    }
  }

  public boolean isCorrector() {
    return corrector;
  }
}
