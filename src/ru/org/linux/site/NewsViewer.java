package ru.org.linux.site;

import java.io.IOException;
import java.net.URLEncoder;
import java.sql.*;
import java.util.Properties;

import ru.org.linux.util.*;

public class NewsViewer implements Viewer {
  private final ProfileHashtable profile;
  private final Properties config;
  private boolean viewAll = false;
  private int section = 0;
  private int group = 0;
  private String datelimit = null;
  private String limit="";
  private boolean moderateMode = false;

  public NewsViewer(Properties Config, ProfileHashtable prof) {
    config = Config;
    profile = prof;
  }

  private String showCurrent(Connection db, ResultSet res) throws IOException, SQLException, UtilException {
    boolean multiPortal = (group==0 && section==0);

    StringBuffer out = new StringBuffer();
    int msgid = res.getInt("msgid");
    String url = res.getString("url");
    String subj = StringUtil.makeTitle(res.getString("subj"));
    String linktext = res.getString("linktext");
    boolean imagepost = res.getBoolean("imagepost");
    boolean votepoll = res.getBoolean("vote");
    boolean linkup = res.getBoolean("linkup");
    String image = res.getString("image");
    Timestamp lastmod = res.getTimestamp("lastmod");
    String messageText = res.getString("message");

    if (lastmod == null) {
      lastmod = new Timestamp(0);
    }
    double messages = profile.getInt("messages");
    boolean searchMode = profile.getBoolean("SearchMode");

    out.append("<hr noshade class=\"news-divider\">");
    out.append("<div class=news><h2>");

    String newslink;
    String jumplink;
    if (!searchMode) {
      newslink = "view-message.jsp?msgid=" + msgid;
      jumplink = "jump-message.jsp?msgid=" + msgid + "&amp;lastmod=" + lastmod.getTime();
    } else {
      newslink = "view-message.jsp?msgid=" + msgid;
      jumplink = newslink;
    }
    if (!linkup) {
      out.append("<a href=\"").append(jumplink).append("\">");
    } else {
      out.append("<a href=\"").append(url).append("\">");
    }
    if (multiPortal) {
      out.append('(').append(res.getString("pname")).append(") ");
    }
    out.append(subj);
    out.append("</a>");

    out.append("</h2>");

    if (image != null) {
      out.append("<div class=\"entry-userpic\">");
      out.append("<a href=\"view-news.jsp?section=").append(res.getInt("section")).append("&amp;group=").append(res.getInt("guid")).append("\">");
      try {
        ImageInfo info = new ImageInfo(config.getProperty("HTMLPathPrefix") + profile.getString("style") + image);
        out.append("<img src=\"/").append(profile.getString("style")).append(image).append("\" ").append(info.getCode()).append(" border=0 alt=\"Группа ").append(res.getString("gtitle")).append("\">");
      } catch (BadImageException e) {
        out.append("[bad image] <img class=newsimage src=\"/").append(profile.getString("style")).append(image).append("\" " + " border=0 alt=\"Группа ").append(res.getString("gtitle")).append("\">");
      }
      out.append("</a>");
      out.append("</div>");
    }

    out.append("<div class=\"entry-body\">");
    out.append("<div class=msg>\n");

    if (!votepoll) out.append(messageText);

    if (url != null && !imagepost && !votepoll && !linkup) {
      out.append("<p>&gt;&gt;&gt; <a href=\"").append(HTMLFormatter.htmlSpecialChars(url)).append("\">").append(linktext).append("</a>");
    } else if (imagepost) {
      try {
        try {
          ImageInfo iconInfo = new ImageInfo(config.getProperty("HTMLPathPrefix") + linktext);
          out.append("<p><a href=\"/").append(url).append("\"><img src=\"/").append(linktext).append("\" ALT=\"").append(subj).append("\" ").append(iconInfo.getCode()).append(" ></a>");
        } catch (BadImageException e) {
          out.append("<p><a href=\"/").append(url).append("\">[bad image!]<img src=\"/").append(linktext).append("\" ALT=\"").append(subj).append("\" " + " ></a>");
        } catch (IOException e) {
          out.append("<p><a href=\"/").append(url).append("\">[bad image - io exception!]<img src=\"/").append(linktext).append("\" ALT=\"").append(subj).append("\" " + " ></a>");
        }

        ImageInfo info = new ImageInfo(config.getProperty("HTMLPathPrefix") + url);

        out.append("<p><i>").append(info.getWidth()).append('x').append(info.getHeight()).append(", ").append(info.getSizeString()).append("</i>");

        out.append("<p>&gt;&gt;&gt; <a href=\"/").append(url).append("\">Просмотр</a>");
      } catch (BadImageException e) {
        out.append("<p>&gt;&gt;&gt; <a href=\"/").append(url).append("\">[BAD IMAGE!] Просмотр</a>");
      } catch (IOException e) {
        out.append("<p>&gt;&gt;&gt; <a href=\"/").append(url).append("\">[BAD IMAGE: IO Exception!] Просмотр</a>");
      }
    } else if (votepoll) {
      try {
        int id = Poll.getPollIdByTopic(db, msgid);
        Poll poll = new Poll(db, id);
	out.append(poll.renderPoll(db, config, profile));
        out.append("<p>&gt;&gt;&gt; <a href=\"").append("vote-vote.jsp?msgid=").append(msgid).append("\">Голосовать</a>");
        out.append("<p>&gt;&gt;&gt; <a href=\"").append(jumplink).append("\">Результаты</a>");
      } catch (BadImageException e) {
        out.append("<p>&gt;&gt;&gt; <a href=\"").append("\">[BAD POLL!] Просмотр</a>");
      } catch (PollNotFoundException e) {
        out.append("<p>&gt;&gt;&gt; <a href=\"").append("\">[BAD POLL!] Просмотр</a>");
      }
    }

    out.append("</div>");

    String nick = res.getString("nick");
    out.append("<div class=sign>").append(nick).append("(<a href=\"whois.jsp?nick=").append(URLEncoder.encode(nick)).append("\">*</a>) (").append(Template.dateFormat.format(res.getTimestamp("postdate"))).append(")</div>");

    if (!moderateMode && res.getBoolean("comment") && !searchMode) {
      out.append("<div class=\"nav\">");

      if (!res.getBoolean("expired"))
        out.append("[<a href=\"comment-message.jsp?msgid=").append(msgid).append("\">Добавить&nbsp;комментарий</a>]");

      int stat1 = res.getInt("stat1");

      if (stat1 > 0) {

	out.append(" [<a href=\"");

        if (searchMode)
     	  out.append(newslink);
        else
	  out.append(jumplink);
	  
	if (stat1 % 10 == 1 && stat1 % 100 != 11)
	    out.append("\">Добавлен&nbsp;").append(stat1);
	else
	    out.append("\">Добавлено&nbsp;").append(stat1);

	if (stat1 % 100 >= 10 && stat1 % 100 <= 20)
	  out.append("&nbsp;комментариев</a>");
	else
	  switch (stat1 % 10) {
	    case 1: out.append("&nbsp;комментарий</a>");break;
	    case 2:
	    case 3:
	    case 4: out.append("&nbsp;комментария</a>");break;
	    default: out.append("&nbsp;комментариев</a>");break;
	  }

	int pages = (int) Math.ceil(stat1 / messages);

	if (pages != 1){
	  out.append("&nbsp;(стр.");
	  for (int i = 0; i < pages; i++) {
	    out.append(" <a href=\"").append(jumplink).append("&amp;page=").append(i).append("\">").append(i + 1).append("</a>");
	  }
	  out.append(")");
	}
	out.append("]");

    }
      out.append("</div>");
    } else if (moderateMode) {
      out.append("<div class=nav>");
      out.append("[<a href=\"commit.jsp?msgid=").append(msgid).append("\">Подтвердить</a>]");
      out.append(" [<a href=\"delete.jsp?msgid=").append(msgid).append("\">Удалить</a>]");
      if (!votepoll)
	out.append(" [<a href=\"edit.jsp?msgid=").append(msgid).append("\">Править</a>]");
      else
        out.append(" [<a href=\"edit-vote.jsp?msgid=").append(msgid).append("\">Править</a>]");
      out.append("</div>");
    }
    out.append("</div>");

    out.append("</div>");

    return out.toString();
  }

