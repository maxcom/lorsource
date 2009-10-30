<%@ page contentType="text/html; charset=utf-8"%>
<%@ page import="java.sql.Connection,java.sql.ResultSet,java.sql.Statement,java.util.Date,ru.org.linux.site.SectionNotFoundException,ru.org.linux.site.LorDataSource"   buffer="200kb"%>
<%--
  ~ Copyright 1998-2009 Linux.org.ru
  ~    Licensed under the Apache License, Version 2.0 (the "License");
  ~    you may not use this file except in compliance with the License.
  ~    You may obtain a copy of the License at
  ~
  ~        http://www.apache.org/licenses/LICENSE-2.0
  ~
  ~    Unless required by applicable law or agreed to in writing, software
  ~    distributed under the License is distributed on an "AS IS" BASIS,
  ~    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
  ~    See the License for the specific language governing permissions and
  ~    limitations under the License.
  --%>

<%
  response.setDateHeader("Expires", new Date(new Date().getTime()-20*3600*1000).getTime());
  response.setDateHeader("Last-Modified", new Date(new Date().getTime()-120*1000).getTime());
%>
<META HTTP-EQUIV="Refresh" CONTENT="600; URL=http://www.linux.org.ru/sidebar.jsp">
<%
  Connection db = null;
  try {

    db = LorDataSource.getConnection();

    Statement st = db.createStatement();

    int section = 1;
    ResultSet rs = st.executeQuery("SELECT name, imagepost FROM sections WHERE id=" + section);

    if (!rs.next()) {
      throw new SectionNotFoundException(section);
    }

%>
<strong><a href="http://www.linux.org.ru/" target="_content">LINUX.ORG.RU</a></strong><br>
Последние новости
<p style="font-size: small">
<%
 	rs.close();
	rs=st.executeQuery("SELECT topics.title as subj, topics.lastmod, topics.stat1, postdate, nick, image, groups.title as gtitle, topics.id as msgid, groups.id as guid, topics.url, topics.linktext, imagepost, postdate<(CURRENT_TIMESTAMP-expire) as expired FROM topics,groups, users, sections WHERE sections.id="+section+" AND (topics.moderate OR NOT sections.moderate) AND topics.userid=users.id AND topics.groupid=groups.id AND section=" + section + " AND NOT deleted AND commitdate is not null ORDER BY commitdate DESC LIMIT 10");

	while (rs.next()) {%>
		* <a target="_content" href="view-message.jsp?msgid=<%= rs.getInt("msgid") %>"> <%= rs.getString("subj")%></a> (<%= rs.getInt("stat1") %> комментариев)<br>
<%	}


	rs.close();
%>
<%
	st.close();
  } finally {
    if (db!=null) {
      db.close();
    }
  }
%>
