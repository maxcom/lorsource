package ru.org.linux.site;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.sql.*;
import java.util.Date;

import ru.org.linux.util.HTMLFormatter;
import ru.org.linux.util.ProfileHashtable;
import ru.org.linux.util.UtilException;

public class SearchViewer implements Viewer {
  public static final int SEARCH_TOPICS = 1;
  public static final int SEARCH_ALL = 0;

  public static final int SEARCH_3MONTH = 1;
  public static final int SEARCH_YEAR = 2;

  public static final int SORT_R = 1;
  public static final int SORT_DATE = 2;

  private final String query;
  private int include = SEARCH_ALL;
  private int date = SEARCH_ALL;
  private int section = 0;
  private int sort = SORT_R;

  private String username = "";

  public SearchViewer(String query) {
    this.query = query;
  }

  public String show(Connection db) throws IOException, SQLException, UtilException, UserErrorException {
    StringBuilder select = new StringBuilder("SELECT id, title, postdate, section, topic, userid, rank, headline(message, q, 'HighlightAll=True') as headline FROM ("+
        "SELECT " +
        "msgs.id, title, postdate, section, topic, userid, rank(idxFTI, q) as rank,message");

    if (include==SEARCH_ALL) {
      select.append(" FROM msgs_and_cmts as msgs, msgbase, plainto_tsquery(?) as q");
    } else {
      select.append(" FROM msgs, msgbase, plainto_tsquery(?) as q");
    }

    select.append(" WHERE msgs.id = msgbase.id AND idxFTI @@ q");

    if (date==SEARCH_3MONTH) {
      select.append(" AND postdate>CURRENT_TIMESTAMP-'3 month'::interval");
    } else if (date == SEARCH_YEAR) {
      select.append(" AND postdate>CURRENT_TIMESTAMP-'1 year'::interval");
    }

    if (section!=0) {
      select.append(" AND section=").append(section);
    }

    if (username.length()>0) {
      try {
        User user = User.getUser(db, username);

        select.append(" AND userid=").append(user.getId());
      } catch (UserNotFoundException ex) {
        throw new UserErrorException("User not found: "+username);
      }
    }

    if (sort==SORT_DATE) {
      select.append(" ORDER BY postdate DESC");
    } else {
      select.append(" ORDER BY rank DESC");
    }

    select.append(" LIMIT 100) as qq, plainto_tsquery(?) as q");

    PreparedStatement pst = null;
    try {
      pst = db.prepareStatement(select.toString());

      pst.setString(1, query);
      pst.setString(2, query);

      ResultSet rs = pst.executeQuery();

      return printResults(db, rs);
    } catch (UserNotFoundException ex) {
      throw new RuntimeException(ex);
    } finally {
      if (pst!=null) {
        pst.close();
      }
    }
  }

  private String printResults(Connection db, ResultSet rs) throws SQLException, UserNotFoundException {
    StringBuilder out = new StringBuilder("<h1>Результаты поиска</h1>");

    out.append("<div class=\"messages\"><div class=\"comment\">");

    while (rs.next()) {
      String title = rs.getString("title");
      int topic = rs.getInt("topic");
      int id = rs.getInt("id");
      String headline = rs.getString("headline");
      Timestamp postdate = rs.getTimestamp("postdate");
      int userid = rs.getInt("userid");
      User user = User.getUserCached(db, userid);

      String url;

      if (topic==0) {
        url = "view-message.jsp?msgid="+id;
      } else {
        url = "jump-message.jsp?msgid="+topic+"&amp;cid="+id;
      }

      out.append("<table width=\"100%\" cellspacing=0 cellpadding=0 border=0>");
      out.append("<tr class=body><td>");
      out.append("<div class=msg>");

      out.append("<h2><a href=\"").append(url).append("\">").append(HTMLFormatter.htmlSpecialChars(title)).append("</a></h2>");

      out.append("<p>").append(headline).append("</p>");

      out.append("<div class=sign>");
      out.append(user.getSignature(false, postdate));
      out.append("</div>");

      out.append("</div></td></tr></table><p>");
    }

    out.append("</div></div>");

    return out.toString();
  }

  public String getVariantID(ProfileHashtable prof) throws UtilException {
    try {
      return "search?q="+ URLEncoder.encode(query, "koi8-r")+"&include="+include+"&date="+date+"&section="+section+"&sort="+sort+"&username="+URLEncoder.encode(username);
    } catch (UnsupportedEncodingException e) {
      throw new RuntimeException(e);
    }
  }

  public Date getExpire() {
    return new java.util.Date(new java.util.Date().getTime() + 15*60*1000);
  }

  public static int parseInclude(String include) {
    if (include==null) {
      return SEARCH_ALL;
    }

    if (include.equals("topics")) {
      return SEARCH_TOPICS;
    }

    return SEARCH_ALL;
  }

  public static int parseDate(String date) {
    if (date==null) {
      return SEARCH_YEAR;
    }

    if (date.equals("3month")) {
      return SEARCH_3MONTH;
    }

    if (date.equals("all")) {
      return SEARCH_ALL;
    }

    return SEARCH_YEAR;
  }

  public void setInclude(int include) {
    this.include = include;
  }

  public void setDate(int date) {
    this.date = date;
  }

  public void setSection(int section) {
    this.section = section;
  }

  public void setSort(int sort) {
    this.sort = sort;
  }

  public void setUser(String username) {
    this.username = username;
  }
}
