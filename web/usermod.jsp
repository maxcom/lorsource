<%@ page import="java.net.URLEncoder" %>
<%@ page import="java.sql.Connection" %>
<%@ page import="java.sql.Statement" %>
<%@ page import="java.util.Random" %>
<%@ page import="java.util.logging.Logger" %>
<%@ page import="ru.org.linux.site.*" %>
<%@ page import="ru.org.linux.util.HTMLFormatter" %>
<%@ page import="ru.org.linux.util.ServletParameterParser" %>
<%@ page pageEncoding="koi8-r" contentType="text/html;charset=utf-8" language="java"   %>
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
  Logger logger = Logger.getLogger("ru.org.linux");
%>
<jsp:include page="/WEB-INF/jsp/head.jsp"/>

<title>usermod</title>
<jsp:include page="WEB-INF/jsp/header.jsp"/>

<%
  if (!tmpl.isModeratorSession()) {
    throw new IllegalAccessException("Not authorized");
  }

  String action = new ServletParameterParser(request).getString("action");
  int id = new ServletParameterParser(request).getInt("id");

  if (!request.getMethod().equals("POST")) {
    throw new IllegalAccessException("Invalid method");
  }

  Connection db = null;

  try {
    db = LorDataSource.getConnection();
    db.setAutoCommit(false);

    Statement st = db.createStatement();

    User user = User.getUser(db, id);

    User moderator = User.getUser(db, (String) session.getValue("nick"));

    boolean redirect = true;

    if (action.equals("block") || action.equals("block-n-delete-comments")) {
      if (!user.isBlockable()) {
        throw new AccessViolationException("Пользователя " + user.getNick() + " нельзя заблокировать");
      }

      user.block(db);
      logger.info("User " + user.getNick() + " blocked by " + session.getValue("nick"));

      if (action.equals("block-n-delete-comments")) {
        out.print(user.deleteAllComments(db, moderator));
        redirect = false;
      }
    } else if (action.equals("toggle_corrector")) {
      if (user.getScore()<User.CORRECTOR_SCORE) {
        throw new AccessViolationException("Пользователя " + user.getNick() + " нельзя сделать корректором");
      }

      if (user.canCorrect()) {
        st.executeUpdate("UPDATE users SET corrector='f' WHERE id=" + id);
      } else {
        st.executeUpdate("UPDATE users SET corrector='t' WHERE id=" + id);
      }
    } else if (action.equals("unblock")) {
      if (!user.isBlockable()) {
        throw new AccessViolationException("Пользователя " + user.getNick() + " нельзя разблокировать");
      }

      st.executeUpdate("UPDATE users SET blocked='f' WHERE id=" + id);
      logger.info("User " + user.getNick() + " unblocked by " + session.getValue("nick"));
    } else if (action.equals("remove_userpic")) {
      if (user.canModerate()) {
        throw new AccessViolationException("Пользователю " + user.getNick() + " нельзя удалить картинку");
      }

      if (user.getPhoto() == null) {
        throw new AccessViolationException("Пользователь " + user.getNick() + " картинки не имеет");
      }

      st.executeUpdate("UPDATE users SET photo=null WHERE id=" + id);
      user.changeScore(db, -10);
      logger.info("Clearing " + user.getNick() + " userpic by " + session.getValue("nick"));
    } else if (action.equals("remove_userinfo")) {
      if (user.canModerate()) {
        throw new AccessViolationException("Пользователю " + user.getNick() + " нельзя удалить сведения");
      }

      tmpl.getObjectConfig().getStorage().updateMessage("userinfo", String.valueOf(id), "");

      user.changeScore(db, -10);
      logger.info("Clearing " + user.getNick() + " userinfo");
    } else {
      throw new UserErrorException("Invalid action=" + HTMLFormatter.htmlSpecialChars(action));
    }

    if (redirect) {
      Random random = new Random();

      response.setHeader("Location", tmpl.getMainUrl() + "whois.jsp?nick=" + URLEncoder.encode(user.getNick()) + "&nocache=" + random.nextInt());
      response.setStatus(HttpServletResponse.SC_MOVED_TEMPORARILY);
    }

    db.commit();
  } finally {
    if (db != null) {
      db.close();
    }
  }

%>
<jsp:include page="WEB-INF/jsp/footer.jsp"/>