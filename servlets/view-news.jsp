<%@ page contentType="text/html; charset=koi8-r"%>
<%@ page import="java.net.URLEncoder,java.sql.Connection,java.sql.ResultSet,java.sql.Statement,java.util.Calendar,ru.org.linux.site.BadSectionException" errorPage="error.jsp" buffer="200kb"%>
<%@ page import="ru.org.linux.site.NewsViewer"%>
<%@ page import="ru.org.linux.site.Template"%>
<%@ page import="ru.org.linux.util.DateUtil"%>
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
   int section = tmpl.getParameters().getInt("section");

   db = tmpl.getConnection("view-news");

   Statement st=db.createStatement();

   ResultSet rs=st.executeQuery("SELECT name, browsable, imagepost FROM sections WHERE id=" + section);

   if (!rs.next()) throw new BadSectionException();

   int month=0;
   int year=0;

   String ptitle;
   if (request.getParameter("month")==null) {
   	ptitle=rs.getString("name");
   } else {
   	month=tmpl.getParameters().getInt("month");
	year=tmpl.getParameters().getInt("year");
   	ptitle="Архив: " + rs.getString("name") + ", " + year + ", " + DateUtil.getMonth(month);
   }

  if (!rs.getBoolean("browsable")|| section==2) {
    throw new BadSectionException();
  }

%>
	<title><%= ptitle %></title>
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
      if (rs.getBoolean("imagepost")) {
              if (tmpl.getProfileName()!=null)
                      out.print("[<a href=\"http://images.linux.org.ru/addsshot.php?profile="+URLEncoder.encode(tmpl.getProfileName())+"\">Добавить изображение</a>]");
              else
                      out.print("[<a href=\"http://images.linux.org.ru/addsshot.php\">Добавить изображение</a>]");

      }
%>
    </td>
   </tr>
</table>
</div>
</div>
</div>
<%
rs.close();
%>

<H1><%= ptitle %></H1>

<%
  if (month != 0) {
    rs = st.executeQuery("SELECT topics.title as subj, topics.lastmod, topics.stat1, postdate, nick, image, groups.title as gtitle, topics.id as msgid, sections.comment, groups.id as guid, topics.url, topics.linktext, imagepost, linkup, postdate<(CURRENT_TIMESTAMP-expire) as expired, message FROM topics,groups,users,sections,msgbase WHERE sections.id=groups.section AND topics.id=msgbase.id AND (topics.moderate OR NOT sections.moderate) AND topics.userid=users.id AND topics.groupid=groups.id AND section=" + section + " AND postdate>'"+year+"-"+month+"-01'::timestamp AND (postdate<'"+year+"-"+month+"-01'::timestamp+'1 month'::interval) AND NOT deleted ORDER BY commitdate");
  } else {
    rs = st.executeQuery("SELECT topics.title as subj, topics.lastmod, topics.stat1, postdate, nick, image, groups.title as gtitle, topics.id as msgid, sections.comment, groups.id as guid, topics.url, topics.linktext, imagepost, linkup, postdate<(CURRENT_TIMESTAMP-expire) as expired, message FROM topics,groups,users,sections,msgbase WHERE sections.id=groups.section AND topics.id=msgbase.id AND (topics.moderate OR NOT sections.moderate) AND topics.userid=users.id AND topics.groupid=groups.id AND section=" + section + " AND NOT deleted ORDER BY commitdate DESC LIMIT 20");
  }

  NewsViewer nw = new NewsViewer(tmpl.getConfig(), tmpl.getProf(), rs, false, false);

  out.print(nw.showAll());

  rs.close();
%>

<h1>Предыдущие месяцы</h1>
<%

	rs=st.executeQuery("select year, month, c from monthly_stats where section=" + section + " order by year, month");
	while (rs.next()) {
		int tMonth=rs.getInt("month");
		int tYear=rs.getInt("year");
		if (month!=tMonth || year!=tYear)
			out.print("<a href=\"view-news.jsp?year="+tYear+"&amp;month="+tMonth+"&amp;section="+section+"\">"+rs.getInt("year") + ' ' + DateUtil.getMonth(tMonth) + "</a> (" + rs.getInt("c") + ")<br>");
		else
			out.print(rs.getInt("year") + " " + DateUtil.getMonth(tMonth) + " (" + rs.getInt("c") + ")<br>");
	}
	rs.close();
%>
<%
 	st.close();
  } finally {
    if (db!=null) db.close();
  }
%>

<%= tmpl.DocumentFooter() %>
