package ru.org.linux.site;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import ru.org.linux.util.StringUtil;

public class MessageTable {
  public static String showComments(Connection db, String nick) throws SQLException {
    StringBuilder out = new StringBuilder();

    PreparedStatement pst = db.prepareStatement("SELECT sections.name as ptitle, groups.title as gtitle, topics.title, topics.id as topicid, comments.id as msgid, comments.postdate FROM sections, groups, topics, comments, users WHERE sections.id=groups.section AND groups.id=topics.groupid AND comments.topic=topics.id AND comments.userid=users.id AND users.nick=? AND NOT comments.deleted ORDER BY postdate DESC LIMIT 50");
    pst.setString(1, nick);
    ResultSet rs=pst.executeQuery();

    while (rs.next()) {
      out.append("<tr><td>").append(rs.getString("ptitle")).append("</td><td>").append(rs.getString("gtitle")).append("</td><td><a href=\"jump-message.jsp?msgid=").append(rs.getInt("topicid")).append('#').append(rs.getInt("msgid")).append("\" rev=contents>").append(StringUtil.makeTitle(rs.getString("title"))).append("</a></td><td>").append(Template.dateFormat.format(rs.getTimestamp("postdate"))).append("</td></tr>");
    }

    rs.close();
    pst.close();

    return out.toString();
  }
}
