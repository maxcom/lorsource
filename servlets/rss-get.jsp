<?xml version="1.0" encoding="koi8-r"?>
<%@ page contentType="application/rss+xml; charset=koi8-r"%>
<%@ page import="java.sql.Connection,java.sql.ResultSet,java.sql.Statement,java.text.DateFormat,java.text.SimpleDateFormat,java.util.Date,ru.org.linux.site.BadSectionException" errorPage="error.jsp" buffer="200kb"%>
<%@ page import="ru.org.linux.site.Template"%>
<%@ page import="ru.org.linux.util.HTMLFormatter"%>
<% Template tmpl = new Template(request, config, response); %>
<%
  response.setDateHeader("Expires", new Date(new Date().getTime()-20*3600*1000).getTime());
  response.setDateHeader("Last-Modified", new Date(new Date().getTime()-120*1000).getTime());
//  response.setHeader("Content-type", "application/rss+xml");

   DateFormat rfc822 = new SimpleDateFormat("EEE, d MMM yyyy HH:mm:ss Z");
%>

<rss version="2.0">
<channel>
<title>Linux.org.ru News</title>
<link>http://www.linux.org.ru/</link>
<description>Latest Linux/Opensource news Linux.org.ru.</description>
<language>ru</language>
<pubDate><%= rfc822.format(new Date()) %> </pubDate>

<%
  Connection db = null;
  try {

   db = tmpl.getConnection("sidebar");

   Statement st=db.createStatement();

   int section = 1;
   ResultSet rs=st.executeQuery("SELECT name, browsable, imagepost FROM sections WHERE id=" + section);

   if (!rs.next()) throw new BadSectionException();

  rs.close();
  rs=st.executeQuery("SELECT topics.title as subj, topics.lastmod, topics.stat1, postdate, nick, image, groups.title as gtitle, topics.id as msgid, sections.comment, groups.id as guid, topics.url, topics.linktext, imagepost, linkup, postdate<(CURRENT_TIMESTAMP-expire) as expired, message FROM topics,groups, users, sections, msgbase WHERE sections.id=groups.section AND topics.id=msgbase.id AND sections.id="+section+" AND (topics.moderate OR NOT sections.moderate) AND topics.userid=users.id AND topics.groupid=groups.id AND NOT deleted AND commitdate>(CURRENT_TIMESTAMP-'1 month'::interval) ORDER BY commitdate DESC LIMIT 10");

  while (rs.next()) {
    int msgid=rs.getInt("msgid");
%>
<item>
  <title><%= rs.getString("subj")%></title>
  <link>http://www.linux.org.ru/jump-message.jsp?msgid=<%= msgid %></link>
  <pubDate><%= rfc822.format(rs.getTimestamp("postdate")) %></pubDate>
  <description>
	<%= HTMLFormatter.htmlSpecialChars(rs.getString("message")) %>
 
  </description>
</item>

<%	}


	rs.close();
%>
<%
	st.close();
  } finally {
    if (db!=null) db.close();
  }
%>
</channel>
</rss>
