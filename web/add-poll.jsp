<%@ page import="ru.org.linux.site.AccessViolationException"%>
<%@ page import="ru.org.linux.site.Poll"%>
<%@ page import="ru.org.linux.site.Template"%>
<%@ page import="ru.org.linux.util.HTMLFormatter"%>
<%@ page contentType="text/html; charset=utf-8"  %>
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

<jsp:include page="/WEB-INF/jsp/head.jsp"/>


<%
//  response.addHeader("Cache-Control", "no-store, no-cache, must-revalidate");
//  response.addHeader("Pragma", "no-cache");

  if (!Template.isSessionAuthorized(session)) {
    throw new AccessViolationException("Not authorized");
  }

%>
<title>Добавить опрос</title>

<jsp:include page="WEB-INF/jsp/header.jsp"/>

<h1>Добавить опрос</h1>

<form action="add.jsp" method="POST">
<input type="hidden" name="session" value="<%= HTMLFormatter.htmlSpecialChars(session.getId()) %>">  
<input type="hidden" name="group" value="19387">
<input type="hidden" name="mode" value="html">
<input type="hidden" name="texttype" value="0">

  Вопрос: <input type="text" name="title" size="40"><br>
  <%
    for (int i=0; i< Poll.MAX_POLL_SIZE; i++) {
      %>
        Вариант #<%= i%>: <input type="text" name="var<%= i%>" size="40"><br>
      <%
    }
  %>
  <input type="submit" value="Добавить">
</form>

<jsp:include page="WEB-INF/jsp/footer.jsp"/>
