<%@ page contentType="text/html; charset=utf-8" %>
<%@ page import="java.sql.Connection" %>
<%@ page import="java.sql.ResultSet" %>
<%@ page import="java.sql.Statement" %>
<%@ page import="java.sql.Timestamp" %>
<%@ page import="ru.org.linux.site.*" %>
<%@ taglib tagdir="/WEB-INF/tags" prefix="lor" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>

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
  <c:if test="${message.sectionId == 1}">
    Метки (теги):
    <input type="text" id="tags" name="tags" size=40 value="${message.tags}"><br>
    Популярные теги: <%= Tags.getEditTags(Tags.getTopTags(db)) %> <br>
  </c:if>
  <%
    Message message = (Message) request.getAttribute("message");
  %>
  <c:if test="${(message.sectionId == 1) || (message.sectionId == 3)}">
    Переместить в группу:
    <select name="chgrp">
      <%
        Statement st = db.createStatement();
        ResultSet rq = st.executeQuery("SELECT id, title FROM groups WHERE section=" + message.getSectionId() + " ORDER BY id");
        out.println("<option value=" + message.getGroupId() + '>' + message.getGroupTitle() + " (не менять)</option>");
        while (rq.next()) {
          int id = rq.getInt("id");
          if (id != message.getGroupId()) {
            out.println("<option value=" + id + '>' + rq.getString("title") + "</option>");
          }
        }
        if (rq != null) {
          rq.close();
        }
        st.close();
      %>
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
