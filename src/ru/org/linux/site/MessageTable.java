package ru.org.linux.site;

import java.sql.*;
import java.util.Date;

import ru.org.linux.util.HTMLFormatter;
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

  public static String getSectionRss(Connection db, int sectionid) throws SQLException, BadSectionException {
    StringBuilder out = new StringBuilder();

    Section section = new Section(db, sectionid);

    out.append("<title>Linux.org.ru: ").append(section.getName()).append("</title>");
    out.append("<pubDate>").append(Template.RFC822.format(new Date())).append("</pubDate>");
    out.append("<description>Linux.org.ru: ").append(section.getName()).append("</description>");

    Statement st=db.createStatement();

    ResultSet rs = st.executeQuery(
        "SELECT topics.title as subj, topics.lastmod, topics.stat1, postdate, nick, image, " +
            "groups.title as gtitle, topics.id as msgid, sections.comment, sections.vote, " +
            "groups.id as guid, topics.url, topics.linktext, imagepost, linkup, " +
            "postdate<(CURRENT_TIMESTAMP-expire) as expired, message " +
            "FROM topics,groups, users, sections, msgbase " +
            "WHERE sections.id=groups.section AND topics.id=msgbase.id " +
            "AND sections.id=" + sectionid + " AND (topics.moderate OR NOT sections.moderate) " +
            "AND topics.userid=users.id AND topics.groupid=groups.id AND NOT deleted " +
            "AND postdate>(CURRENT_TIMESTAMP-'3 month'::interval) " +
            "ORDER BY commitdate DESC LIMIT 10"
    );

    while (rs.next()) {
      int msgid = rs.getInt("msgid");
      boolean vote = rs.getBoolean("vote");
      if (vote) {
        int id = Poll.getPollIdByTopic(db, msgid);
        if (id > 0) {
          try {
            Poll poll = new Poll(db, id);
            out.append("<item>\n" + "  <title>").append(rs.getString("subj")).append("</title>\n" + "  <link>http://www.linux.org.ru/jump-message.jsp?msgid=").append(msgid).append("</link>\n" + "  <guid>http://www.linux.org.ru/jump-message.jsp?msgid=").append(msgid).append("</guid>\n" + "  <pubDate>").append(Template.RFC822.format(rs.getTimestamp("postdate"))).append("</pubDate>\n" + "  <description>\n" + "\t");
            out.append(HTMLFormatter.htmlSpecialChars(poll.renderPoll(db))).append("\n" + " \n" + "  </description>\n" + "</item>");
          } catch (PollNotFoundException e) {
          }
        }
      } else {
        out.append("<item>\n" + "  <title>").append(rs.getString("subj")).append("</title>\n" + "  <link>http://www.linux.org.ru/jump-message.jsp?msgid=").append(msgid).append("</link>\n" + "  <guid>http://www.linux.org.ru/jump-message.jsp?msgid=").append(msgid).append("</guid>\n" + "  <pubDate>").append(Template.RFC822.format(rs.getTimestamp("postdate"))).append("</pubDate>\n" + "  <description>\n" + "\t").append(HTMLFormatter.htmlSpecialChars(rs.getString("message"))).append("\n" + " \n" + "  </description>\n" + "</item>");
      }
    }

    return out.toString();
  }
}
