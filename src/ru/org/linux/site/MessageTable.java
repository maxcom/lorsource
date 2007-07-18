package ru.org.linux.site;

import java.io.IOException;
import java.sql.*;
import java.util.Date;

import ru.org.linux.util.BadImageException;
import ru.org.linux.util.HTMLFormatter;
import ru.org.linux.util.ImageInfo;
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

  public static String getSectionRss(Connection db, int sectionid, int groupid, String htmlPath, String fullUrl) throws SQLException, BadGroupException, BadSectionException {
    StringBuilder out = new StringBuilder();

    Section section = new Section(db, sectionid);
    Group group = null;
    if (groupid!=0) {
      group = new Group(db, groupid);
      if (group.getSectionId()!=sectionid) {
        throw new BadGroupException("группа #"+groupid+" не пренадлежит разделу #"+sectionid);
      }
    }

    out.append("<title>Linux.org.ru: ").append(section.getName());
    if (group!=null) {
      out.append(" - ").append(group.getTitle());
    }
    out.append("</title>");
    out.append("<pubDate>").append(Template.RFC822.format(new Date())).append("</pubDate>");
    out.append("<description>Linux.org.ru: ").append(section.getName());
    if (group!=null) {
      out.append(" - ").append(group.getTitle());
    }
    out.append("</description>");

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
            (group!=null?" AND groupid="+group.getId():"")+
            "ORDER BY commitdate DESC, postdate DESC LIMIT 10"
    );

    while (rs.next()) {
      int msgid = rs.getInt("msgid");
      boolean vote = rs.getBoolean("vote");
      String linktext = rs.getString("linktext");
      String url = rs.getString("url");
      String subj = rs.getString("subj");

      if (section.isImagepost()) {
        try {
          ImageInfo iconInfo = new ImageInfo(htmlPath + linktext);
          ImageInfo info = new ImageInfo(htmlPath + url);

          out.append("<item>");
          out.append("  <link>http://www.linux.org.ru/jump-message.jsp?msgid=").append(msgid).append("</link>\n");
          out.append("  <guid>http://www.linux.org.ru/jump-message.jsp?msgid=").append(msgid).append("</guid>\n");
          out.append("  <pubDate>").append(Template.RFC822.format(rs.getTimestamp("postdate"))).append("</pubDate>\n");
          out.append("  <title>").append(HTMLFormatter.htmlSpecialChars(subj)).append("</title>\n");

          out.append("  <description>\n" + "\t");
          out.append(HTMLFormatter.htmlSpecialChars(rs.getString("message")));
          out.append(HTMLFormatter.htmlSpecialChars("<p><img src=\""+fullUrl+linktext+"\" ALT=\""+subj+"\" "+iconInfo.getCode()+" >"));
          out.append(HTMLFormatter.htmlSpecialChars("<p><i>"+info.getWidth()+'x'+info.getHeight()+", "+info.getSizeString()+"</i>"));
          out.append("</description>\n");

          out.append("</item>");
        } catch (BadImageException e) {
          // TODO write to log
        } catch (IOException e) {
          // TODO write to log
        }
      } else if (vote) {
        int id = Poll.getPollIdByTopic(db, msgid);
        if (id > 0) {
          try {
            Poll poll = new Poll(db, id);
            out.append("<item>\n" + "  <title>").append(HTMLFormatter.htmlSpecialChars(subj)).append("</title>\n" + "  <link>http://www.linux.org.ru/jump-message.jsp?msgid=").append(msgid).append("</link>\n" + "  <guid>http://www.linux.org.ru/jump-message.jsp?msgid=").append(msgid).append("</guid>\n" + "  <pubDate>").append(Template.RFC822.format(rs.getTimestamp("postdate"))).append("</pubDate>\n" + "  <description>\n" + "\t");
            out.append(HTMLFormatter.htmlSpecialChars(poll.renderPoll(db, fullUrl))).append("\n" + " \n" + "  </description>\n" + "</item>");
          } catch (PollNotFoundException e) {
            // TODO write to log
          }
        }
      } else {
        out.append("<item>\n");
        out.append("  <title>").append(HTMLFormatter.htmlSpecialChars(subj)).append("</title>\n");
        out.append("  <link>http://www.linux.org.ru/jump-message.jsp?msgid=").append(msgid).append("</link>\n");
        out.append("  <guid>http://www.linux.org.ru/jump-message.jsp?msgid=").append(msgid).append("</guid>\n");
        out.append("  <pubDate>").append(Template.RFC822.format(rs.getTimestamp("postdate"))).append("</pubDate>\n");
        out.append("  <description>\n" + "\t").append(HTMLFormatter.htmlSpecialChars(rs.getString("message"))).append("</description>\n");
        out.append("</item>");
      }
    }

    return out.toString();
  }
}
