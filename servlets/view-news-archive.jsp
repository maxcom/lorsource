<%@ page contentType="text/html; charset=koi8-r"%>
<%@ page import="java.sql.Connection,java.sql.ResultSet,java.sql.Statement,ru.org.linux.site.BadSectionException,ru.org.linux.site.Template,ru.org.linux.util.DateUtil" errorPage="error.jsp" buffer="200kb"%>
<% Template tmpl = new Template(request, config, response); %>
<%= tmpl.head() %>
<% Connection db=null;
  try { %>
<%
   int section = tmpl.getParameters().getInt("section");

   db = tmpl.getConnection("view-news-archive");

   Statement st=db.createStatement();

   ResultSet rs=st.executeQuery("SELECT name, browsable, imagepost FROM sections WHERE id=" + section);

   if (!rs.next()) throw new BadSectionException();

  String ptitle=rs.getString("name") + " - Архив";
%>
	<title><%= ptitle %></title>
<%= tmpl.DocumentHeader() %>

<H1><%= ptitle %></H1>
<%

if (!rs.getBoolean("browsable")) { throw new BadSectionException(); }
if (rs.getBoolean("imagepost")) out.print("[<a href=\"addsshot.php\">Добавить изображение</a>]");

rs.close();
%>

<%

	rs=st.executeQuery("select date_part('year', postdate) as year, date_part('month', postdate) as month, count(topics.id) as c from topics, groups, sections where topics.groupid=groups.id and groups.section=sections.id and section=" + section + " and topics.moderate and not deleted group by year, month order by year, month");
  int year = 0;
  int month = 0;
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
