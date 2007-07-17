<%@ page contentType="text/html; charset=koi8-r"%>
<%@ page import="java.sql.Connection,java.sql.ResultSet,java.sql.Statement,java.util.Calendar,ru.org.linux.site.BadSectionException,ru.org.linux.site.NewsViewer" errorPage="/error.jsp" buffer="200kb"%>
<%@ page import="ru.org.linux.site.Section"%>
<%@ page import="ru.org.linux.site.Template"%>
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

  db = tmpl.getConnection("view-news");

  Section section = new Section(db, sectionid);

  Statement st = db.createStatement();

  int month = 0;
  int year = 0;

  String ptitle;
  if (request.getParameter("month") == null) {
    ptitle = section.getName();
  } else {
    month = tmpl.getParameters().getInt("month");
    year = tmpl.getParameters().getInt("year");
    ptitle = "Архив: " + section.getName() + ", " + year + ", " + DateUtil.getMonth(month);
  }

  if (!section.isBrowsable() || sectionid == 2) {
    throw new BadSectionException(sectionid);
  }

%>
	<title><%= ptitle %></title>
        <LINK REL="alternate" HREF="section-rss.jsp?section=<%= sectionid %>" TYPE="application/rss+xml">

<%= tmpl.DocumentHeader() %>

<div class=messages>
<div class=nav>
<div class="color1">
  <table width="100%" cellspacing=1 cellpadding=1 border=0><tr class=body>
    <td align=left valign=middle>
      <strong><%= ptitle %></strong>
    </td>
    <td align=right valign=middle>
<%
      if (section.isImagepost()) {
        out.print("[<a style=\"text-decoration: none\" href=\"add.jsp?group=4962\">Добавить изображение</a>]");
      }
      if (section.isVotePoll()) out.print("[<a style=\"text-decoration: none\" href=\"add-poll.jsp?group=19387\">Добавить голосование</a>]");
  out.print("[<a style=\"text-decoration: none\" href=\"view-news-archive.jsp?section="+sectionid+"\">Архив</a>]");
  out.print("[<a style=\"text-decoration: none\" href=\"section-rss.jsp?section="+sectionid+"\">RSS</a>]");

%>
    </td>
   </tr>
</table>
</div>
</div>
</div>

<H1><%= ptitle %></H1>

<%
  ResultSet rs;

  if (month != 0) {
    rs = st.executeQuery("SELECT topics.title as subj, topics.lastmod, topics.stat1, postdate, nick, image, groups.title as gtitle, topics.id as msgid, sections.comment, groups.id as guid, topics.url, topics.linktext, imagepost, vote, linkup, postdate<(CURRENT_TIMESTAMP-expire) as expired, message FROM topics,groups,users,sections,msgbase WHERE sections.id=groups.section AND topics.id=msgbase.id AND (topics.moderate OR NOT sections.moderate) AND topics.userid=users.id AND topics.groupid=groups.id AND section=" + sectionid + " AND postdate>='" + year + "-" + month + "-01'::timestamp AND (postdate<'" + year + "-" + month + "-01'::timestamp+'1 month'::interval) AND NOT deleted ORDER BY commitdate");
  } else {
    rs = st.executeQuery("SELECT topics.title as subj, topics.lastmod, topics.stat1, postdate, nick, image, groups.title as gtitle, topics.id as msgid, sections.comment, groups.id as guid, topics.url, topics.linktext, imagepost, vote, linkup, postdate<(CURRENT_TIMESTAMP-expire) as expired, message FROM topics,groups,users,sections,msgbase WHERE sections.id=groups.section AND topics.id=msgbase.id AND (topics.moderate OR NOT sections.moderate) AND topics.userid=users.id AND topics.groupid=groups.id AND section=" + sectionid + " AND NOT deleted ORDER BY commitdate DESC LIMIT 20");
  }

  NewsViewer nw = new NewsViewer(tmpl.getConfig(), tmpl.getProf(), rs, false, false);

  out.print(nw.showAll(db, tmpl));

  rs.close();
%>

<%
 	st.close();
  } finally {
    if (db!=null) db.close();
  }
%>

<%= tmpl.DocumentFooter() %>
