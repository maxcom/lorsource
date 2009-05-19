<%@ page pageEncoding="koi8-r" contentType="text/html; charset=utf-8"%>
<%@ page import="java.sql.Connection,ru.org.linux.site.LorDataSource,ru.org.linux.site.MissingParameterException,ru.org.linux.site.Poll"   buffer="200kb"%>
<%@ page import="ru.org.linux.site.Template" %>
<%@ page import="ru.org.linux.util.ServletParameterParser" %>
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

<% Template tmpl = Template.getTemplate(request); %>
<jsp:include page="/WEB-INF/jsp/head.jsp"/>

<%
  int voteid = new ServletParameterParser(request).getInt("vote");

  Connection db = null;
  try {

    db = LorDataSource.getConnection();

    Poll poll = new Poll(db, voteid);

    response.setHeader("Location", tmpl.getMainUrl() + "jump-message.jsp?msgid=" + poll.getTopicId());
    response.setStatus(HttpServletResponse.SC_MOVED_TEMPORARILY);

  } finally {
    if (db != null) {
      db.close();
    }
  }
%>
