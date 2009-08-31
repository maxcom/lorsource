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

import java.io.File;
import java.io.IOException;
import java.net.URLEncoder;
import java.sql.*;
import java.text.DateFormat;
import java.util.Date;
import java.util.Properties;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import ru.org.linux.util.*;

public class NewsViewer implements Viewer {
  private static final Log logger = LogFactory.getLog("ru.org.linux");

  private final ProfileHashtable profile;
  private final Properties config;
  private boolean viewAll = false;
  private int section = 0;
  private int group = 0;
  private String datelimit = null;
  private String limit="";
  private String tag="";
  private boolean moderateMode = false;

  public NewsViewer(Properties config, ProfileHashtable prof) {
    this.config = config;
    profile = prof;
  }

  private String showCurrent(Connection db, Message msg) throws SQLException {
    DateFormat dateFormat = DateFormats.createDefault();

    boolean multiPortal = (group==0 && section==0);

    StringBuilder out = new StringBuilder();
    int msgid = msg.getId();
    String url = msg.getUrl();
    String subj = msg.getTitle();
    String linktext = msg.getLinktext();
    boolean imagepost = msg.getSection().isImagepost();
    boolean votepoll = msg.isVotePoll();

    Group group = null;
    try {
      group = new Group(db, msg.getGroupId());
    } catch (BadGroupException e) {
      throw new RuntimeException(e);
    }

    String image = group.getImage();
    Timestamp lastmod = msg.getLastModified();
    boolean expired = msg.isExpired();

    if (lastmod == null) {
      lastmod = new Timestamp(0);
    }
    double messages = profile.getInt("messages");

    out.append("<div class=news><h2>");

    String mainlink = "view-message.jsp?msgid=" + msgid;
    String jumplink;
    
    if (!expired) {
      jumplink = mainlink+ "&amp;lastmod=" + lastmod.getTime();
    } else {
      jumplink = mainlink;
    }

    out.append("<a href=\"").append(jumplink).append("\">");

    if (multiPortal) {
      out.append('(').append(msg.getSection().getTitle()).append(") ");
    }
    out.append(subj);
    out.append("</a>");

    out.append("</h2>");

    if (image != null) {
      out.append("<div class=\"entry-userpic\">");
      out.append("<a href=\"view-news.jsp?section=").append(msg.getSectionId()).append("&amp;group=").append(msg.getGroupId()).append("\">");
      try {
        ImageInfo info = new ImageInfo(config.getProperty("HTMLPathPrefix") + profile.getString("style") + image);
        out.append("<img src=\"/").append(profile.getString("style")).append(image).append("\" ").append(info.getCode()).append(" border=0 alt=\"Группа ").append(group.getTitle()).append("\">");
      } catch (IOException e) {
        logger.warn("Bad Image for group "+msg.getGroupId(), e);
        out.append("[bad image] <img class=newsimage src=\"/").append(profile.getString("style")).append(image).append("\" " + " border=0 alt=\"Группа ").append(group.getTitle()).append("\">");
      } catch (BadImageException e) {
        logger.warn("Bad Image for group "+msg.getGroupId(), e);
        out.append("[bad image] <img class=newsimage src=\"/").append(profile.getString("style")).append(image).append("\" " + " border=0 alt=\"Группа ").append(group.getTitle()).append("\">");
      }
      out.append("</a>");
      out.append("</div>");
    }

    out.append("<div class=\"entry-body\">");
    out.append("<div class=msg>\n");

    if (!votepoll) {
        out.append(msg.getProcessedMessage(db));
    }

    if (url != null && !imagepost && !votepoll) {
      out.append("<p>&gt;&gt;&gt; <a href=\"").append(HTMLFormatter.htmlSpecialChars(url)).append("\">").append(linktext).append("</a>");
    } else if (imagepost) {
      showMediumImage(config.getProperty("HTMLPathPrefix"), out, url, subj, linktext);
    } else if (votepoll) {
      try {
        Poll poll = Poll.getPollByTopic(db, msgid);
	out.append(poll.renderPoll(db, config, profile));
        out.append("<p>&gt;&gt;&gt; <a href=\"").append("vote-vote.jsp?msgid=").append(msgid).append("\">Голосовать</a>");
        out.append("<p>&gt;&gt;&gt; <a href=\"").append(jumplink).append("\">Результаты</a>");
      } catch (BadImageException e) {
        logger.warn("Bad Image for poll msgid="+msgid, e);
        out.append("<p>&gt;&gt;&gt; <a href=\"").append("\">[BAD POLL!] Просмотр</a>");
      } catch (IOException e) {
        logger.warn("Bad Image for poll msgid="+msgid, e);
        out.append("<p>&gt;&gt;&gt; <a href=\"").append("\">[BAD POLL!] Просмотр</a>");
      } catch (PollNotFoundException e) {
        logger.warn("Bad poll msgid="+msgid, e);
        out.append("<p>&gt;&gt;&gt; <a href=\"").append("\">[BAD POLL!] Просмотр</a>");
      }
    }

    out.append("</div>");

    if (msg.getSection().isPremoderated()) {
      String tagLinks = Tags.getTagLinks(db, msgid);

      if (tagLinks.length()>0) {
        out.append("<p class=\"tags\">Метки: <span class=tag>");
        out.append(tagLinks);
        out.append("<span></p>");
      }
    }

    User user = null;
    try {
      user = User.getUserCached(db, msg.getUid());
    } catch (UserNotFoundException e) {
      throw new RuntimeException(e);
    }

    String nick = user.getNick();
    out.append("<div class=sign>");
    out.append(user.getSignature(false, msg.getPostdate(), true));
    out.append("</div>");

    if (!moderateMode) {
      out.append("<div class=\"nav\">");

      if (!expired) {
        out.append("[<a href=\"comment-message.jsp?msgid=").append(msgid).append("\">Добавить&nbsp;комментарий</a>]");
      }

      int stat1 = msg.getCommentCount();

      if (stat1 > 0) {
        int pages = (int) Math.ceil(stat1 / messages);

	out.append(" [<a href=\"");

        if (pages<=1) {
          out.append(jumplink);
        } else {
          out.append(mainlink);          
        }

        out.append("\">");

//        if (stat1 % 10 == 1 && stat1 % 100 != 11) {
//          out.append("Добавлен&nbsp;");
//        } else {
//          out.append("Добавлено&nbsp;");
//        }

        out.append(stat1);

        if (stat1 % 100 >= 10 && stat1 % 100 <= 20) {
          out.append("&nbsp;комментариев</a>");
        } else {
          switch (stat1 % 10) {
            case 1:
              out.append("&nbsp;комментарий</a>");
              break;
            case 2:
            case 3:
            case 4:
              out.append("&nbsp;комментария</a>");
              break;
            default:
              out.append("&nbsp;комментариев</a>");
              break;
          }
        }

	if (pages != 1){
	  out.append("&nbsp;(стр.");
	  for (int i = 1; i < pages; i++) {
            if (i==pages-1) {
              out.append(" <a href=\"").append(jumplink).append("&amp;page=").append(i).append("\">").append(i + 1).append("</a>");
            } else {
              out.append(" <a href=\"").append(mainlink).append("&amp;page=").append(i).append("\">").append(i + 1).append("</a>");
            }
          }
	  out.append(')');
	}
	out.append(']');
      }

      out.append("</div>");
    } else if (moderateMode) {
      out.append("<div class=nav>");
      out.append("[<a href=\"commit.jsp?msgid=").append(msgid).append("\">Подтвердить</a>]");
      out.append(" [<a href=\"delete.jsp?msgid=").append(msgid).append("\">Удалить</a>]");
      if (!votepoll) {
        out.append(" [<a href=\"edit.jsp?msgid=").append(msgid).append("\">Править</a>]");
      }
      else {
        out.append(" [<a href=\"edit-vote.jsp?msgid=").append(msgid).append("\">Править</a>]");
      }

      out.append("</div>");
    }
    out.append("</div>");

    out.append("</div>");

    return out.toString();
  }

