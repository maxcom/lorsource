<?xml version="1.0" encoding="utf-8"?>
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
<%@ page contentType="application/rss+xml; charset=utf-8"%>
<%@ page import="java.sql.Connection,java.text.DateFormat,java.util.Date"   buffer="200kb"%>
<%@ page import="ru.org.linux.site.*" %>
<%@ page import="ru.org.linux.util.ServletParameterParser" %>
<%@ page import="java.util.List" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib tagdir="/WEB-INF/tags" prefix="lor" %>
<% Template tmpl = Template.getTemplate(request); %>

<rss version="2.0">
<channel>
<link>http://www.linux.org.ru/</link>
<language>ru</language>

<%
  int sectionId = 1;
  if (request.getParameter("section") != null) {
    sectionId = new ServletParameterParser(request).getInt("section");
  }

  int groupId = 0;
  if (request.getParameter("group") != null) {
    groupId = new ServletParameterParser(request).getInt("group");
  }

  String userAgent = request.getHeader("User-Agent");
  boolean feedBurner = userAgent!=null && userAgent.contains("FeedBurner");

  if (sectionId==1 && groupId==0 && !feedBurner && request.getParameter("noredirect")==null) {
    response.setHeader("Location", "http://feeds.feedburner.com/org/LOR");
    response.setStatus(HttpServletResponse.SC_MOVED_TEMPORARILY);
  }

  NewsViewer nv = new NewsViewer();
  nv.setSection(sectionId);
  nv.setDatelimit(" postdate>(CURRENT_TIMESTAMP-'3 month'::interval) ");
  if (groupId !=0) {
    nv.setGroup(groupId);
  }

  nv.setLimit("LIMIT 20");

  Connection db = null;
  try {
    db = LorDataSource.getConnection();

    Section section = new Section(db, sectionId);
    Group group = null;
    if (groupId!=0) {
      group = new Group(db, groupId);

      if (group.getSectionId()!=sectionId) {
        throw new BadGroupException("группа #"+groupId+" не принадлежит разделу #"+sectionId);
      }
    }

    out.append("<title>Linux.org.ru: ").append(section.getName());
    if (group !=null) {
      out.append(" - ").append(group.getTitle());
    }
    out.append("</title>");

    DateFormat rfc822 = DateFormats.createRFC822();

    out.append("<pubDate>").append(rfc822.format(new Date())).append("</pubDate>");
    out.append("<description>Linux.org.ru: ").append(section.getName());
    if (group!=null) {
      out.append(" - ").append(group.getTitle());
    }
    out.append("</description>\n");
    
%>
  <%
    List<Message> messages=feedBurner?nv.getMessages(db):nv.getMessagesCached(db); 
  %>
  <c:forEach var="msg" items="<%= messages %>">
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
