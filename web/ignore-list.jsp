<%@ page import="java.io.File"%>
<%@ page import="java.io.IOException"%>
<%@ page import="java.net.URLEncoder"%>
<%@ page import="java.sql.*" %>
<%@ page import="java.util.*" %>
<%@ page import="java.util.Date" %>
<%@ page import="java.util.logging.Logger" %>
<%@ page import="javax.mail.Session" %>
<%@ page import="javax.mail.Transport" %>
<%@ page import="javax.mail.internet.InternetAddress" %>
<%@ page import="javax.mail.internet.MimeMessage" %>
<%@ page import="javax.servlet.http.Cookie" %>
<%@ page import="javax.servlet.http.HttpServletResponse" %>
<%@ page import="org.apache.commons.fileupload.FileItem" %>
<%@ page import="org.apache.commons.fileupload.disk.DiskFileItemFactory" %>
<%@ page import="org.apache.commons.fileupload.servlet.ServletFileUpload" %>
<%@ page import="org.apache.commons.lang.StringUtils" %>
<%@ page import="ru.org.linux.boxlet.BoxletVectorRunner" %>
<%@ page import="ru.org.linux.site.*" %>
<%@ page import="ru.org.linux.storage.StorageNotFoundException" %>
<%@ page import="ru.org.linux.util.*" %>
<%@ page pageEncoding="koi8-r" contentType="text/html; charset=utf-8"  %>
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

      User addUser = User.getUser(db, nick);

      if (!ail.containsUser(addUser)) {
        ail.addUser(db, addUser);
 //       if (user.getScore() > User.IGNORE_PENALTI_THRESHOLD) {
  //        addUser.changeScore(db, -User.IGNORE_PENALTI_SCORE);
   //     }
      }
    } else if (request.getParameter("del") != null) {
      int uid = new ServletParameterParser(request).getInt("ignore_list");

      User delUser = User.getUserCached(db, uid);

      if (!ail.removeNick(db, uid)) {
        throw new BadInputException("неверный ник");
      } else {
//        if (user.getScore()>User.IGNORE_PENALTI_THRESHOLD) {
 //         delUser.changeScore(db, User.IGNORE_PENALTI_SCORE);
  //      }
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
  Map<Integer,String> ignoreList = ignore.getIgnoreList();
  session.setAttribute("ignoreList", ignoreList);

  //db.commit();
%>

<h1>Список Игнорирования</h1>

<form action="ignore-list.jsp" method="POST">

  Ник: <input type="text" name="nick" size="20" maxlength="80"><input type="submit" name="add" value="Добавить"><br>
<!-- input type="checkbox" name="activated" value="1" <%= ignore.isActivated()?"checked":"" %>> Список включен <input type="submit" name="set" value="Установить"><br -->
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
