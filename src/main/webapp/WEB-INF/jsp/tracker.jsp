<%@ page info="last active topics" %>
<%@ page contentType="text/html; charset=utf-8" %>
<%@ page import="ru.org.linux.site.Template" %>
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
<%--@elvariable id="newUsers" type="java.util.List<ru.org.linux.user.User>"--%>
<%--@elvariable id="msgs" type="java.util.List<ru.org.linux.spring.dao.TrackerItem>"--%>
<%--@elvariable id="template" type="ru.org.linux.site.Template"--%>
<%--@elvariable id="deleteStats" type="java.util.List<ru.org.linux.site.DeleteInfoStat>"--%>
<% Template tmpl = Template.getTemplate(request); %>
<jsp:include page="/WEB-INF/jsp/head.jsp"/>
<%@ taglib tagdir="/WEB-INF/tags" prefix="lor" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="fn" uri="http://java.sun.com/jsp/jstl/functions" %>
<%@ taglib prefix="form" uri="http://www.springframework.org/tags/form" %>
<%@ taglib prefix="ftm" uri="http://java.sun.com/jsp/jstl/fmt" %>

<%
    String title = "Последние сообщения";
    if ((Boolean) request.getAttribute("mine")) {
      title += " (мои темы)";
    }
%>

<title><%= title %>
</title>
<jsp:include page="/WEB-INF/jsp/header.jsp"/>

<form:form commandName="tracker" method="GET">
  <div class=nav>
      <div id="navPath">
        <%= title %>
      </div>

      <div class="nav-buttons">
        <form:select path="filter">
            <form:options items="${filterItems}" itemValue="value" itemLabel="label"/>
        </form:select>
        <input type="submit" value="показать">
      </div>
  </div>
</form:form>

<h1 class="optional"><%= title %>
</h1>

<div class=forum>
  <table class="message-table">
    <thead>
    <tr>
      <th>Заголовок</th>
      <th>Последнее<br>сообщение</th>
      <th>Число ответов</th>
    </tr>
    </thead>
    <tbody>
    <c:forEach var="msg" items="${msgs}">

      <tr>
        <td>
          <c:if test="${filter=='mine' && msg.resolved}">
            <img src="/img/solved.png" alt="решено" title="решено"/>
          </c:if>
          <% if (tmpl.getProf().isShowNewFirst()) { %>
          <a href="${msg.urlReverse}">
                <% } else { %>
            <a href="${msg.url}">
              <% } %>
                ${msg.title}
            </a>

                (<%--
                  --%><c:if test="${msg.author != null}"><lor:user user="${msg.author}" decorate="true"/>
                  в
                  </c:if><%--
                   --%><a href="${msg.groupUrl}" class="secondary">${msg.groupTitle}</a><%--

                   --%><c:if test="${msg.uncommited}">, не подтверждено</c:if><%--
                  --%><c:if test="${msg.wikiArticle}">, статья</c:if><%--
                   --%><c:if test="${msg.wikiComment}">, комментарий</c:if><%--
                  --%>)
        </td>
        <td class="dateinterval">
          <lor:dateinterval date="${msg.postdate}"/>,
          <c:if test="${msg.lastCommentBy != null}">
            <lor:user user="${msg.lastCommentBy}" decorate="true"/>
          </c:if>
        </td>
        <td class='numbers'>
            <c:choose>
                <c:when test="${msg.stat1==0}">
                    -
                </c:when>
                <c:when test="${msg.stat1 > 0 && msg.wiki}">
                    +${msg.stat1}
                </c:when>
                <c:otherwise>
                    ${msg.stat1}
                </c:otherwise>
            </c:choose>
      </tr>
    </c:forEach>
    </tbody>

  </table>
</div>

<div class="nav">
  <div style="display: table; width: 100%">
    <div style="display: table-cell; text-align: left">
      <c:if test="${offset>0}">
        <a href="tracker.jsp?offset=${offset-topics}${query}">← предыдущие</a>
      </c:if>
    </div>
    <div style="display: table-cell; text-align: right">
      <c:if test="${offset+topics<300 and fn:length(msgs)==topics}">
        <a href="tracker.jsp?offset=${offset+topics}${query}">следующие →</a>
      </c:if>
    </div>
  </div>
</div>

<c:if test="${newUsers!=null and fn:length(newUsers)!=0}">
  <h2>Новые пользователи</h2>
  Новые пользователи за последние 3 дня:
  <c:forEach items="${newUsers}" var="user">
    <c:if test="${user.activated}">
      <b>
    </c:if>
    <c:if test="${user.blocked}">
      <s>
    </c:if>
    <a href="/people/${user.nick}/profile">${user.nick}</a>
    <c:if test="${user.blocked}">
      </s>
    </c:if>
    <c:if test="${user.activated}">
      </b>
    </c:if>
  </c:forEach>
  (всего ${fn:length(newUsers)})
</c:if>

<c:if test="${deleteStats!=null and fn:length(deleteStats)!=0}">
  <h2>Статистика удаленных за 24 часа</h2>
  <div class=forum>

  <table width="100%" class="message-table">
    <thead>
    <tr>
      <th>Причина</th>
      <th>Количество</th>
      <th>Сумма score</th>
      <th>Средний score</th>
    </tr>
    </thead>
    <tbody>
      <c:forEach items="${deleteStats}" var="stat">
          <tr>
              <td><c:out escapeXml="true" value="${stat.reason}"/></td>
              <td>${stat.count}</td>
              <td>${stat.sum}</td>
              <td><ftm:formatNumber value="${stat.avg}" maxFractionDigits="2"/></td>
          </tr>
      </c:forEach>
  </table>
  </div>
</c:if>
<jsp:include page="/WEB-INF/jsp/footer.jsp"/>
