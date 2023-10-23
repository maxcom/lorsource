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
<%@ tag pageEncoding="UTF-8" trimDirectiveWhitespaces="true"%>
<%@ attribute name="messages" required="false" type="java.util.List" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib prefix="l" uri="http://www.linux.org.ru" %>
<%@ taglib tagdir="/WEB-INF/tags" prefix="lor" %>
<%@ taglib prefix="fn" uri="http://java.sun.com/jsp/jstl/functions" %>

<div class=forum>
  <table class="message-table">
    <thead>
    <tr>
      <th class="hideon-tablet">Группа</th>
      <th>Заголовок</th>
      <th>Последнее сообщение</th>
      <th><i class="icon-reply"></i></th>
    </tr>
    </thead>
    <tbody>
    <c:forEach var="msg" items="${messages}">
      <c:set var="groupLink"><%--
                         --%><a href="${msg.groupUrl}" class="secondary">${msg.groupTitle}</a><%--

                         --%><c:if test="${msg.uncommited}">, не подтверждено</c:if><%--
                        --%></c:set>
      <tr>
        <td class="hideon-tablet">${groupLink}</td>
        <td>
          <c:if test="${msg.resolved}">
            <img src="/img/solved.png" alt="решено" title="решено" width=15 height=15>
          </c:if>
          <a href="${msg.lastPageUrl}">
            <c:forEach var="tag" items="${msg.tags}">
              <span class="tag">${tag}</span>
            </c:forEach>

            <l:title>${msg.title}</l:title>
          </a>

          (<%--
            --%><c:if test="${msg.topicAuthor != null}"><lor:user user="${msg.topicAuthor}"/><%--
            --%><span class="hideon-desktop"> в </span><%--
            --%></c:if><span class="hideon-desktop">${groupLink}</span>)
        </td>
        <td class="dateinterval">
          <lor:dateinterval date="${msg.postdate}"/>, <lor:user user="${msg.author}"/>
        </td>
        <td class='numbers'>
          <c:choose>
          <c:when test="${msg.commentCount==0}">
          -
          </c:when>
          <c:otherwise>
            ${msg.commentCount}
          </c:otherwise>
          </c:choose>
      </tr>
    </c:forEach>
    </tbody>

  </table>
</div>
