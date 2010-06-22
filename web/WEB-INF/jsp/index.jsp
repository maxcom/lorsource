<%@ page contentType="text/html; charset=utf-8"%>
<%@ page import="java.sql.Connection,java.sql.ResultSet,java.sql.Statement,java.util.Date"   buffer="60kb"%>
<%@ page import="ru.org.linux.site.*" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib prefix="lor" uri="http://www.linux.org.ru" %>
<%@ taglib tagdir="/WEB-INF/tags" prefix="lorDir" %>
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

<% Template tmpl = Template.getTemplate(request); %>
<jsp:include page="/WEB-INF/jsp/head.jsp"/>

<title>LINUX.ORG.RU - Русская информация об ОС Linux</title>
<META NAME="Keywords" CONTENT="linux линукс операционная система документация gnu бесплатное свободное програмное обеспечение софт unix юникс software free documentation operating system новости news">
<META NAME="Description" CONTENT="Все о Linux на русском языке">
<LINK REL="alternate" TITLE="L.O.R RSS" HREF="section-rss.jsp?section=1" TYPE="application/rss+xml">

<%
  response.setDateHeader("Expires", new Date(new Date().getTime() - 20 * 3600 * 1000).getTime());
  response.setDateHeader("Last-Modified", new Date(new Date().getTime() - 2 * 1000).getTime());

%>
<jsp:include page="/WEB-INF/jsp/header-main.jsp"/>
<%
  boolean columns3 = tmpl.getProf().getBoolean("main.3columns");

  Connection db = null;
  try {
%>

<div style="clear: both"></div>
<div class="<%= columns3?"newsblog2":"newsblog"%>">
  <div class="<%= columns3?"newsblog-in2":"newsblog-in"%>">

<h1><a href="/news/">Новости</a></h1>

<c:if test="${template.style != 'black'}">
  <div class="infoblock" style="border: 1px solid #777; margin: 0; text-align: justify;">
    <a href="http://job.samsung.ru/"><img width="130" height="43" src="/adv/Samsung_Logo.png" alt="" style="float: left; border: 0"></a>
    <div style="margin-left: 135px">
    <h2>Вакансии</h2>
    Samsung Electronics приглашает разработчиков LINUX на работу в R&amp;D центры в Южной Корее (г. Сувон)<br>
    Область специализации: Linux Kernel, TV Server SW, Open OS, Mobile Platform SW, Service
    Platform, Convergence, SW Platform, Multicore SW, System SW Tool.
    <a href="http://job.samsung.ru/" style="color: white">http://job.samsung.ru</a>
    </div>
  </div>
</c:if>

<c:if test="${template.moderatorSession or template.correctorSession}">
<div class="nav"   style="border-bottom: none">
<%
    if (db==null) {
      db = LorDataSource.getConnection();
    }

    Statement st = db.createStatement();
    ResultSet rs = st.executeQuery("select count(*) from topics,groups,sections where section=sections.id AND sections.moderate and topics.groupid=groups.id and not deleted and not topics.moderate AND postdate>(CURRENT_TIMESTAMP-'1 month'::interval)");

    if (rs.next()) {
      int count = rs.getInt("count");

      out.print("[<a href=\"view-all.jsp\">Неподтвержденных</a>: " + count);
    }

    rs.close();

    rs = st.executeQuery("select count(*) from topics,groups where section=1 AND topics.groupid=groups.id and not deleted and not topics.moderate AND postdate>(CURRENT_TIMESTAMP-'1 month'::interval)");

    if (rs.next()) {
      int count = rs.getInt("count");

      if (count>0) {
        out.print(", в том числе <a href=\"view-all.jsp?section=1\">новостей</a>: " + count + ']');
      } else {
        out.print(", новостей нет]");
      }
    }

    rs.close();

    st.close();

    db.close(); db=null;
%>
  </div>
  </c:if>
    <%
  db = LorDataSource.getConnection();

  NewsViewer nv = NewsViewer.getMainpage();

  boolean multiPortal = false;

  if (tmpl.getProf().getBoolean(DefaultProfile.MAIN_GALLERY)) {
    nv.addSection(3);
    multiPortal = true;
  }
%>
    <c:forEach var="msg" items="<%= nv.getMessagesCached(db ) %>">
      <lorDir:news db="<%= db %>" message="${msg}" multiPortal="<%= multiPortal %>" moderateMode="false"/>
    </c:forEach>
<%
  db.close(); db=null;
%>
<div class="nav">
  [<a href="/news/?offset=20">← предыдущие</a>]
  [<a href="add-section.jsp?section=1">добавить новость</a>]
  [<a href="view-all.jsp?section=1">непроверенные новости</a>]
  [<a href="section-rss.jsp?section=1">RSS</a>]
</div>
</div>
</div>
<div class=column>
  <% if (Template.isSessionAuthorized(session)) { %>
<div class=boxlet>
<h2>Добро пожаловать!</h2>
<div class="boxlet_content">
Ваш статус:
<%
  if (db==null) {
    db = LorDataSource.getConnection();
  }
  
  User user = User.getCurrentUser(db, session);

  out.print(user.getStatus());
%>
  <ul>
    <li><a href="tracker.jsp?filter=mine">Мои темы</a></li>
    <li><a href="show-comments.jsp?nick=<%= user.getNick() %>">Мои комментарии</a></li>
    <li><a href="show-replies.jsp?nick=<%= user.getNick() %>">Уведомления</a></li>
  </ul>
  <ul>
    <li><a href="edit-profile.jsp">Настройки</a></li>
  </ul>
</div>
</div>
  <% db.close(); db=null; } %>
  <lor:boxlets object="<%= columns3 ? \"main3-1\" : \"main2\" %>" var="boxes">
      <c:forEach var="boxlet" items="${boxes}">
        <div class="boxlet">
            <c:import url="/${boxlet}.boxlet"/>
        </div>
      </c:forEach>
  </lor:boxlets>
</div>
<% if (columns3) { %>
<div class=column2>
  <lor:boxlets object="main3-2" var="boxes">
      <c:forEach var="boxlet" items="${boxes}">
        <div class="boxlet">
            <c:import url="/${boxlet}.boxlet"/>
        </div>
      </c:forEach>
  </lor:boxlets>
</div>
<% } %>

<div style="clear: both"></div>

<% } finally {
    if (db!=null) {
      db.close();
    }
  }
%>
<jsp:include page="/WEB-INF/jsp/footer-main.jsp"/>
