<%@ page contentType="text/html; charset=utf-8"%>
<%@ page import="java.util.Date,ru.org.linux.profile.DefaultProfile,ru.org.linux.site.Template"   buffer="60kb"%>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib prefix="lor" uri="http://www.linux.org.ru" %>
<%@ taglib tagdir="/WEB-INF/tags" prefix="lorDir" %>
<%@ taglib prefix="fn" uri="http://java.sun.com/jsp/jstl/functions" %>
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
<%--@elvariable id="template" type="ru.org.linux.site.Template"--%>
<%--@elvariable id="news" type="java.util.List<ru.org.linux.message.PreparedMessage>"--%>
<%--@elvariable id="uncommited" type="java.lang.Integer"--%>
<%--@elvariable id="uncommitedNews" type="java.lang.Integer"--%>

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
  boolean columns3 = tmpl.getProf().isThreeColumnsOnMain();
%>

<div style="clear: both"></div>
<div class="<%= columns3?"newsblog2":"newsblog"%>">
  <div class="<%= columns3?"newsblog-in2":"newsblog-in"%>">

<h1><a href="/news/">Новости</a></h1>

<c:if test="${template.style != 'black'}">
  <div class="infoblock" style="border: 1px solid #777; text-align: justify;">
    <a href="http://job.samsung.ru/"><img width="130" height="43" src="/adv/Samsung_Logo.png" alt="" style="float: left; border: 0"></a>
    <div style="margin-left: 135px">
<%--
    <h2>Вакансии</h2>
--%>
    SAMSUNG Electronics&nbsp;&mdash; мировой лидер в&nbsp;производстве полупроводников,
телекоммуникационного оборудования и&nbsp;цифровой конвергенции&nbsp;&mdash; объявляет о&nbsp;приеме
на&nbsp;работу программистов, инженеров-разработчиков, физиков и&nbsp;химиков: LINUX,
Android, C/C++, Smart Phones, Smart TV, 3D&nbsp;Imaging, 3D&nbsp;Graphics, ASIC, FPGA,
SoC, Graphene, MEMS, Biomedical Engineering. Дополнительная информация:
    <a href="http://job.samsung.ru/" style="color: white">http://job.samsung.ru</a>
    </div>
  </div>
</c:if>

<c:if test="${template.moderatorSession or template.correctorSession}">
<div class="nav"   style="border-bottom: none">
  <c:if test="${uncommited > 0}">
    [<a href="view-all.jsp">Неподтвержденных</a>: ${uncommited},

    <c:if test="${uncommitedNews > 0}">
      в том числе <a href="view-all.jsp?section=1">новостей</a>: ${uncommitedNews}]
    </c:if>
    <c:if test="${uncommitedNews == 0}">
      новостей нет]
    </c:if>
  </c:if>
</div>
</c:if>
<%
  boolean multiPortal = false;

  if (tmpl.getProf().isShowGalleryOnMain()) {
    multiPortal = true;
  }
%>
    <c:forEach var="msg" items="${news}">
      <lorDir:news message="${msg.message}" preparedMessage="${msg}" multiPortal="<%= multiPortal %>" moderateMode="false"/>
    </c:forEach>
<div class="nav">
  [<a href="/news/?offset=20">← предыдущие</a>]
  [<a href="add-section.jsp?section=1">добавить новость</a>]
  [<a href="view-all.jsp?section=1">непроверенные новости</a>]
  [<a href="section-rss.jsp?section=1">RSS</a>]
</div>
</div>
</div>
<div class=column>

  <c:if test="${template.sessionAuthorized}">
    <div class=boxlet>
      <h2>Добро пожаловать!</h2>

      <div class="boxlet_content">
        Ваш статус: ${template.currentUser.status}
        <ul>
          <li><a href="tracker.jsp?filter=mine">Мои темы</a></li>
          <li><a href="/people/${template.nick}/favs">Избранные темы</a></li>
          <li><a href="show-comments.jsp?nick=${template.nick}">Мои комментарии</a></li>
          <c:set var="events">
             <lorDir:events/>
          </c:set>
          <li>${fn:trim(events)}</li>
        </ul>
        <ul>
          <li><a href="edit-profile.jsp">Настройки</a></li>
        </ul>
      </div>
    </div>
  </c:if>

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

<jsp:include page="/WEB-INF/jsp/footer-main.jsp"/>
