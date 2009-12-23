<%@ page import="java.sql.Connection"%>
<%@ page import="java.util.Map"%>
<%@ page import="ru.org.linux.site.*"%>
<%@ page import="ru.org.linux.util.ServletParameterParser" %>
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

<jsp:include page="WEB-INF/jsp/head.jsp"/>

<%
  response.addHeader("Cache-Control", "no-store, no-cache, must-revalidate");
  response.addHeader("Pragma", "no-cache");

  if (!Template.isSessionAuthorized(session)) {
    throw new AccessViolationException("Not authorized");
  }

  Connection db = null;
  try {
%>
<title>Список Игнорирования</title>

<jsp:include page="WEB-INF/jsp/header.jsp"/>

<%

  db = LorDataSource.getConnection();
  User user = User.getUser(db, (String) session.getAttribute("nick"));
  user.checkAnonymous();

  if ("POST".equals(request.getMethod())) {
    IgnoreList ail = new IgnoreList(db, user.getId());

    if (request.getParameter("add") != null) {
      // Add nick to ignore list
      String nick = request.getParameter("nick");
      if (nick == null) {
        nick = "";
      }
      if ("".equals(nick)) {
        throw new BadInputException("ник не может быть пустым");
      }
      if (nick.equals(user.getNick())){
        throw new BadInputException("нельзя игнорировать самого себя");
      }

      User addUser = User.getUser(db, nick);

      if (!ail.containsUser(addUser)) {
        ail.addUser(db, addUser);
      }
    } else if (request.getParameter("del") != null) {
      int uid = new ServletParameterParser(request).getInt("ignore_list");

      if (!ail.removeNick(db, uid)) {
        throw new BadInputException("неверный ник");
      }
    }
  }

  IgnoreList ignore = new IgnoreList(db, user.getId());
  Map<Integer,String> ignoreList = ignore.getIgnoreList();
%>

<h1>Список Игнорирования</h1>

<form action="ignore-list.jsp" method="POST">

  Ник: <input type="text" name="nick" size="20" maxlength="80"><input type="submit" name="add" value="Добавить"><br>
<% if (!ignoreList.isEmpty()) { %>
<select name="ignore_list" size="10" width="20">
<%
  for (Object o : ignoreList.keySet()) {
    int id = (Integer) o;
    String nick = ignoreList.get(id);
    if (id > 0) {
%>
  <option value="<%= id%>"><%= nick %>
  </option>
  <%
      }
    }
  %>
 </select>
<br>
  <input type="submit" name="del" value="Удалить">
<% } %>
</form>

<%
  } finally {
    if (db != null) {
      db.close();
    }
  }
%>

<jsp:include page="WEB-INF/jsp/footer.jsp"/>