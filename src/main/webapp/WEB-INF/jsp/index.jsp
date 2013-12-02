<%@ page contentType="text/html; charset=utf-8"%>
<%@ page import="ru.org.linux.site.Template"   buffer="60kb"%>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib prefix="lor" uri="http://www.linux.org.ru" %>
<%@ taglib tagdir="/WEB-INF/tags" prefix="lorDir" %>
<%@ taglib prefix="fn" uri="http://java.sun.com/jsp/jstl/functions" %>
<%--
  ~ Copyright 1998-2013 Linux.org.ru
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
<%--@elvariable id="news" type="java.util.List<ru.org.linux.topic.PersonalizedPreparedTopic>"--%>
<%--@elvariable id="uncommited" type="java.lang.Integer"--%>
<%--@elvariable id="uncommitedNews" type="java.lang.Integer"--%>
<%--@elvariable id="hasDrafts" type="java.lang.Boolean"--%>
<% Template tmpl = Template.getTemplate(request); %>
<jsp:include page="/WEB-INF/jsp/head.jsp"/>

<title>LINUX.ORG.RU - Русская информация об ОС Linux</title>
<META NAME="Keywords" CONTENT="linux линукс операционная система документация gnu бесплатное свободное програмное обеспечение софт unix юникс software free documentation operating system новости news">
<META NAME="Description" CONTENT="Все о Linux на русском языке">
<LINK REL="alternate" TITLE="L.O.R RSS" HREF="section-rss.jsp?section=1" TYPE="application/rss+xml">
<jsp:include page="/WEB-INF/jsp/header-main.jsp"/>

<div id="mainpage">
<div id="news">

<c:if test="${showAdsense}">
<div align="center">
  <script type="text/javascript"><!--
  google_ad_client = "ca-pub-6069094673001350";

  if ($(window).width()<728) {
    /* lor-main-mobile */
    google_ad_slot = "2628258430";
    google_ad_width = 320;
    google_ad_height = 50;
  } else if ($(window).width()>1000) {
    /* lor-main-wide */
    google_ad_slot = "4104991635";
    google_ad_width = 728;
    google_ad_height = 90;
  } else {
    /* lor-main */
    google_ad_slot = "1151525234";
    google_ad_width = 468;
    google_ad_height = 60;
  }
  //-->
  </script>
  <script type="text/javascript"
  src="http://pagead2.googlesyndication.com/pagead/show_ads.js">
  </script>
</div>
</c:if>

<c:if test="${template.moderatorSession or template.correctorSession}">
<div class="nav"   style="border-bottom: none">
  <c:if test="${uncommited > 0}">
    [<a href="view-all.jsp">Неподтвержденных</a>: ${uncommited},

    <c:if test="${uncommitedNews > 0}">
      в том числе <a href="view-all.jsp?section=1">новостей</a>:&nbsp;${uncommitedNews}]
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
      <lorDir:news preparedMessage="${msg.preparedTopic}" messageMenu="${msg.topicMenu}" multiPortal="<%= multiPortal %>" moderateMode="false"/>
    </c:forEach>
<div class="nav">
  [<a href="/news/?offset=20">← предыдущие</a>]
  [<a href="add-section.jsp?section=1">добавить новость</a>]
  [<a href="view-all.jsp?section=1">неподтвержденные новости</a>]
  [<a href="section-rss.jsp?section=1">RSS</a>]
</div>
</div>
<aside id=boxlets>

  <c:if test="${template.sessionAuthorized}">
    <div class=boxlet>
      <h2>Добро пожаловать!</h2>

      <div class="boxlet_content">
        Ваш статус: ${template.currentUser.status}
        <ul>
          <li><a href="/people/${template.nick}/">Мои темы</a></li>
          <c:if test="${favPresent}">
              <li><a href="/people/${template.nick}/favs">Избранные темы</a></li>
          </c:if>
          <li><a href="show-comments.jsp?nick=${template.nick}">Мои комментарии</a></li>
          <c:if test="${hasDrafts}">
              <li>
                  <a href="/people/${template.nick}/drafts">Черновики</a>
              </li>
          </c:if>
        </ul>
        <ul>
          <li><a href="/people/${template.nick}/settings">Настройки</a></li>
          <li><a href="/people/${template.nick}/edit">Регистрация</a></li>
        </ul>
      </div>
    </div>
  </c:if>

  <lor:boxlets var="boxes">
      <c:forEach var="boxlet" items="${boxes}">
        <div class="boxlet">
            <c:import url="/${boxlet}.boxlet"/>
        </div>
      </c:forEach>
  </lor:boxlets>
</aside>
</div>

<jsp:include page="/WEB-INF/jsp/footer-main.jsp"/>
