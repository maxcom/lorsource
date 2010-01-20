<?xml version="1.0" encoding="utf-8"?>
<%--
  ~ Copyright 1998-2010 Linux.org.ru
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
<%@ page contentType="application/rss+xml; charset=utf-8"%>
<%@ page import="java.sql.Connection,java.util.Date,ru.org.linux.site.LorDataSource"   buffer="200kb"%>
<%@ page import="ru.org.linux.site.Message" %>
<%@ page import="ru.org.linux.site.MessageTable" %>
<%@ page import="ru.org.linux.site.Template" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib tagdir="/WEB-INF/tags" prefix="lor" %>
<% Template tmpl = Template.getTemplate(request); %>

<rss version="2.0">
  <channel>
    <link>http://www.linux.org.ru/</link>
    <language>ru</language>
    <title>Linux.org.ru: ${ptitle}</title>

    <description>Linux.org.ru: ${ptitle}</description>

    <pubDate>
      <lor:rfc822date date="<%= new Date() %>"/>
    </pubDate>
    <%
  Connection db = null;
  try {
    db = LorDataSource.getConnection();

  %>
  <c:forEach var="msg" items="${messages}">
    <item>
      <author><lor:user db="<%= db %>" id="${msg.uid}"/></author>
      <link>http://www.linux.org.ru/view-message.jsp?msgid=${msg.id}</link>
      <guid>http://www.linux.org.ru/view-message.jsp?msgid=${msg.id}</guid>
      <title><c:out escapeXml="true" value="${msg.title}"/></title>
      <c:if test="${msg.commitDate!=null}">
        <pubDate><lor:rfc822date date="${msg.commitDate}"/></pubDate>
      </c:if>
      <c:if test="${msg.commitDate==null}">
        <pubDate><lor:rfc822date date="${msg.postdate}"/></pubDate>      
      </c:if>
      <description>
      <%
        out.print(MessageTable.getTopicRss(db, tmpl.getConfig().getProperty("HTMLPathPrefix"), tmpl.getMainUrl(), (Message) pageContext.getAttribute("msg")));
      %>
      </description>
    </item>
  </c:forEach>
<%
  } finally {
    if (db!=null) {
      db.close();
    }
  }
%>
</channel>
</rss>
