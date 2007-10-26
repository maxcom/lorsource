<%@ page contentType="text/html; charset=koi8-r"%>
<%@ page import="java.sql.Connection,java.sql.ResultSet,java.sql.Statement,ru.org.linux.site.BadSectionException,ru.org.linux.site.Section,ru.org.linux.site.Template" errorPage="/error.jsp" buffer="200kb"%>
<%@ page import="ru.org.linux.util.DateUtil" %>
<% Template tmpl = new Template(request, config, response); %>
<%= tmpl.head() %>
<% Connection db=null;
  try { %>
<%
  int sectionid = tmpl.getParameters().getInt("section");

  db = tmpl.getConnection();

  Section section = new Section(db, sectionid);

  Statement st = db.createStatement();

  String ptitle = section.getName() + " - Архив";
%>
	<title><%= ptitle %></title>
<%= tmpl.DocumentHeader() %>

<H1><%= ptitle %></H1>
<%

if (!section.isBrowsable()) { throw new BadSectionException(sectionid); }

%>
<%

  ResultSet rs = st.executeQuery("select year, month, c from monthly_stats where section=" + sectionid + " order by year, month");
  while (rs.next()) {
    int tMonth = rs.getInt("month");
    int tYear = rs.getInt("year");
    out.print("<a href=\"view-news.jsp?year=" + tYear + "&amp;month=" + tMonth + "&amp;section=" + sectionid + "\">" + rs.getInt("year") + ' ' + DateUtil.getMonth(tMonth) + "</a> (" + rs.getInt("c") + ")<br>");
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
