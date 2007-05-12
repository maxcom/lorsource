package ru.org.linux.site;

import java.io.IOException;
import java.net.URLEncoder;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.Properties;

import ru.org.linux.util.*;

public class NewsViewer {
  private final ResultSet res;
  private final boolean moderateMode;
  private final boolean multiPortal;
  private final ProfileHashtable profile;
  private final Properties config;

  public NewsViewer(Properties Config, ProfileHashtable prof, ResultSet r, boolean Moderate, boolean Portals) {
    config = Config;
    res = r;
    moderateMode = Moderate;
    multiPortal = Portals;
    profile = prof;
  }

  public String showCurrent() throws IOException, SQLException, UtilException {
    StringBuffer out = new StringBuffer();
    int msgid = res.getInt("msgid");
    String url = res.getString("url");
    String subj = StringUtil.makeTitle(res.getString("subj"));
    String linktext = res.getString("linktext");
    boolean imagepost = res.getBoolean("imagepost");
    boolean linkup = res.getBoolean("linkup");
//    Storage storage = (Storage) profile.getObjectProperty("Storage");
    String image = res.getString("image");
    Timestamp lastmod = res.getTimestamp("lastmod");
    String messageText = res.getString("message");

    if (lastmod == null) {
      lastmod = new Timestamp(0);
    }
    double messages = profile.getIntProperty("messages");
    boolean searchMode = profile.getBooleanProperty("SearchMode");

    out.append("<hr noshade class=\"news-divider\">");
    out.append("<div class=news><h2>");

    final String newslink;
    final String jumplink;
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

    final int columnWidth;

    if (imagepost) { /* gallery */
      columnWidth = 160;
    } else { /* news */
      columnWidth = 70;
    }

    if (image != null) {
      out.append("<a href=\"group.jsp?group=").append(res.getInt("guid")).append("\">");
      try {
        ImageInfo info = new ImageInfo(config.getProperty("HTMLPathPrefix") + profile.getStringProperty("style") + image);
        int width = info.getWidth();
        int margin = (columnWidth - width) / 2;
        if (margin < 0) {
          margin = 0;
        }
        out.append("<img style=\"margin-left: ").append(margin).append("px; margin-right: ").append(margin).append("\" class=newsimage src=\"/").append(profile.getStringProperty("style")).append(image).append("\" ").append(info.getCode()).append(" border=0 alt=\"Группа ").append(res.getString("gtitle")).append("\">");
      } catch (BadImageException e) {
        out.append("[bad image] <img class=newsimage src=\"/").append(profile.getStringProperty("style")).append(image).append("\" " + " border=0 alt=\"Группа ").append(res.getString("gtitle")).append("\">");
      }
      out.append("</a>");
    } else if (imagepost) {
      try {
        ImageInfo info = new ImageInfo(config.getProperty("HTMLPathPrefix") + linktext);
        out.append("<a href=\"").append(url).append("\"><img class=newsimage src=\"/").append(linktext).append("\" ALT=\"").append(subj).append("\" ").append(info.getCode()).append(" ></a>");
      } catch (BadImageException e) {
        out.append("<a href=\"").append(url).append("\">[bad image!]<img class=newsimage src=\"/").append(linktext).append("\" ALT=\"").append(subj).append("\" " + " ></a>");
      } catch (IOException e) {
        out.append("<a href=\"").append(url).append("\">[bad image - io exception!]<img class=newsimage src=\"/").append(linktext).append("\" ALT=\"").append(subj).append("\" " + " ></a>");
      }
    } else {
      out.append("<a href=\"group.jsp?group=").append(res.getInt("guid")).append("\">");
      out.append(res.getString("gtitle"));
      out.append("</a>");
    }

    out.append("<div class=\"msg-placeholder\" style=\"margin-left: ").append(columnWidth).append("px\">");
    out.append("<div class=msg>\n");

//    out.append(storage.readMessage("msgbase", String.valueOf(msgid)));
    out.append(messageText);

    if (url != null && !imagepost && !linkup) {
      out.append("<p>&gt;&gt;&gt; <a href=\"").append(HTMLFormatter.htmlSpecialChars(url)).append("\">").append(linktext).append("</a>");
    } else if (imagepost) {
      try {
        ImageInfo info = new ImageInfo(config.getProperty("HTMLPathPrefix") + url);

        out.append("<p><i>").append(info.getWidth()).append('x').append(info.getHeight()).append(", ").append(info.getSizeString()).append("</i>");

        out.append("<p>&gt;&gt;&gt; <a href=\"").append(url).append("\">Просмотр</a>");
      } catch (BadImageException e) {
        out.append("<p>&gt;&gt;&gt; <a href=\"").append(url).append("\">[BAD IMAGE!] Просмотр</a>");
      } catch (IOException e) {
        out.append("<p>&gt;&gt;&gt; <a href=\"").append(url).append("\">[BAD IMAGE: IO Exception!] Просмотр</a>");
      }
    }

    out.append("</div>");

    String nick = res.getString("nick");
    out.append("<div class=sign>").append(nick).append("(<a href=\"whois.jsp?nick=").append(URLEncoder.encode(nick)).append("\">*</a>) (").append(Template.dateFormat.format(res.getTimestamp("postdate"))).append(")</div>");

    if (!moderateMode && res.getBoolean("comment") && !searchMode) {
      out.append("<div class=\"nav\">");

      if (!res.getBoolean("expired"))
        out.append("[&nbsp;<a href=\"comment-message.jsp?msgid=").append(msgid).append("\">Добавить&nbsp;комментарий</a>&nbsp;]");

      int stat1 = res.getInt("stat1");

      if (stat1 > 0) {

	out.append("&nbsp;[&nbsp;<a href=\"");

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
	out.append("&nbsp;]");

    }
      out.append("</div>");
    } else if (moderateMode) {
      out.append("<div class=nav>");
      out.append("[<a href=\"commit.jsp?msgid=").append(msgid).append("\">Подтвердить</a>]");
      out.append(" [<a href=\"delete.jsp?msgid=").append(msgid).append("\">Удалить</a>]");
	out.append(" [<a href=\"edit.jsp?msgid=").append(msgid).append("\">Править</a>]");
      out.append("</div>");
    }
    out.append("</div>");

    out.append("</div>");

    return out.toString();
  }

  public String showAll() throws IOException, SQLException, UtilException {
    StringBuffer buf = new StringBuffer();

    while (res.next()) {
      buf.append(showCurrent());
    }

    return buf.toString();
  }

}
