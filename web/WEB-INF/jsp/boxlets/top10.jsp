<%--
  ~ Copyright 1998-2009 Linux.org.ru
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

<%--
  Created by IntelliJ IDEA.
  User: rsvato
  Date: May 4, 2009
  Time: 1:58:24 PM
  To change this template use File | Settings | File Templates.
--%>
<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="lor" uri="http://www.linux.org.ru" %>
<lor:cache key="top10.boxlet?style=${style}&amp;perPage=${perPage}" expire="${5 * 60 * 1000}">
  <h2>Top 10</h2>

  <div class="boxlet_content">
    <h3>Наиболее обсуждаемые темы этого месяца</h3>
    <c:url var="arrow_url" value="/${style}/img/arrow.gif"/>
    <c:forEach items="${messages}" var="message">
      <c:choose>
        <c:when test="${message.movedUp}">
          <img src="${arrow_url}" alt="[up]" width="10" height="12"/>
        </c:when>
        <c:otherwise>*</c:otherwise>
      </c:choose>
      <c:url value="/view-message.jsp" var="msg_link">
        <c:param name="msgid" value="${message.msgid}"/>
        <c:param name="lastmod" value="${message.lastmod.time}"/>
      </c:url>
      <a href="${msg_link}">${message.title}</a>
      <c:if test="${message.pages gt 1}">
        <c:url value="/view-message.jsp" var="page_link">
          <c:param name="msgid" value="${message.msgid}"/>
          <c:param name="lastmod" value="${message.lastmod.time}"/>
          <c:param name="page" value="${message.pages - 1}"/>
        </c:url>
        (стр. <a href="${page_link}">${message.pages}</a>)
      </c:if>
      (${message.answers})
      <br/>
    </c:forEach>
  </div>
</lor:cache>