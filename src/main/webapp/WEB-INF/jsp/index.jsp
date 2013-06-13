<%@ page contentType="text/html; charset=utf-8"%>
<%@ page import="ru.org.linux.site.Template"   buffer="60kb"%>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib prefix="lor" uri="http://www.linux.org.ru" %>
<%@ taglib tagdir="/WEB-INF/tags" prefix="lorDir" %>
<%@ taglib prefix="fn" uri="http://java.sun.com/jsp/jstl/functions" %>
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
  --%>
<%--@elvariable id="template" type="ru.org.linux.site.Template"--%>
<%--@elvariable id="news" type="java.util.List<ru.org.linux.topic.PersonalizedPreparedTopic>"--%>
<%--@elvariable id="uncommited" type="java.lang.Integer"--%>
<%--@elvariable id="uncommitedNews" type="java.lang.Integer"--%>

<% Template tmpl = Template.getTemplate(request); %>
<jsp:include page="/WEB-INF/jsp/head.jsp"/>

<title>LINUX.ORG.RU - Русская информация об ОС Linux</title>
<META NAME="Keywords" CONTENT="linux линукс операционная система документация gnu бесплатное свободное програмное обеспечение софт unix юникс software free documentation operating system новости news">
<META NAME="Description" CONTENT="Все о Linux на русском языке">
<LINK REL="alternate" TITLE="L.O.R RSS" HREF="section-rss.jsp?section=1" TYPE="application/rss+xml">
<jsp:include page="/WEB-INF/jsp/header-main.jsp"/>
<div style="clear: both"></div>
<div class="newsblog">
  <div class="newsblog-in">

<c:if test="${template.style != 'black'}">
<div class="infoblock" style="border: 1px solid #777; text-align: justify;">
  <a rel="nofollow" href="http://job.samsung.ru/"><img width="130" height="43" src="/adv/Samsung_Logo.png" alt="" style="float: left; border: 0; padding-right: 5px"></a>
  <div id="samsung_text">
    Samsung Electronics Co., Ltd. приглашает инженеров-программистов
    (System- and Platform-Level Security, Cryptography, AES&nbsp;/ RSA Encryption,
    Application&nbsp;/ Embedded&nbsp;/ System Software, Cloud Computing, Android, Linux Kernel, Python, C/C++,
    Java, LTE&nbsp;/ CDMA Design, Modem, DSP, Crest Factor Reduction [CFR], Digital Pre-Distorter [DPD]),
    а&nbsp;также профессоров и&nbsp;доцентов (д.ф.-м.н., д.т.н., к.ф.-м.н., к.т.н.) в&nbsp;области разработки
    программного обеспечения (Open OS: Linux, Android; Software Algorithms, Multimedia, and Graphics)
    на&nbsp;работу в&nbsp;Samsung R&amp;D Center и&nbsp;Samsung Advanced Technology Center (Республика Корея).
    Дополнительная информация:
    <a href="http://job.samsung.ru/" rel="nofollow" style="color: white" target="_blank">http://job.samsung.ru</a>
    (вакансии: <a href="http://job.samsung.ru/vacancies.aspx" rel="nofollow" target="_blank" style="color: white">http://job.samsung.ru/vacancies.aspx</a>)
  </div>
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
</div>
<aside class=column>

  <c:if test="${template.sessionAuthorized}">
    <div class=boxlet>
      <h2>Добро пожаловать!</h2>

      <div class="boxlet_content">
        Ваш статус: ${template.currentUser.status}
        <ul>
          <li><a href="/people/${template.nick}/">Мои темы</a></li>
          <li><a href="/people/${template.nick}/favs">Избранные темы</a></li>
          <li><a href="show-comments.jsp?nick=${template.nick}">Мои комментарии</a></li>
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

<div style="clear: both"></div>

<jsp:include page="/WEB-INF/jsp/footer-main.jsp"/>
