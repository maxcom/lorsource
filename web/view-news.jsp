<%@ page contentType="text/html; charset=koi8-r"%>
<%@ page import="java.sql.Connection,java.sql.Statement,java.util.Calendar,ru.org.linux.site.*" errorPage="/error.jsp" buffer="200kb"%>
<%@ page import="ru.org.linux.util.DateUtil" %>
<% Template tmpl = new Template(request, config, response); %>
<%= tmpl.head() %>
<%
  Connection db = null;
  try {
    if (request.getParameter("month") == null) {
      response.setDateHeader("Expires", System.currentTimeMillis()+60*1000);
      response.setDateHeader("Last-Modified", System.currentTimeMillis());
    } else {
      long expires = System.currentTimeMillis()+30*24*60*60*1000L;

      response.setDateHeader("Expires", System.currentTimeMillis()+30*24*60*60*1000L);

      int month=tmpl.getParameters().getInt("month");
      int year=tmpl.getParameters().getInt("year");

      Calendar calendar = Calendar.getInstance();
      calendar.set(year, month-1, 1);
      calendar.add(Calendar.MONTH, 1);

      long lastmod = calendar.getTimeInMillis();

      if (lastmod<System.currentTimeMillis()) {
        response.setDateHeader("Expires", expires);
        response.setDateHeader("Last-Modified", lastmod);
      } else {
        response.setDateHeader("Expires", System.currentTimeMillis()+60*1000);
        response.setDateHeader("Last-Modified", System.currentTimeMillis());
      }
    }
%>

<%
  int sectionid = tmpl.getParameters().getInt("section");
  Group group = null;

  db = tmpl.getConnection();

  Section section = new Section(db, sectionid);

  if (request.getParameter("group") != null) {
    int groupid = tmpl.getParameters().getInt("group");
    group = new Group(db, groupid);

    if (group.getSectionId() != sectionid) {
      throw new ScriptErrorException("группа #" + groupid + " не пренадлежит разделу #" + sectionid);
    }
  }

  Statement st = db.createStatement();

  String navtitle = section.getName();

  if (group != null) {
    navtitle = "<a href=\"view-news.jsp?section=" + section.getId() + "\">" + section.getName() + "</a> - " + group.getTitle();
  }

  int month = 0;
  int year = 0;
  String ptitle;

  if (request.getParameter("month") == null) {
    ptitle = section.getName();
    if (group != null) {
      ptitle += " - " + group.getTitle();
    }
  } else {
    month = tmpl.getParameters().getInt("month");
    year = tmpl.getParameters().getInt("year");
    ptitle = "Архив: " + section.getName();

    if (group != null) {
      ptitle += " - " + group.getTitle();
    }

    ptitle += ", " + year + ", " + DateUtil.getMonth(month);
    navtitle += " - Архив " + year + ", " + DateUtil.getMonth(month);
  }

  if (!section.isBrowsable() || sectionid == 2) {
    throw new BadSectionException(sectionid);
  }

%>
	<title><%= ptitle %></title>
        <LINK REL="alternate" HREF="section-rss.jsp?section=<%= sectionid %><%= (group!=null?("&amp;group="+group.getId()):"")%>" TYPE="application/rss+xml">

<%= tmpl.DocumentHeader() %>

  <table class=nav><tr>
    <td align=left valign=middle>
      <strong><%= navtitle %></strong>
    </td>
    <td align=right valign=middle>
<%
  if (month==0) {
      if (section.isImagepost()) {
        out.print("[<a href=\"add.jsp?group=4962\">Добавить изображение</a>]");
      } else if (section.isVotePoll()) {
        out.print("[<a href=\"add-poll.jsp?group=19387\">Добавить голосование</a>]");
      } else {
        if (group==null) {
          out.print("[<a href=\"add-section.jsp?section="+section.getId()+"\">Добавить</a>]");
        } else {
          out.print("[<a href=\"add.jsp?group="+group.getId()+"\">Добавить</a>]");
        }
      }

    if (group==null) {
      out.print("[<a href=\"view-section.jsp?section="+section.getId()+"\">Таблица</a>]");
    } else {
      out.print("[<a href=\"group.jsp?group="+group.getId()+"\">Таблица</a>]");
    }
  }
  out.print("[<a href=\"view-news-archive.jsp?section="+sectionid+"\">Архив</a>]");
  out.print("[<a href=\"section-rss.jsp?section="+sectionid+(group!=null?("&amp;group="+group.getId()):"")+"\">RSS</a>]");

%>
    </td>
   </tr>
</table>

<H1><%= ptitle %></H1>
<%
  //  ResultSet rs;

//  if (month != 0) {
//    rs = st.executeQuery("SELECT topics.title as subj, topics.lastmod, topics.stat1, postdate, nick, image, groups.title as gtitle, topics.id as msgid, sections.comment, groups.id as guid, topics.url, topics.linktext, imagepost, vote, linkup, postdate<(CURRENT_TIMESTAMP-expire) as expired, message FROM topics,groups,users,sections,msgbase WHERE sections.id=groups.section AND topics.id=msgbase.id AND (topics.moderate OR NOT sections.moderate) AND topics.userid=users.id AND topics.groupid=groups.id AND section=" + sectionid + (group!=null?" AND groupid="+group.getId():"") +" AND postdate>='" + year + "-" + month + "-01'::timestamp AND (postdate<'" + year + "-" + month + "-01'::timestamp+'1 month'::interval) AND NOT deleted ORDER BY commitdate");
//  } else {
//    rs = st.executeQuery("SELECT topics.title as subj, topics.lastmod, topics.stat1, postdate, nick, image, groups.title as gtitle, topics.id as msgid, sections.comment, groups.id as guid, topics.url, topics.linktext, imagepost, vote, linkup, postdate<(CURRENT_TIMESTAMP-expire) as expired, message FROM topics,groups,users,sections,msgbase WHERE sections.id=groups.section AND topics.id=msgbase.id AND (topics.moderate OR NOT sections.moderate) AND topics.userid=users.id AND topics.groupid=groups.id AND section=" + sectionid + (group!=null?" AND groupid="+group.getId():"") + " AND NOT deleted ORDER BY commitdate DESC LIMIT 20");
//  }

  NewsViewer nw = new NewsViewer(tmpl.getConfig(), tmpl.getProf());
  nw.setSection(sectionid);
  if (group != null) {
    nw.setGroup(group.getId());
  }

  if (month != 0) {
    nw.setDatelimit("postdate>='" + year + "-" + month + "-01'::timestamp AND (postdate<'" + year + "-" + month + "-01'::timestamp+'1 month'::interval)");
  } else {
    nw.setDatelimit("commitdate>(CURRENT_TIMESTAMP-'3 month'::interval)");
    nw.setLimit("LIMIT 20");
  }

  out.print(ViewerCacher.getViewer(nw, tmpl, false, false));
%>

<%
 	st.close();
  } finally {
    if (db!=null) db.close();
  }
%>

<%= tmpl.DocumentFooter() %>
