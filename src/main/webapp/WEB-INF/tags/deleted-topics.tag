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
<%@ tag pageEncoding="UTF-8" trimDirectiveWhitespaces="true"%>
<%@ attribute name="topics" required="true" type="java.util.List<ru.org.linux.topic.DeletedTopic>" %>
<%@ attribute name="showDates" required="true" type="java.lang.Boolean" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib tagdir="/WEB-INF/tags" prefix="lor" %>

<div class=forum>
  <table class="message-table" width="100%">
    <thead>
    <tr>
      <th>Заголовок</th>
      <th>Причина удаления</th>
      <c:if test="${showDates}">
        <th>Штраф</th>
        <th>Дата</th>
      </c:if>
    </tr>
    <tbody>

    <c:forEach items="${topics}" var="topic">

    <tr>
      <td><a href="view-message.jsp?msgid=${topic.id}">${topic.title}</a> (${topic.nick})</td>
      <td>${topic.reason}</td>
      <c:if test="${showDates}">
        <td>${topic.bonus}</td>
        <td>
          написано <lor:dateinterval date="${topic.postdate}"/><br>
          удалено <lor:dateinterval date="${topic.delDate}"/>
        </td>
      </c:if>
    </tr>
    </c:forEach>
  </table>
</div>

