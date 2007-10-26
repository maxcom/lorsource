<%@ page import="java.sql.Connection"%>
<%@ page import="java.util.Map"%>
<%@ page import="ru.org.linux.site.*"%>
<%@ page contentType="text/html; charset=koi8-r" errorPage="/error.jsp"%>
<% Template tmpl = new Template(request, config, response);%>
<%= tmpl.head() %>

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

<%= tmpl.DocumentHeader() %>

<%

  db = tmpl.getConnection();
  //db.setAutoCommit(false);
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
      ail.addNick(db, nick);
    } else if (request.getParameter("del") != null) {
      int uid = tmpl.getParameters().getInt("ignore_list");
      if (!ail.removeNick(db, uid)) {
        throw new BadInputException("неверный ник");
      }
    } else if (request.getParameter("set") != null) {
      // Enable/Disable ignore list
      boolean activated = false;
      if (request.getParameter("activated") != null) {
        activated = true;
      }
      ail.setActivated(activated);
    }

  }

  IgnoreList ignore = new IgnoreList(db, user.getId());
  Map ignoreList = ignore.getIgnoreList();
  session.setAttribute("ignoreList", ignoreList);

  //db.commit();
%>

<h1>Список Игнорирования</h1>

<form action="ignore-list.jsp" method="POST">

  Ник: <input type="text" name="nick" size="20" maxlength="80"><input type="submit" name="add" value="Добавить"><br>
<!-- input type="checkbox" name="activated" value="1" <%= ignore.getActivated()?"checked":"" %>> Список включен <input type="submit" name="set" value="Установить"><br -->
<% if (!ignoreList.isEmpty()) { %>
<select name="ignore_list" size="10" width="20">
<%
  for (Object o : ignoreList.keySet()) {
    int id = (Integer) o;
    String nick = (String) ignoreList.get(id);
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

<%=	tmpl.DocumentFooter() %>
