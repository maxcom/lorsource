<%@ page contentType="text/html; charset=koi8-r"%>
<%@ page import="java.sql.Connection,java.sql.PreparedStatement, java.util.Date, javax.servlet.http.Cookie, javax.servlet.http.HttpServletResponse, ru.org.linux.site.AccessViolationException, ru.org.linux.site.BadInputException" errorPage="/error.jsp"%>
<%@ page import="ru.org.linux.site.Template"%>
<%@ page import="ru.org.linux.site.User"%>
<% Template tmpl = new Template(request, config, response); %>
<%= tmpl.head() %>
<%
  Connection db = null;

  try {
	db = tmpl.getConnection("login");
        db.setAutoCommit(false);
        String nick=request.getParameter("nick");
	if (nick==null || "".equals(nick))
		throw new BadInputException("Не указан nick");

	User user=new User(db, nick);

        if (!user.isActivated()) {
          String activation = request.getParameter("activate");

          if (activation==null) {
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

        if (session==null)
		throw new BadInputException("не удалось открыть сессию; созможно отсутствует поддержка Cookie");

	session.putValue("login", Boolean.TRUE);
	session.putValue("nick", nick);
	session.putValue("moderator", Boolean.valueOf(user.canModerate()));

	Cookie cookie=new Cookie("password", user.getMD5(tmpl.getSecret()));
	cookie.setMaxAge(60*60*24*31*24);
	cookie.setPath("/");
	response.addCookie(cookie);

        response.setHeader("Location", tmpl.getMainUrl()+"edit-profile.jsp?mode=setup&profile="+nick);
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