  public static void showMediumImage(String htmlPath, StringBuilder out, String url, String subj, String linktext) {
    try {
      out.append("<p>");
      String mediumName = ScreenshotProcessor.getMediumName(url);

      if (!new File(htmlPath, mediumName).exists()) {
        mediumName = linktext;
      }

      ImageInfo iconInfo = new ImageInfo(htmlPath + mediumName);
      ImageInfo info = new ImageInfo(htmlPath + url);

      out.append("<a href=\"/").append(url).append("\"><img src=\"/").append(mediumName).append("\" ALT=\"").append(subj).append("\" ").append(iconInfo.getCode()).append(" ></a>");
      out.append("</p><p>");


      out.append("&gt;&gt;&gt; <a href=\"/").append(url).append("\">Просмотр</a>");
      out.append(" (<i>").append(info.getWidth()).append('x').append(info.getHeight()).append(", ").append(info.getSizeString()).append("</i>)");
    } catch (BadImageException e) {
      logger.warn("Bad image", e);
      out.append("&gt;&gt;&gt; <a href=\"/").append(url).append("\">[BAD IMAGE!] Просмотр</a>");
    } catch (IOException e) {
      logger.warn("Bad image", e);
      out.append("&gt;&gt;&gt; <a href=\"/").append(url).append("\">[BAD IMAGE: IO Exception!] Просмотр</a>");
    }

    out.append("</p>");
  }

