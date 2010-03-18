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
<%@ page import="java.sql.Connection"   buffer="200kb"%>
<%@ page import="java.util.List" %>
<%@ page import="ru.org.linux.site.*" %>
<%@ page import="ru.org.linux.util.ServletParameterParser" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib tagdir="/WEB-INF/tags" prefix="lor" %>
<%@ taglib prefix="fn" uri="http://java.sun.com/jsp/jstl/functions" %>
<% Template tmpl = Template.getTemplate(request); %>

<%
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
<link>http://www.linux.org.ru/view-message.jsp?msgid=${msgid}</link>
<language>ru</language>
<title>Linux.org.ru: ${message.title}</title>
<%
  Connection db = null;
  try {
    List<Comment> list = ((CommentList) request.getAttribute("comments")).getList();
    int fromIndex = list.size() - num;
    if (fromIndex<0) {
      fromIndex = 0;
    }
%>
  <c:set var="list" value="<%= list %>"/>
  <description><![CDATA[<%
    db = LorDataSource.getConnection();

    out.print(MessageTable.getTopicRss(
            db,
            tmpl.getConfig().getProperty("HTMLPathPrefix"),
            tmpl.getMainUrl(),
            (Message) request.getAttribute("message")));
%>%>]]>
  </description>
  <c:forEach items="${list}" var="comment" begin="<%= fromIndex %>">
    <item>
      <title>
        <c:if test="${fn:length(comment.title)>0}">
          <c:out escapeXml="true" value="${comment.title}"/>
        </c:if>
        <c:if test="${fn:length(comment.title)==0}">
          <c:out escapeXml="true" value="${message.title}"/>
        </c:if>
      </title>
      <author><lor:user db="<%= db %>" id="${comment.userid}"/></author>
      <link>http://www.linux.org.ru/jump-message.jsp?msgid=${message.id}&amp;cid=${comment.id}</link>
      <guid>http://www.linux.org.ru/jump-message.jsp?msgid=${message.id}&amp;cid=${comment.id}</guid>
      <pubDate><lor:rfc822date date="${comment.postdate}"/></pubDate>
      <description ><![CDATA[<%
          Comment comment = (Comment) pageContext.getAttribute("comment");
          out.print(comment.getProcessedMessage(db));
              %>]]>
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