  public String show(Connection db) throws IOException, SQLException, UtilException {
    StringBuffer buf = new StringBuffer();
    Statement st = db.createStatement();

    StringBuilder where = new StringBuilder(
        "sections.id=groups.section AND topics.id=msgbase.id AND topics.userid=users.id " +
            "AND topics.groupid=groups.id AND NOT deleted"
    );

    if (!viewAll) {
      where.append(" AND (topics.moderate OR NOT sections.moderate)");
    } else {
      where.append(" AND (NOT topics.moderate) AND sections.moderate");    
    }

    if (section!=0) {
      where.append(" AND section=").append(section);
    }

    if (group!=0) {
      where.append(" AND groupid=").append(group);
    }

    if (datelimit!=null) {
      where.append(" AND ").append(datelimit);
    }

    ResultSet res = st.executeQuery(
        "SELECT topics.title as subj, topics.lastmod, topics.stat1, postdate, nick, image, " +
            "groups.title as gtitle, topics.id as msgid, sections.comment, groups.id as guid, " +
            "topics.url, topics.linktext, imagepost, vote, sections.name as pname, linkup, " +
            "postdate<(CURRENT_TIMESTAMP-expire) as expired, message, sections.id as section " +
            "FROM topics,groups,users,sections,msgbase " +
            "WHERE " + where.toString()+" " +
            "ORDER BY commitdate DESC, msgid DESC "+limit
    );

    while (res.next()) {
      buf.append(showCurrent(db, res));
    }

    res.close();

    return buf.toString();
  }

