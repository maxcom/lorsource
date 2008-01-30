package ru.org.linux.site;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;

import gnu.regexp.RE;
import gnu.regexp.REException;
import org.apache.commons.lang.StringUtils;

public class Tags{
  private static final RE tagRE;  
  
  static {
    try {
      tagRE = new RE("([\\w\\ ]+)", RE.REG_ICASE);
    } catch (REException e) {
      throw new RuntimeException(e);
    }
  }                          
  
  private final ArrayList<String> tags;

  private static synchronized int getOrCreateTag(Connection con, String tag) throws SQLException {
    PreparedStatement st2 = con.prepareStatement("SELECT id FROM tags_values WHERE value=?");
    st2.setString(1,tag);
    ResultSet rs = st2.executeQuery();
    int id;

    if (rs.next()) {
      id = rs.getInt("id");
    } else {
      PreparedStatement st = con.prepareStatement("INSERT INTO tags_values (value) VALUES(?)");
      st.setString(1,tag);
      st.executeUpdate();
      st.close();
      
      rs = st2.executeQuery();
      rs.next();
      id = rs.getInt("id");
    }

    rs.close();
    st2.close();

    return id;
  }

  private Tags(Connection con, int msgid) throws SQLException {
    tags = new ArrayList<String>();
    
    PreparedStatement st = con.prepareStatement("SELECT tags_values.value FROM tags, tags_values WHERE tags.msgid=? AND tags_values.id=tags.tagid");
    st.setInt(1, msgid);

    ResultSet rs = st.executeQuery();

    while (rs.next()) {
      tags.add(rs.getString("value"));
    }
    st.close();
  }

  public String toString() {
    String str = "";
    if (tags==null || tags.isEmpty()) {
      return "";
    }
    for (String tag : tags) {
      str += (str.length() > 0 ? "," : "") + tag;
    }
    return str;
  }

  public ArrayList<String> getTags() {
    return tags;
  }

  public static Map<Integer, String> getCloud(Connection con) throws SQLException {
    Map<Integer, String> cloud = new HashMap<Integer, String>();
     PreparedStatement st = con.prepareStatement("SELECT counter,value FROM tags_values ORDER BY counter DESC LIMIT 10");
    ResultSet rs = st.executeQuery();
    while (rs.next()) {
      cloud.put(rs.getInt("counter"),rs.getString("value"));
    }
    return cloud;  
  }
  
  public static ArrayList<String> getMessageTags(Connection con, int msgid) throws SQLException {
    return new Tags(con, msgid).getTags();
  }

  public static void checkTag(String tag) throws UserErrorException {
    // обработка тега: только буквы/цифры/пробелы, никаких спецсимволов, запятых, амперсандов и <>
    if (!tagRE.isMatch(tag)) {
      throw new UserErrorException("Invalid tag: '"+tag+"'");
    }
  }

  public static void updateTags(Connection con, int msgid, String tags, boolean moderate) throws SQLException, UserErrorException {
    Set<String> tagSet = new HashSet<String>();

    // Теги разделяютчя пайпом или запятой
    tags = tags.replaceAll("\\|",",");
    String [] tagsArr = tags.split(",");

    if (tagsArr.length==0) {
      return;
    }

    for (int i = 0; i < tagsArr.length; i++) {
      String tag = StringUtils.stripToNull(tagsArr[i].toLowerCase());
      // плохой тег - выбрасываем
      if (tag==null) {
        continue;
      }

      // обработка тега: только буквы/цифры/пробелы, никаких спецсимволов, запятых, амперсандов и <>
      checkTag(tag);

      tagSet.add(tag);
    }

    PreparedStatement st1 = con.prepareStatement("SELECT tagid FROM tags WHERE msgid=? AND tagid=?");
    PreparedStatement st2 = con.prepareStatement("INSERT INTO tags VALUES(?,?)");
    PreparedStatement st3 = con.prepareStatement("UPDATE tags_values SET counter=counter+1 WHERE id=?");
    st1.setInt(1, msgid);
    st2.setInt(1, msgid);

    for (String tag : tagSet) {
      // Добавляем тег в таблицу tags_values и/или берем его id
      int id = getOrCreateTag(con, tag);

      // Есть ли этот тег у нас в таблице tags для этого топика?
      st1.setInt(2,id);
      ResultSet rs = st1.executeQuery();
      if (!rs.next()) {
        // Еще нет - вставляем
        st2.setInt(2,id);
        st2.executeUpdate();
      }
      rs.close();

      // Модерируем? прибавляем счетчик использования
      if (moderate) {
        st3.setInt(1,id);
        st3.executeUpdate();
      }
    }

    st3.close();
    st2.close();
    st1.close();
  }

  public static String getPlainTags(Connection con, int msgid) throws SQLException {
    return new Tags(con,msgid).toString();
  }
  
  public static String getTagLinks(Connection con, int msgid) throws SQLException {
    StringBuilder buf = new StringBuilder();
    Tags tags = new Tags(con, msgid);
    ArrayList<String> mtags = tags.getTags();
    if (mtags.isEmpty()) {
      return "";
    }
    for (String mtag : mtags) {
      if (buf.length() > 0) {
        buf.append(", ");
      } else {
        buf.append(" [метки: ");
      }
      try {
        buf.append("<a href=\"view-news.jsp?section=1&amp;tag=").append(URLEncoder.encode(mtag, "UTF-8")).append("\">").append(mtag).append("</a>");
      } catch (UnsupportedEncodingException e) {
        throw new RuntimeException(e);
      }
    }
    if (buf.length() > 0) {
      buf.append("]");
    }
    return buf.toString();
  }
}