  @Override
  public String show(Connection db) throws SQLException, UtilException, UserErrorException {
    StringBuilder buf = new StringBuilder();
    Statement st = db.createStatement();

    StringBuilder where = new StringBuilder(
        "sections.id=groups.section AND topics.id=msgbase.id AND topics.userid=users.id " +
            "AND topics.groupid=groups.id AND NOT deleted"
    );

    if (!viewAll) {
      where.append(" AND topics.moderate AND sections.moderate");
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

    if (tag!=null && !"".equals(tag)) {
      PreparedStatement pst = db.prepareStatement("SELECT id FROM tags_values WHERE value=? AND counter>0");
      pst.setString(1,tag);
      ResultSet rs = pst.executeQuery();
      if (rs.next()) {
        int tagid=rs.getInt("id");
        if (tagid>0) {
          where.append(" AND topics.id IN (SELECT msgid FROM tags WHERE tagid=").append(tagid).append(')');
        }
      } else {
        throw new UserErrorException("Tag not found");
      }
      rs.close();
      pst.close();
    }
    
    ResultSet res = st.executeQuery(
      "SELECT " +
          "postdate, topics.id as msgid, users.id as userid, topics.title, " +
          "topics.groupid as guid, topics.url, topics.linktext, user_agents.name as useragent, " +
          "groups.title as gtitle, vote, havelink, section, topics.sticky, topics.postip, " +
          "postdate<(CURRENT_TIMESTAMP-sections.expire) as expired, deleted, lastmod, commitby, " +
          "commitdate, topics.stat1, postscore, topics.moderate, message, notop,bbcode " +
          "FROM topics " +
          "INNER JOIN users ON (users.id=topics.userid) " +
          "INNER JOIN groups ON (groups.id=topics.groupid) " +
          "INNER JOIN sections ON (sections.id=groups.section) " +
          "INNER JOIN msgbase ON (msgbase.id=topics.id) " +
          "LEFT JOIN user_agents ON (user_agents.id=topics.ua_id) " +

//        "SELECT topics.title as subj, topics.lastmod, topics.stat1, postdate, nick, image, " +
//            "groups.title as gtitle, topics.id as msgid, groups.id as guid, " +
//            "topics.url, topics.linktext, imagepost, vote, sections.name as pname, " +
//            "postdate<(CURRENT_TIMESTAMP-expire) as expired, message, bbcode, " +
//            "sections.id as section, NOT topics.sticky AS ssticky, sections.moderate " +
//            "FROM topics,groups,users,sections,msgbase " +
            "WHERE " + where+ ' ' +
            "ORDER BY sticky,commitdate DESC, msgid DESC "+limit
    );

    while (res.next()) {
      Message message = new Message(db, res);
      buf.append(showCurrent(db, message));
    }

    res.close();

    return buf.toString();
  }

  public void setViewAll(boolean viewAll) {
    this.viewAll = viewAll;
    moderateMode = viewAll;
  }

  public void setTag(String tag) {
    this.tag = tag;
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

  @Override
  public String getVariantID(ProfileHashtable prof) throws UtilException {
    StringBuilder id = new StringBuilder("view-news?"+
        "t=" + prof.getInt("topics")+
        "&m=" + prof.getInt("messages") +
        "&st=" + prof.getString("style") +
        "&tg=" + URLEncoder.encode(tag));

    if (viewAll) {
      id.append("&v-all=true");
    }

    if (section!=0) {
      id.append("&sec=").append(section);
    }

    if (group!=0) {
      id.append("&grp=").append(group);
    }

    if (datelimit!=null) {
      id.append("&dlmt=").append(URLEncoder.encode(datelimit));
    }

    if (limit!=null && limit.length()>0) {
      id.append("&lmt=").append(URLEncoder.encode(limit));
    }

    return id.toString();
  }

  @Override
  public Date getExpire() {
    if (limit==null || limit.length()==0) {
      return new Date(new Date().getTime() + 10*60*1000);
    }

    return new Date(new Date().getTime() + 60*1000);
  }

  public static NewsViewer getMainpage(Properties config, ProfileHashtable profile) {
    NewsViewer nw = new NewsViewer(config, profile);
    nw.section = 1;
    nw.limit = "LIMIT 20";
    nw.datelimit = "commitdate>(CURRENT_TIMESTAMP-'1 month'::interval)";
    return nw;
  }
}
