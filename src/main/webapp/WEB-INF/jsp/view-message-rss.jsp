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
<%@ page import="ru.org.linux.message.MessageTable"   buffer="200kb"%>
<%@ page import="ru.org.linux.message.PreparedMessage" %>
<%@ page import="ru.org.linux.site.Template" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib tagdir="/WEB-INF/tags" prefix="lor" %>
<%@ taglib prefix="fn" uri="http://java.sun.com/jsp/jstl/functions" %>
<%--@elvariable id="message" type="ru.org.linux.message.Message"--%>
<%--@elvariable id="comments" type="ru.org.linux.site.CommentList"--%>
<%--@elvariable id="commentsPrepared" type="java.util.List<ru.org.linux.site.PreparedComment>"--%>
<%--@elvariable id="preparedMessage" type="ru.org.linux.message.PreparedMessage"--%>
<% Template tmpl = Template.getTemplate(request); %>

<rss version="2.0">
<channel>
<link>http://www.linux.org.ru/view-message.jsp?msgid=${message.id}</link>
<language>ru</language>
<title>Linux.org.ru: ${message.title}</title>
  <description><![CDATA[<%
    out.print(MessageTable.getTopicRss(
            tmpl.getConfig().getProperty("HTMLPathPrefix"),
            (PreparedMessage) request.getAttribute("preparedMessage")));
%>]]>
  </description>
  <c:forEach items="${commentsPrepared}" var="comment">
    <item>
      <title>
        <c:if test="${fn:length(comment.comment.title)>0}">
          <c:out escapeXml="true" value="${comment.comment.title}"/>
        </c:if>
        <c:if test="${fn:length(comment.comment.title)==0}">
          <c:out escapeXml="true" value="${message.title}"/>
        </c:if>
      </title>
      <author><lor:user user="${comment.author}"/></author>
      <link>http://www.linux.org.ru/jump-message.jsp?msgid=${message.id}&amp;cid=${comment.comment.id}</link>
      <guid>http://www.linux.org.ru/jump-message.jsp?msgid=${message.id}&amp;cid=${comment.comment.id}</guid>
      <pubDate><lor:rfc822date date="${comment.comment.postdate}"/></pubDate>
      <description ><![CDATA[${comment.processedMessage}]]>
      </description>
    </item>
  </c:forEach>
</channel>
</rss>
