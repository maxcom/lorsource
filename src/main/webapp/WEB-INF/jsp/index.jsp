<%@ page contentType="text/html; charset=utf-8"%>
<%@ page import="java.util.Date"   buffer="60kb"%>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib prefix="lor" uri="http://www.linux.org.ru" %>
<%@ taglib tagdir="/WEB-INF/tags" prefix="lorDir" %>
<%@ taglib prefix="fn" uri="http://java.sun.com/jsp/jstl/functions" %>
<%@ taglib prefix="sec" uri="http://www.springframework.org/security/tags" %>
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
<%--@elvariable id="news" type="java.util.List<ru.org.linux.topic.PersonalizedPreparedTopic>"--%>
<%--@elvariable id="uncommited" type="java.lang.Integer"--%>
<%--@elvariable id="uncommitedNews" type="java.lang.Integer"--%>

<jsp:include page="/WEB-INF/jsp/head.jsp"/>

<title>LINUX.ORG.RU - Русская информация об ОС Linux</title>
<META NAME="Keywords" CONTENT="linux линукс операционная система документация gnu бесплатное свободное програмное обеспечение софт unix юникс software free documentation operating system новости news">
<META NAME="Description" CONTENT="Все о Linux на русском языке">
<LINK REL="alternate" TITLE="L.O.R RSS" HREF="section-rss.jsp?section=1" TYPE="application/rss+xml">

<%
  response.setDateHeader("Expires", new Date(new Date().getTime() - 20 * 3600 * 1000).getTime());
  response.setDateHeader("Last-Modified", new Date(new Date().getTime() - 2 * 1000).getTime());

%>
<%--
<style type="text/css">
    #samsung_text { margin-left: 135px; }
    @media screen and (max-width: 640px) {
        #samsung_text { margin-left: 0; }
    }
</style>
--%>
<jsp:include page="/WEB-INF/jsp/header-main.jsp"/>

<c:set var="multiPortal" value="false" />
<c:set var="columns3" value="false" />

<c:if test="${auth}">
<c:set var="multiPortal" value="${currentProperties.showGalleryOnMain}" />
<c:set var="columns3" value="${currentProperties.threeColumnsOnMain}" />

<c:set var="newsblogClass" value="newsblog" />
<c:set var="newsblogClassIn" value="newsblog-in" />
<c:set var="boxletsObject" value="main2" />

<c:if test="${columns3}">
<c:set var="newsblogClass" value="newsblog2" />
<c:set var="newsblogClassIn" value="newsblog-in2" />
<c:set var="boxletsObject" value="main3-1" />
</c:if>

</c:if>


<div style="clear: both"></div>

<div class="${newsblogClass}">
<div class="${newsblogClassIn}">


<%--
<c:if test="${currentStyle != 'black'}">
  <div class="infoblock" style="border: 1px solid #777; text-align: justify;">
    <a rel="nofollow" href="http://job.samsung.ru/"><img width="130" height="43" src="/adv/Samsung_Logo.png" alt="" style="float: left; border: 0; padding-right: 5px"></a>
    <div id="samsung_text">
    SAMSUNG Electronics&nbsp;&mdash; мировой лидер в&nbsp;производстве полупроводников,
    телекоммуникационного оборудования и&nbsp;цифровой конвергенции&nbsp;&mdash;
    объявляет о&nbsp;приеме на&nbsp;работу инженеров-программистов,
    разработчиков в&nbsp;исследовательские центры компании в&nbsp;Южной Корее:
    Cloud Computing, Cryptography&nbsp;/ Encryption, Security Software, Multimedia
    (TV, BD, PVR, HTS), Linux Kernel, Android, C/C++ Programming, Widget,
    DRM, Network Software, LTE eNB Software Design, Modem ASIC, FPGA, SoC.
    Дополнительная информация:
    <a href="http://job.samsung.ru/" rel="nofollow" style="color: white">http://job.samsung.ru</a>
    </div>
  </div>
</c:if>
--%>

<sec:authorize access="hasAnyRole('ROLE_MODERATOR', 'ROLE_CORRECTOR')">
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
</sec:authorize>


    <c:forEach var="msg" items="${news}">
      <lorDir:news preparedMessage="${msg.preparedTopic}" messageMenu="${msg.topicMenu}" multiPortal="${multiPortal}" moderateMode="false"/>
    </c:forEach>
<div class="nav">
  [<a href="/news/?offset=20">← предыдущие</a>]
  [<a href="add-section.jsp?section=1">добавить новость</a>]
  [<a href="view-all.jsp?section=1">непроверенные новости</a>]
  [<a href="section-rss.jsp?section=1">RSS</a>]
</div>
</div>
</div>
<aside class=column>

  <sec:authorize access="isAuthenticated()">
    <sec:authentication property="principal" var="principal"/>
    <div class=boxlet>
      <h2>Добро пожаловать!</h2>

      <div class="boxlet_content">
        Ваш статус: ${principal.user.status}
        <ul>
          <li><a href="/people/${principal.username}/">Мои темы</a></li>
          <li><a href="/people/${principal.username}/favs">Избранные темы</a></li>
          <li><a href="show-comments.jsp?nick=${principal.username}">Мои комментарии</a></li>
          <c:set var="events">
             <lorDir:events/>
          </c:set>
          <li>${fn:trim(events)}</li>
        </ul>
        <ul>
          <li><a href="/people/${principal.username}/settings">Настройки</a></li>
          <li><a href="/people/${principal.username}/edit">Регистрация</a></li>
        </ul>
      </div>
    </div>
  </sec:authorize>

    <lor:boxlets object="${boxletsObject}" var="boxes">
      <c:forEach var="boxlet" items="${boxes}">
        <div class="boxlet">
            <c:import url="/${boxlet}.boxlet"/>
        </div>
      </c:forEach>
    </lor:boxlets>

</aside>
<c:if test="${columns3}">
<aside class=column2>
  <lor:boxlets object="main3-2" var="boxes">
      <c:forEach var="boxlet" items="${boxes}">
        <div class="boxlet">
            <c:import url="/${boxlet}.boxlet"/>
        </div>
      </c:forEach>
  </lor:boxlets>
</aside>
</c:if>

<div style="clear: both"></div>

<jsp:include page="/WEB-INF/jsp/footer-main.jsp"/>
