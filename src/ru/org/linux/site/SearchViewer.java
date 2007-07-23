package ru.org.linux.site;

import java.io.IOException;
import java.net.URLEncoder;
import java.sql.*;
import java.util.Date;

import ru.org.linux.util.HTMLFormatter;
import ru.org.linux.util.ProfileHashtable;
import ru.org.linux.util.UtilException;

public class SearchViewer implements Viewer {
  public final static int SEARCH_TOPICS = 1;
  public final static int SEARCH_ALL = 0;

  public final static int SEARCH_3MONTH = 1;
  public final static int SEARCH_YEAR = 2;

  public final static int SORT_R = 1;
  public final static int SORT_DATE = 2;

  private final String query;
  private int include = SEARCH_ALL;
  private int date = SEARCH_ALL;
  private int section = 0;
  private int sort = SORT_R;

  public SearchViewer(String query) {
    this.query = query;
  }

  public String show(Connection db) throws IOException, SQLException, UtilException {
    StringBuilder select = new StringBuilder("SELECT msgs.id, title, postdate, section, topic, rank(idxFTI, q) as rank, headline(message, q, 'HighlightAll=True') as headline");

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
      select.append(" AND section="+section);
    }

    if (sort==SORT_DATE) {
      select.append(" ORDER BY postdate DESC");
    } else {
      select.append(" ORDER BY rank DESC");
    }

    select.append(" LIMIT 100");

    PreparedStatement pst = db.prepareStatement(select.toString());

    pst.setString(1, query);

    ResultSet rs = pst.executeQuery();

    return printResults(rs);
  }

  private String printResults(ResultSet rs) throws SQLException {
    StringBuilder out = new StringBuilder("<h1>Результаты поиска</h1>");

    out.append("<div class=\"messages\"><div class=\"comment\"");

    while (rs.next()) {
      String title = rs.getString("title");
      int topic = rs.getInt("topic");
      int id = rs.getInt("id");
      String headline = rs.getString("headline");
      Timestamp postdate = rs.getTimestamp("postdate");

      String url;

      if (topic==0) {
        url = "jump-message.jsp?msgid="+id;
      } else {
        url = "jump-message.jsp?msgid="+topic+"#"+id;
      }

      out.append("<table width=\"100%\" cellspacing=0 cellpadding=0 border=0>");
      out.append("<tr class=body><td>");
      out.append("<div class=msg>");

      out.append("<h2><a href=\""+url+"\">"+ HTMLFormatter.htmlSpecialChars(title)+"</a></h2>");

      out.append("<p>"+headline+"</p>");

      out.append("<i>"+Template.dateFormat.format(postdate)+"</i>");

      out.append("</div></td></tr></table><p>");
    }

    out.append("</div></div>");

    return out.toString();
  }

  public String getVariantID(ProfileHashtable prof) throws UtilException {
    return "search?q="+ URLEncoder.encode(query)+"&include="+include+"&date="+date+"&section="+section+"&sort="+sort;
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
}
