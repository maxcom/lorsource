<%@ page info="last active topics" %>
<%@ page contentType="text/html; charset=utf-8" %>
<%--
  ~ Copyright 1998-2023 Linux.org.ru
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

<title>${ptitle}</title>
<jsp:include page="/WEB-INF/jsp/header.jsp"/>

<h1><i class="icon-tag"></i> <a href="${url}">${tagTitle}</a></h1>

<nav>
  <c:if test="${fn:length(sectionList)>1}">
    <c:forEach items="${sectionList}" var="cursection">
      <c:if test="${section == cursection.id}">
        <a href="${url}?section=${cursection.id}" class="btn btn-selected">${cursection.name}</a>
      </c:if>

      <c:if test="${section != cursection.id}">
        <a href="${url}?section=${cursection.id}" class="btn btn-default">${cursection.name}</a>
      </c:if>
    </c:forEach>
  </c:if>

  <c:if test="${not empty addUrl}">
    <a class="btn btn-primary" href="${addUrl}">Добавить</a>
  </c:if>
</nav>

<div class="infoblock" style="font-size: medium">
  <div class="fav-buttons">
    <c:if test="${showFavoriteTagButton}">
      <c:url var="tagFavUrl" value="/user-filter">
        <c:param name="newFavoriteTagName" value="${tag}"/>
      </c:url>

      <a id="tagFavAdd" href="${tagFavUrl}" title="В избранное"><i class="icon-eye"></i></a>
    </c:if>
    <c:if test="${not template.sessionAuthorized}">
      <a id="tagFavNoth" href="#"><i class="icon-eye"  title="Добавить в избранное"></i></a>
    </c:if>
    <c:if test="${showUnFavoriteTagButton}">
      <c:url var="tagFavUrl" value="/user-filter"/>

      <a id="tagFavAdd" href="${tagFavUrl}" title="Удалить из избранного" class="selected"><i class="icon-eye"></i></a>
    </c:if>
    <br><span id="favsCount" title="Кол-во пользователей, добавивших в избранное">${favsCount}</span>

    <br>

    <c:if test="${showIgnoreTagButton}">
      <c:url var="tagIgnUrl" value="/user-filter">
        <c:param name="newIgnoreTagName" value="${tag}"/>
      </c:url>

      <a id="tagIgnore" href="${tagIgnUrl}" title="Игнорировать"><i class="icon-eye-with-line"></i></a>
    </c:if>
    <c:if test="${!showIgnoreTagButton && !showUnIgnoreTagButton}">
      <a id="tagIgnNoth" href="#"><i class="icon-eye-with-line" title="Игнорировать"></i></a>
    </c:if>
    <c:if test="${showUnIgnoreTagButton}">
      <c:url var="tagIgnUrl" value="/user-filter"/>

      <a id="tagIgnore" href="${tagFavUrl}" title="Перестать игнорировать" class="selected"><i class="icon-eye-with-line"></i></a>
    </c:if>
    <br><span id="ignoreCount" title="Кол-во пользователей, игнорирующих тег">${ignoreCount}</span>
  </div>

  <c:if test="${counter > 0}">
    <p>
      Всего сообщений: ${counter}
    </p>
  </c:if>
</div>

<lor:tracker-topics-new messages="${messages}"/>

<div class="nav">
  <div style="display: table; width: 100%">
    <div style="display: table-cell; text-align: left">
      <c:if test="${not empty prevLink}">
        <a  href="${prevLink}">← предыдущие</a>
      </c:if>
    </div>
    <div style="display: table-cell; text-align: right">
      <c:if test="${not empty nextLink}">
        <a href="${nextLink}">следующие →</a>
      </c:if>
    </div>
  </div>
</div>

<jsp:include page="/WEB-INF/jsp/footer.jsp"/>
