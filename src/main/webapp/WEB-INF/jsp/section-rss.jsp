<%--
  ~ Copyright 1998-2022 Linux.org.ru
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
  --%><?xml version="1.0" encoding="utf-8"?>
<%--@elvariable id="messages" type="java.util.List<ru.org.linux.topic.PreparedTopic>"--%>
<%@ page contentType="application/rss+xml; charset=utf-8"%>
<%@ page import="java.util.Date"%>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib tagdir="/WEB-INF/tags" prefix="lor" %>
<%@ taglib prefix="l" uri="http://www.linux.org.ru" %>
<%--@elvariable id="template" type="ru.org.linux.site.Template"--%>
<%--@elvariable id="configuration" type="ru.org.linux.spring.SiteConfig"--%>
<rss version="2.0">
  <channel>
    <link>https://www.linux.org.ru/</link>
    <language>ru</language>
    <title>Linux.org.ru: <c:out escapeXml="true" value="${ptitle}"/></title>

    <description>Linux.org.ru: <c:out escapeXml="true" value="${ptitle}"/></description>

    <pubDate>
      <lor:rfc822date date="<%= new Date() %>"/>
    </pubDate>
  <c:forEach var="msg" items="${messages}">
    <item>
      <author>${msg.author.nick}</author>
      <link>${configuration.secureUrlWithoutSlash}${msg.message.link}</link>
      <guid>${configuration.secureUrlWithoutSlash}${msg.message.link}</guid>
      <title>${l:escapeXml(msg.message.title)}</title>
      <c:if test="${msg.message.commitDate!=null}">
        <pubDate><lor:rfc822date date="${msg.message.commitDate}"/></pubDate>
      </c:if>
      <c:if test="${msg.message.commitDate==null}">
        <pubDate><lor:rfc822date date="${msg.message.postdate}"/></pubDate>
      </c:if>
      <lor:message-rss preparedTopic="${msg}"/>
    </item>
  </c:forEach>
</channel>
</rss>
