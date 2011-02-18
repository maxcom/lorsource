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

import java.io.Serializable;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;
import java.util.regex.Pattern;

import org.apache.commons.lang.StringUtils;

public class Tags implements Serializable {
  private static final Pattern tagRE = Pattern.compile("([\\w\\ \\+-]+)", Pattern.CASE_INSENSITIVE);

  private final List<String> tags;
  private static final int TOP_TAGS_COUNT = 50;

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

  public Tags(Connection con, int msgid) throws SQLException {
    tags = new ArrayList<String>();
    
    PreparedStatement st = con.prepareStatement("SELECT tags_values.value FROM tags, tags_values WHERE tags.msgid=? AND tags_values.id=tags.tagid ORDER BY value");
    st.setInt(1, msgid);

    ResultSet rs = st.executeQuery();

    while (rs.next()) {
      tags.add(rs.getString("value"));
    }

    st.close();
  }

  public Tags(List<String> tags) {
    this.tags = tags;
  }

  @Override
  public String toString() {
    return toString(tags);
  }

  public static String toString(List<String> tags) {
    if (tags==null || tags.isEmpty()) {
      return "";
    }

    StringBuilder str = new StringBuilder();

    for (String tag : tags) {
      str.append(str.length() > 0 ? "," : "").append(tag);
    }

    return str.toString();
  }

  public List<String> getTags() {
    return tags;
  }

  public static SortedSet<String> getTopTags(Connection con) throws SQLException {
    SortedSet<String> set = new TreeSet<String>();
    PreparedStatement st = con.prepareStatement("SELECT counter,value FROM tags_values WHERE counter>1 ORDER BY counter DESC LIMIT " + TOP_TAGS_COUNT);
    ResultSet rs = st.executeQuery();

    while (rs.next()) {
      set.add(rs.getString("value"));
    }

    return set;
  }

  public static Map<String,Integer> getAllTags(Connection con) throws SQLException {
    Map<String,Integer> map = new TreeMap<String,Integer>();
    PreparedStatement st = con.prepareStatement("SELECT counter,value FROM tags_values WHERE counter>0");
    ResultSet rs = st.executeQuery();

    while (rs.next()) {
      map.put(rs.getString("value"),rs.getInt("counter"));
    }

    return map;
  }

  public static List<String> getMessageTags(Connection con, int msgid) throws SQLException {
    return new Tags(con, msgid).tags;
  }

  public static void checkTag(String tag) throws UserErrorException {
    // обработка тега: только буквы/цифры/пробелы, никаких спецсимволов, запятых, амперсандов и <>
    if (!tagRE.matcher(tag).matches()) {
      throw new UserErrorException("Invalid tag: '"+tag+ '\'');
    }
  }

  public static void updateCounters(Connection con, List<String> oldTags, List<String> newTags) throws SQLException {
    PreparedStatement stInc = con.prepareStatement("UPDATE tags_values SET counter=counter+1 WHERE id=?");
    PreparedStatement stDec = con.prepareStatement("UPDATE tags_values SET counter=counter-1 WHERE id=?");

    if (oldTags==null) {
      oldTags = Collections.emptyList();
    }

    for (String tag : newTags) {
      if (!oldTags.contains(tag)) {
        int id = getOrCreateTag(con, tag);
        stInc.setInt(1, id);
        stInc.executeUpdate();
      }
    }

    for (String tag : oldTags) {
      if (!newTags.contains(tag)) {
        int id = getOrCreateTag(con, tag);
        stDec.setInt(1, id);
        stDec.executeUpdate();
      }
    }
  }

  public static List<String> parseTags(String tags) throws UserErrorException {
    Set<String> tagSet = new HashSet<String>();

    // Теги разделяютчя пайпом или запятой
    tags = tags.replaceAll("\\|",",");
    String [] tagsArr = tags.split(",");

    if (tagsArr.length==0) {
      return Collections.emptyList();
    }

    for (String aTagsArr : tagsArr) {
      String tag = StringUtils.stripToNull(aTagsArr.toLowerCase());
      // плохой тег - выбрасываем
      if (tag == null) {
        continue;
      }

      // обработка тега: только буквы/цифры/пробелы, никаких спецсимволов, запятых, амперсандов и <>
      checkTag(tag);

      tagSet.add(tag);
    }

    return new ArrayList<String>(tagSet);
  }

  // TODO: move to JSP
  public static String getEditTags(Collection<String> tags) {
    StringBuilder out = new StringBuilder();
    boolean first = true;

    for (String tag : tags) {
      if (!first) {
        out.append(", ");
      }
      out.append("<a onclick=\"addTag('").append(tag).append("')\">");
      out.append(tag);
      out.append("</a>");
      first = false;
    }

    return out.toString();
  }

  public static boolean updateTags(Connection con, int msgid, List<String> tagList) throws SQLException {
    List<String> oldTags = getMessageTags(con, msgid);

    PreparedStatement insertStatement = con.prepareStatement("INSERT INTO tags VALUES(?,?)");
    PreparedStatement deleteStatement = con.prepareStatement("DELETE FROM tags WHERE msgid=? and tagid=?");

    insertStatement.setInt(1, msgid);
    deleteStatement.setInt(1, msgid);

    boolean modified = false;
    for (String tag : tagList) {
      if (!oldTags.contains(tag)) {
        int id = getOrCreateTag(con, tag);

        insertStatement.setInt(2, id);
        insertStatement.executeUpdate();
        modified = true;
      }
    }

    for (String tag : oldTags) {
      if (!tagList.contains(tag)) {
        int id = getOrCreateTag(con, tag);

        deleteStatement.setInt(2, id);
        deleteStatement.executeUpdate();
        modified = true;
      }
    }

    insertStatement.close();
    deleteStatement.close();

    return modified;
  }

  public static String getPlainTags(Connection con, int msgid) throws SQLException {
    return new Tags(con,msgid).toString();
  }

  public static String getTagLinks(Connection con, int msgid) throws SQLException {
    Tags tags = new Tags(con, msgid);

    return getTagLinks(tags);
  }

  public static String getTagLinks(Tags tags)  {
    List<String> mtags = tags.tags;

    StringBuilder buf = new StringBuilder();
    if (mtags.isEmpty()) {
      return "";
    }
    for (String mtag : mtags) {
      if (buf.length() > 0) {
        buf.append(", ");
      }

      try {
        buf.append("<a href=\"view-news.jsp?tag=").append(URLEncoder.encode(mtag, "UTF-8")).append("\">").append(mtag).append("</a>");
      } catch (UnsupportedEncodingException e) {
        throw new RuntimeException(e);
      }
    }
    
    return buf.toString();
  }
}
