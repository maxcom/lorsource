<%@ page contentType="text/html; charset=utf-8" %>
<%@ page import="java.sql.Connection" %>
<%@ page import="java.sql.Timestamp" %>
<%@ page import="ru.org.linux.site.LorDataSource" %>
<%@ page import="ru.org.linux.site.Section" %>
<%@ page import="ru.org.linux.site.Tags" %>
<%@ page import="ru.org.linux.site.Template" %>
<%@ taglib tagdir="/WEB-INF/tags" prefix="lor" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
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
<%--@elvariable id="groups" type="java.util.List<ru.org.linux.site.Group>"--%>
<%--@elvariable id="message" type="ru.org.linux.site.Message"--%>

<% Template tmpl = Template.getTemplate(request);
%>
<jsp:include page="/WEB-INF/jsp/head.jsp"/>

<title>Подтверждение сообщения</title>
<jsp:include page="/WEB-INF/jsp/header.jsp"/>

<%
  Connection db = null;

  try {
    db = LorDataSource.getConnection();
%>
<h1>Подтверждение сообщения</h1>

<p>
  Данная форма предназначена для администраторов сайта и пользователей,
  имеющих права подтверждения сообщений.

<p>

<div class=messages>
  <lor:message db="<%= db %>" message="${message}" showMenu="false"
               user="<%= Template.getNick(session) %>"/>
</div>

<form method=POST action="commit.jsp">
  <input type=hidden name=msgid value="${msgid}">
  <input type=hidden name=groupid value="${message.groupId}">
  Заголовок:
  <input type=text name=title size=40 value="${message.title}">
  <br>
    Bonus score (от 0 до 20):
    <input type=text name=bonus size=40 value="3">

  <br>
    Метки (теги):
    <input type="text" id="tags" name="tags" size=40 value="${message.tags}"><br>
    Популярные теги: <%= Tags.getEditTags(Tags.getTopTags(db)) %> <br>
  <c:if test="${(message.sectionId == 1) || (message.sectionId == 3)}">
    Группа:
    <select name="chgrp">
      <c:forEach var="group" items="${groups}">
        <c:if test="${group.id != message.groupId}">
          <option value="${group.id}">${group.title}</option>
        </c:if>
        <c:if test="${group.id == message.groupId}">
          <option value="${group.id}" selected="selected">${group.title}</option>
        </c:if>
      </c:forEach>
    </select><br>
  </c:if>
  <%
      Section section = (Section) request.getAttribute("section");

      Timestamp lastCommit = section.getLastCommitdate(db);
      if (lastCommit != null) {
        out.println("Последнее подтверждение в разделе: " + tmpl.dateFormat.format(lastCommit) + "<br>");
      }
    } finally {
      if (db != null) {
        db.close();
      }
    }
  %>
  <input type=submit value="Submit/Подтвердить">
</form>
<jsp:include page="/WEB-INF/jsp/footer.jsp"/>
