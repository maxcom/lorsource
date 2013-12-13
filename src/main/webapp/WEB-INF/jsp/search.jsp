<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="fn" uri="http://java.sun.com/jsp/jstl/functions" %>
<%@ page contentType="text/html; charset=utf-8"%>
<%@ taglib tagdir="/WEB-INF/tags" prefix="lor" %>
<%@ taglib prefix="l" uri="http://www.linux.org.ru" %>
<%@ taglib prefix="form" uri="http://www.springframework.org/tags/form" %>
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
<%--@elvariable id="result" type="java.util.List<ru.org.linux.search.SearchItem>"--%>
<%--@elvariable id="query" type="ru.org.linux.search.SearchRequest"--%>
<%--@elvariable id="template" type="ru.org.linux.site.Template"--%>
<%--@elvariable id="time" type="java.lang.Long"--%>
<%--@elvariable id="searchTime" type="java.lang.Long"--%>
<%--@elvariable id="numFound" type="java.lang.Long"--%>
<%--@elvariable id="sorts" type="java.util.Map<SearchViewer.SearchOrder, String>"--%>
<%--@elvariable id="intervals" type="java.util.Map<SearchViewer.SearchInterval, String>"--%>
<%--@elvariable id="ranges" type="java.util.Map<SearchViewer.SearchRange, String>"--%>
<%--@elvariable id="sectionFacet" type="java.util.Map<Integer, String>"--%>
<%--@elvariable id="groupFacet" type="java.util.Map<Integer, String>"--%>
<%--@elvariable id="prevLink" type="java.lang.String"--%>
<%--@elvariable id="nextLink" type="java.lang.String"--%>

<jsp:include page="/WEB-INF/jsp/head.jsp"/>
<title>Поиск по сайту
  <c:if test="${not query.initial}">
    - <c:out value="${query.q}" escapeXml="true"/>
  </c:if>
</title>
<jsp:include page="/WEB-INF/jsp/header.jsp"/>

<H1>Поиск по сайту</h1>
<form:form method="GET" commandName="query" ACTION="search.jsp">

<form:input path="q" TYPE="search" SIZE="50" maxlength="250" autofocus="autofocus"/>
  <button TYPE="submit">Поиск</button><BR>

  <form:hidden path="oldQ"/>
  
  <p>
  <form:select path="range" items="${ranges}"/>

  <label>За:
    <form:select path="interval" items="${intervals}"/>
  </label>

    <label>Пользователь: <form:input path="user" TYPE="text" SIZE="20"/></label>
    <label>В темах пользователя <form:checkbox path="usertopic"/></label>

  <form:errors element="div" cssClass="error" path="*"/>

<c:if test="${not query.initial && numFound!=null}">
  <div class="infoblock">
    <c:if test="${numFound > 1}">
      <div style="float: right">
        <label>сортировать
          <form:select path="sort" onchange="submit()" items="${sorts}"/>
        </label>
      </div>
    </c:if>

    <c:choose>
      <c:when test="${sectionFacet !=null}">
        <div>
          Раздел:
          <c:forEach items="${sectionFacet}" var="facet">
            <label style="display: inline">
                <form:radiobutton path="section" onchange="submit()" value="${facet.key}"/>
                ${facet.value}
            </label>
          </c:forEach>
        </div>
      </c:when>

      <c:otherwise>
        <form:hidden path="section"/>
      </c:otherwise>
    </c:choose>

    <c:if test="${groupFacet!=null}">
      <div>
        Группа:
        <form:select path="group" items="${groupFacet}" onchange="submit()"/>
      </div>
    </c:if>

    <div>
      Всего найдено ${numFound} результатов<!--
      --><c:if test="${numFound > fn:length(result)}"><!--
      -->, показаны ${fn:length(result)}
     </c:if>
    </div>
  </div>

  <div class="messages">
  <div class="comment">
    <c:forEach items="${result}" var="item">
      <div class="msg">
        <div class="msg_header">
          <h2>
            <c:if test="${item.comment}"><i class="icon-comment"></i></c:if>
            <a href="${item.url}"><l:title>${item.title}</l:title></a>
          </h2>
        </div>
        <div class="msg_body">

          <p>${item.message}</p>

          <div class=sign>
            <lor:sign postdate="${item.postdate}" shortMode="false"
                      user="${item.user}"/>
          </div>

          <c:if test="${param.debug}"><div>Rank: ${item.score}</div></c:if>
        </div>
      </div>
    </c:forEach>
  </div>
  </div>

<div class="nav">
  <div style="display: table; width: 100%">
      <c:if test="${prevLink!=null}">
        <div style="display: table-cell; text-align: left">
          <a href="${prevLink}">← предыдущие</a>
        </div>
      </c:if>
      <c:if test="${nextLink!=null}">
        <div style="display: table-right; text-align: right">
          <a href="${nextLink}">следующие →</a>
        </div>
      </c:if>
  </div>
</div>
  
  <p>
    <i>
      Время поиска ${searchTime} ms, время БД ${time} ms
    </i>
  </p>
</c:if>
</form:form>

<jsp:include page="/WEB-INF/jsp/footer.jsp"/>
