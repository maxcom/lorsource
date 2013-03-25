<%--
  ~ Copyright 1998-2012 Linux.org.ru
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
<%@ page contentType="application/rss+xml; charset=utf-8"%>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib tagdir="/WEB-INF/tags" prefix="lor" %>
<%@ taglib prefix="fn" uri="http://java.sun.com/jsp/jstl/functions" %>
<%@ taglib prefix="l" uri="http://www.linux.org.ru" %>
<%--@elvariable id="message" type="ru.org.linux.topic.Topic"--%>
<%--@elvariable id="preparedMessage" type="ru.org.linux.topic.PreparedTopic"--%>
<%--@elvariable id="commentsPrepared" type="java.util.List<ru.org.linux.comment.PreparedRSSComment>"--%>
<%--@elvariable id="template" type="ru.org.linux.site.Template"--%>
<rss version="2.0">
<channel>
<link>${template.mainUrlNoSlash}${message.link}</link>
<language>ru</language>
<title>Linux.org.ru: ${l:escapeHtml(message.title)}</title>
  <lor:message-rss preparedMessage="${preparedMessage}"/>
  <c:forEach items="${commentsPrepared}" var="comment">
    <item>
      <title>
        <c:if test="${fn:length(comment.comment.title)>0}">
            ${l:escapeHtml(comment.comment.title)}
        </c:if>
        <c:if test="${fn:length(comment.comment.title)==0}">
          ${l:escapeHtml(message.title)}
        </c:if>
      </title>
      <author>${comment.author.nick}</author>
      <link>${template.mainUrlNoSlash}${message.link}?cid=${comment.comment.id}</link>
      <guid>${template.mainUrlNoSlash}${message.link}?cid=${comment.comment.id}</guid>
      <pubDate><lor:rfc822date date="${comment.comment.postdate}"/></pubDate>
      <description ><![CDATA[${comment.processedMessage}]]>
      </description>
    </item>
  </c:forEach>
</channel>
</rss>
