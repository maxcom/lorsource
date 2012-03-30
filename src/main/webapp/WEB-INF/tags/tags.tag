<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib prefix="lor" uri="http://www.linux.org.ru" %>
<%@ tag import="java.io.UnsupportedEncodingException" %>
<%@ tag import="java.net.URLEncoder" %>
<%@ tag import="java.util.List" %>
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
<%@ tag pageEncoding="UTF-8" %>
<%@ attribute name="list" required="true" type="java.util.List" %>
<%@ attribute name="favoriteTags" required="false" type="java.util.List" %>
<%@ attribute name="ignoreTags" required="false" type="java.util.List" %>

<%--@elvariable id="template" type="ru.org.linux.site.Template"--%>

<p class="tags">  Метки: <span class="tag">
<c:forEach var="tagName" items="${list}">

    <c:url value="/view-news.jsp" var="tag_url">
        <c:param name="tag" value="${tagName}" />
    </c:url>
    <a rel="tag" href="${tag_url}">${tagName}</a>
    <c:if test="${favoriteTags != null and not (favoriteTags == null || lor:arrayContains(favoriteTags, tagName))}">
      <a href="javascript:addUserTag(this, '<c:out value="${tagName}"/>', true);" style="display: none;">
      <img src="/img/add-favorite-tag-ico.png" title="Добавить в фаворитные" alt="[+]"/></a>
    </c:if>
    <c:if test="${not template.moderatorSession && ignoreTags != null && not (ignoreTags == null || lor:arrayContains(ignoreTags, tagName))}">
      <a href="javascript:addUserTag(this, '<c:out value="${tagName}"/>', false);" style="display: none;">
      <img src="/img/add-ignore-tag-ico.png" title="Добавить в игнорируемые" alt="[x]"/></a>
    </c:if>
    &nbsp;
</c:forEach>
</span>
<form class="favoriteTagForm" action="<c:url value="/user-filter/favorite-tag"/>" method="POST">
  <input class="tagName" type="hidden" name="tagName" value="<c:out value="${tagName}"/>">
  <input type="hidden" name="add" value="action">
</form>
<c:if test="${not template.moderatorSession}">
  <form class="ignoreTagForm" action="<c:url value="/user-filter/ignore-tag"/>" method="POST">
    <input class="tagName" type="hidden" name="tagName" value="<c:out value="${tagName}"/>">
    <input type="hidden" name="add" value="action">
  </form>
</c:if>
</p>
<script type="text/javascript"><!--
$(document).ready(function() {
    $("p.tags a").show();
});
//--></script>


