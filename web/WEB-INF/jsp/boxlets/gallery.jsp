<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="lor" uri="http://www.linux.org.ru" %>

<%--
  ~ Copyright 1998-2010 Linux.org.ru
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
  <h2><a href="view-news.jsp?section=3">Галерея</a></h2>

  <div class="boxlet_content">
    <h3>Последние скриншоты</h3>
    <c:forEach var="item" items="${items}">
      <div style="margin-bottom: 1em">
      <div align="center">
        <c:url var="url" value="/view-message.jsp">
          <c:param name="msgid" value="${item.msgid}"/>
        </c:url>
        <a href="${url}">
          <c:choose>
            <c:when test="${not empty item.info}">
              <img src="${item.icon}" alt="Скриншот: ${item.title}" ${item.info.code}/>
            </c:when>
            <c:otherwise>
              [bad image] <img src="${item.icon}" alt="Скриншот: ${item.title}"/>
            </c:otherwise>
          </c:choose>
        </a>
      </div>
      <i>
        <c:choose>
          <c:when test="${not empty item.imginfo}">
            ${item.imginfo.width}x${item.imginfo.height}
          </c:when>
          <c:otherwise>
            [bad image]
          </c:otherwise>
        </c:choose>
        <c:url value="/people/${item.nick}/profile" var="nickurl"/>
      </i> ${item.title} от <a href="${nickurl}">${item.nick}</a> (${item.stat})
      </div>
    </c:forEach>
    <a href="view-news.jsp?section=3">другие скриншоты...</a>
  </div>