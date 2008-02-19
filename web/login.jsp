<%@ page pageEncoding="koi8-r" contentType="text/html; charset=utf-8"%>
<%@ page import="java.sql.Connection,java.sql.PreparedStatement, java.sql.ResultSet, java.sql.Statement, java.util.Date, java.util.List, javax.servlet.http.Cookie" errorPage="/error.jsp"%>
<%@ page import="javax.servlet.http.HttpServletResponse"%>
<%@ page import="ru.org.linux.boxlet.BoxletVectorRunner"%>
<%@ page import="ru.org.linux.site.*" %>
<% Template tmpl = new Template(request, config, response); %>
<%= tmpl.head() %>
<%
  Connection db = null;

  try {
    db = tmpl.getConnection();
    db.setAutoCommit(false);
    String nick = request.getParameter("nick");
    if (nick == null || "".equals(nick))
      throw new BadInputException("Не указан nick");

    User user = User.getUser(db, nick);

    if (!user.isActivated()) {
      String activation = request.getParameter("activate");

      if (activation == null) {
        throw new AccessViolationException("Not activated");
      }

      String regcode = user.getActivationCode(tmpl.getSecret());

      if (regcode.equals(activation)) {
        PreparedStatement pst = db.prepareStatement("UPDATE users SET activated='t' WHERE id=?");
        pst.setInt(1, user.getId());
        pst.executeUpdate();
      } else {
        throw new AccessViolationException("Bad activation code");
      }
    }

    user.checkAnonymous();
    user.checkPassword(request.getParameter("passwd"));

    if (session == null)
      throw new BadInputException("не удалось открыть сессию; созможно отсутствует поддержка Cookie");

    session.putValue("login", Boolean.TRUE);
    session.putValue("nick", nick);
    session.putValue("moderator", user.canModerate());

    Cookie cookie = new Cookie("password", user.getMD5(tmpl.getSecret()));
    cookie.setMaxAge(60 * 60 * 24 * 31 * 24);
    cookie.setPath("/");
    response.addCookie(cookie);

    Cookie prof = new Cookie("profile", nick);
    prof.setMaxAge(60 * 60 * 24 * 31 * 12);
    prof.setPath("/");
    response.addCookie(prof);

    response.setHeader("Location", tmpl.getMainUrl());
    response.setStatus(HttpServletResponse.SC_MOVED_TEMPORARILY);

    User.updateUserLastlogin(db, nick, new Date());

    db.commit();
%>
<title>Login</title>
<%= tmpl.DocumentHeader() %>
<h1>Вход прошел успешно</h1>

<strong>Внимание:</strong>
<ul>
<li>вход работает успешно только в том случае, если у вас включена
поддержка Cookies в броузере
<li>пожалуйста, не забывайте сделать <em>Logout</em> при выходе
</ul>
<%
  } finally {
    if (db!=null) db.close();
  }
%>
<%=	tmpl.DocumentFooter() %>
