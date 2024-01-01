<%@ page info="last active topics" %>
<%@ page contentType="text/html; charset=utf-8" %>
<%--
  ~ Copyright 1998-2024 Linux.org.ru
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
<%--@elvariable id="frozenUsers" type="java.util.List<scala.Tuple2<ru.org.linux.user.User, java.lang.Boolean>>"--%>
<%--@elvariable id="messages" type="java.util.List<ru.org.linux.group.TopicsListItem>"--%>
<%--@elvariable id="template" type="ru.org.linux.site.Template"--%>
<%--@elvariable id="deleteStats" type="java.util.List<ru.org.linux.site.DeleteInfoStat>"--%>
<%--@elvariable id="filters" type="java.util.List<ru.org.linux.spring.TrackerFilterEnum>"--%>
<jsp:include page="/WEB-INF/jsp/head.jsp"/>
<%@ taglib tagdir="/WEB-INF/tags" prefix="lor" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="fn" uri="http://java.sun.com/jsp/jstl/functions" %>
<%@ taglib prefix="form" uri="http://www.springframework.org/tags/form" %>
<%@ taglib prefix="ftm" uri="http://java.sun.com/jsp/jstl/fmt" %>
<%@ taglib prefix="l" uri="http://www.linux.org.ru" %>

<title>${title}</title>
<jsp:include page="/WEB-INF/jsp/header.jsp"/>

<h1>Последние сообщения</h1>

<nav>
  <c:forEach items="${filters}" var="f">
      <c:url var="fUrl" value="/tracker/">
        <c:if test="${f != defaultFilter}">
          <c:param name="filter">${f.value}</c:param>
        </c:if>
      </c:url>
      <c:if test="${filter != f.value}">
        <a class="btn btn-default" href="${fUrl}">${f.label}</a>
      </c:if>
      <c:if test="${filter==f.value}">
        <a href="${fUrl}" class="btn btn-selected">${f.label}</a>
      </c:if>
  </c:forEach>
</nav>

<c:if test="${template.prof.oldTracker}">
  <lor:tracker-topics messages="${messages}"/>
</c:if>

<c:if test="${not template.prof.oldTracker}">
  <lor:tracker-topics-new messages="${messages}"/>
</c:if>

<div class="nav">
  <div style="display: table; width: 100%">
    <div style="display: table-cell; text-align: left">
      <c:if test="${not empty prevLink}">
        <a href="${prevLink}">← предыдущие</a>
      </c:if>
    </div>
    <div style="display: table-cell; text-align: right">
      <c:if test="${not empty nextLink}">
        <a href="${nextLink}">следующие →</a>
      </c:if>
    </div>
  </div>
</div>

<c:if test="${not empty newUsers || not empty frozenUsers || not empty blockedUsers || not empty unFrozenUsers || not empty unBlockedUsers || not empty recentUserpics}">
  <h2>Пользователи</h2>
  <p>
    Новые пользователи за последние 3 дня:
    <c:forEach items="${newUsers}" var="user">
      <lor:user user="${user}" link="true" bold="${user.activated}"/><c:out value=" "/>
    </c:forEach>
    (всего ${fn:length(newUsers)})
  </p>
  <p>
    Замороженные пользователи:
    <c:forEach items="${frozenUsers}" var="user">
      <lor:user user="${user._1()}" bold="${user._2()}" link="true"/><c:out value=" "/>
    </c:forEach>
    (всего ${fn:length(frozenUsers)})
  </p>
  <p>
    Размороженные пользователи за последние 3 дня:
    <c:forEach items="${unFrozenUsers}" var="user">
      <lor:user user="${user._1()}" bold="${user._2()}" link="true"/><c:out value=" "/>
    </c:forEach>
    (всего ${fn:length(unFrozenUsers)})
  </p>
  <p>
    Заблокированные пользователи за последние 3 дня:
    <c:forEach items="${blockedUsers}" var="user">
      <lor:user user="${user}" link="true"/><c:out value=" "/>
    </c:forEach>
    (всего ${fn:length(blockedUsers)})
  </p>
  <p>
    Разблокированные пользователи за последние 3 дня:
    <c:forEach items="${unBlockedUsers}" var="user">
      <lor:user user="${user}" link="true"/><c:out value=" "/>
    </c:forEach>
    (всего ${fn:length(unBlockedUsers)})
  </p>

  <div class="userpic-list">
    <c:forEach items="${recentUserpics}" var="userpic">
      <a href="/people/${userpic._1().nick}/profile">
        <l:userpic userpic="${userpic._2()}"/>
      </a>
    </c:forEach>
  </div>
</c:if>

<jsp:include page="/WEB-INF/jsp/footer.jsp"/>
