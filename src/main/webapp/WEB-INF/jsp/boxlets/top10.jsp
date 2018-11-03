<%--
  ~ Copyright 1998-2015 Linux.org.ru
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
<%--@elvariable id="messages" type="java.util.List<ru.org.linux.topic.TopTenDao.TopTenMessageDTO>"--%>
<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="lor" uri="http://www.linux.org.ru" %>
<%@ taglib prefix="l" uri="http://www.linux.org.ru" %>
<%@ taglib prefix="fn" uri="http://java.sun.com/jsp/jstl/functions"%>
<h2>Top 10</h2>

<div class="boxlet_content">
  Наиболее обсуждаемые темы этого месяца:
  <ul>
    <c:forEach items="${messages}" var="message">
      <li>
      <c:url value="${message.url}" var="msg_link">
        <c:if test="${message.pages == 1}">
          <c:param name="lastmod" value="${message.lastmod.time}"/>
        </c:if>
      </c:url>
      <a href="${fn:escapeXml(msg_link)}"><l:title>${message.title}</l:title></a>
      <c:if test="${message.pages gt 1}">
        <c:url value="${message.url}/page${message.pages-1}" var="page_link">
          <c:param name="lastmod" value="${message.lastmod.time}"/>
        </c:url>
        (стр.&nbsp;<a href="${fn:escapeXml(page_link)}">${message.pages}</a>)
      </c:if>
      (${message.commentCount})
      </li>
    </c:forEach>
  </ul>
</div>