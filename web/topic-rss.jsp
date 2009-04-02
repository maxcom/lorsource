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

<?xml version="1.0" encoding="utf-8"?>
<%@ page pageEncoding="koi8-r" contentType="application/rss+xml; charset=utf-8"%>
<%@ page import="java.sql.Connection,ru.org.linux.site.LorDataSource,ru.org.linux.site.MessageTable"   buffer="200kb"%>
<%@ page import="ru.org.linux.site.Template" %>
<%@ page import="ru.org.linux.util.ServletParameterParser" %>
<% Template tmpl = Template.getTemplate(request); %>

<%
  int topic = 1;
  if (request.getParameter("topic") != null) {
    topic = new ServletParameterParser(request).getInt("topic");
  }

  int num = MessageTable.RSS_DEFAULT;
  if (request.getParameter("num") != null) {
    num = new ServletParameterParser(request).getInt("num");
	if (num < MessageTable.RSS_MIN || num > MessageTable.RSS_MAX) {
	  num = MessageTable.RSS_DEFAULT;
	}
  }

%>
<rss version="2.0">
<channel>
<link>http://www.linux.org.ru/view-message.jsp?msgid=<%= topic %></link>
<language>ru</language>
<%

  Connection db = null;
  try {
    db = LorDataSource.getConnection();
    out.print(MessageTable.getTopicRss(db, topic, num, tmpl.getConfig().getProperty("HTMLPathPrefix"), tmpl.getMainUrl()));
%>
<%
  } finally {
    if (db!=null) {
      db.close();
    }
  }
%>
</channel>
</rss>