  public void setViewAll(boolean viewAll) {
    this.viewAll = viewAll;
    moderateMode = viewAll;
  }

  public void setSection(int section) {
    this.section = section;
  }

  public void setGroup(int group) {
    this.group = group;
  }

  public void setDatelimit(String datelimit) {
    this.datelimit = datelimit;
  }

  public void setLimit(String limit) {
    this.limit = limit;
  }

  public String getVariantID(ProfileHashtable prof) throws UtilException {
    StringBuilder id = new StringBuilder("view-news?"+
        "SearchMode=" + prof.getBoolean("SearchMode") +
        "&topics=" + prof.getInt("topics")+
        "&messages=" + prof.getInt("messages") +
        "&style=" + prof.getString("style"));

    if (viewAll) {
      id.append("&view-all=true");
    }

    if (section!=0) {
      id.append("&section=").append(section);
    }

    if (group!=0) {
      id.append("&group=").append(group);
    }

    if (datelimit!=null) {
      id.append("&datelimit=").append(URLEncoder.encode(datelimit));
    }

    if (limit!=null && limit.length()>0) {
      id.append("&limit=").append(URLEncoder.encode(limit));
    }

    return id.toString();
  }

  public java.util.Date getExpire() {
    if (limit==null || limit.length()==0) {
      return new java.util.Date(new java.util.Date().getTime() + 10*60*1000);      
    }

    return new java.util.Date(new java.util.Date().getTime() + 60*1000);
  }

  public static NewsViewer getMainpage(Properties config, ProfileHashtable profile) {
//      Statement st = db.createStatement();
//      ResultSet rs = st.executeQuery("SELECT topics.stat1, topics.lastmod, topics.title as subj, postdate, nick, image, groups.title as gtitle, topics.id as msgid, sections.comment, groups.id as guid, topics.url, topics.linktext, sections.imagepost, sections.vote, linkup, postdate<(CURRENT_TIMESTAMP-expire) as expired, message FROM topics,groups,users,sections,msgbase WHERE sections.id=groups.section AND topics.id=msgbase.id AND topics.moderate AND topics.userid=users.id AND topics.groupid=groups.id AND section=1 AND NOT deleted AND commitdate>(CURRENT_TIMESTAMP-'1 month'::interval) ORDER BY commitdate DESC LIMIT 20");
    NewsViewer nw = new NewsViewer(config, profile);
    nw.setSection(1);
    nw.setLimit("LIMIT 20");
    nw.setDatelimit("commitdate>(CURRENT_TIMESTAMP-'1 month'::interval)");
    return nw;
  }
}
