<%@ page contentType="text/html; charset=utf-8"%>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib tagdir="/WEB-INF/tags" prefix="lor" %>
<%@ taglib prefix="fn" uri="http://java.sun.com/jsp/jstl/functions" %>
<%@ taglib prefix="form" uri="http://www.springframework.org/tags/form" %>
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
<%--@elvariable id="counter" type="java.lang.Integer"--%>
<%--@elvariable id="favsCount" type="java.lang.Integer"--%>
<%--@elvariable id="sectionList" type="java.util.List<ru.org.linux.section.Section>"--%>
<%--@elvariable id="tag" type="java.lang.String"--%>
<%--@elvariable id="offset" type="java.lang.Integer"--%>
<%--@elvariable id="section" type="java.lang.Integer"--%>
<%--@elvariable id="messages" type="java.util.List<ru.org.linux.topic.PersonalizedPreparedTopic>"--%>

<jsp:include page="/WEB-INF/jsp/head.jsp"/>
<title>${ptitle}</title>

<jsp:include page="/WEB-INF/jsp/header.jsp"/>

<h1><i class="icon-tag"></i> ${navtitle}</h1>

<nav>
  <c:if test="${counter>10}">
    <c:forEach items="${sectionList}" var="cursection">
      <c:if test="${section == cursection.id}">
        <a href="${url}?section=${cursection.id}" class="btn btn-selected">${cursection.name}</a>
      </c:if>

      <c:if test="${section != cursection.id}">
        <a href="${url}?section=${cursection.id}" class="btn btn-default">${cursection.name}</a>
      </c:if>
    </c:forEach>
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

  <p>
    Всего сообщений: ${counter}
  </p>
</div>

<c:forEach var="msg" items="${messages}">
  <lor:news
          preparedMessage="${msg.preparedTopic}"
          messageMenu="${msg.topicMenu}"
          multiPortal="${section==0}"
          minorAsMajor="true"
          moderateMode="false"/>
</c:forEach>

<table class="nav">
  <tr>
    <c:if test="${not empty prevLink}">
      <td align="left" width="35%">
        <a href="${prevLink}">← назад</a>
      </td>
    </c:if>
    <c:if test="${not empty nextLink}">
      <td width="35%" align="right">
        <a href="${nextLink}">вперед →</a>
      </td>
    </c:if>
  </tr>
</table>
<script type="text/javascript">
  $script.ready('lorjs', function() {
    tag_memories_form_setup("${tag}", "${fn:escapeXml(csrfToken)}");
  });
</script>
<jsp:include page="/WEB-INF/jsp/footer.jsp"/>
