<%@ page pageEncoding="koi8-r" contentType="text/html; charset=utf-8"%>
<%@ page import="java.io.File,java.io.IOException,java.net.URLEncoder,java.sql.Connection,java.sql.PreparedStatement,java.sql.ResultSet,java.sql.Statement,java.util.Date" errorPage="/error.jsp" buffer="64kb"%>
<%@ page import="java.util.List"%>
<%@ page import="java.util.Properties"%>
<%@ page import="java.util.Random"%>
<%@ page import="java.util.logging.Logger"%>
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
<% Template tmpl = new Template(request, config, response); %>
<%= tmpl.head() %>
	<title>Получить забытый пароль</title>
<jsp:include page="WEB-INF/jsp/header.jsp"/>

<%
   if (request.getParameter("nick")==null) {
%>
<H1>Получить забытый пароль</H1>
<form method=POST action="lostpwd.jsp">
Имя:
<input type=text name=nick size=40><br>
Email:
<input type=text name=email size=40><br>
<input type=submit value="Get/Получить">
</form>
<%
  } else {
    Connection db = null;
    try {
      String nick = request.getParameter("nick");
      String useremail = request.getParameter("email");

      db = tmpl.getConnection();
      db.setAutoCommit(false);

      User user = User.getUser(db, nick);
      user.checkAnonymous();

      Statement st = db.createStatement();
      ResultSet rs = st.executeQuery("SELECT passwd, canmod, email, name, lostpwd>CURRENT_TIMESTAMP-'1 week'::interval as datecheck FROM users WHERE id=" + user.getId());

      rs.next();

      if (rs.getBoolean("canmod")) {
        throw new AccessViolationException("this feature is not for you, ask me directly");
      }

      if (rs.getBoolean("datecheck")) {
        throw new AccessViolationException("mail flood");
      }
      if (rs.getString("email") == null || !rs.getString("email").equals(useremail)) {
        throw new AccessViolationException("mail не совпадает с указанным при регистрации");
      }


      String password = rs.getString("passwd");
      InternetAddress email = new InternetAddress(rs.getString("email"));
      String name = rs.getString("name");
      if (name != null && !"".equals(name)) {
        email.setPersonal(name);
      }
      else {
        email.setPersonal(nick);
      }

      rs.close();
      st.executeUpdate("UPDATE users SET lostpwd=CURRENT_TIMESTAMP WHERE id=" + user.getId());

      Properties props = new Properties();
      props.put("mail.smtp.host", "localhost");
      Session mailSession = Session.getDefaultInstance(props, null);

      MimeMessage msg = new MimeMessage(mailSession);
      msg.setFrom(new InternetAddress("no-reply@linux.org.ru"));

      msg.addRecipient(MimeMessage.RecipientType.TO, email);
      msg.setSubject("Your password @linux.org.ru");
      msg.setSentDate(new Date());
      msg.setText("Hello!\n\nThis messages was sent as a reply to \"Lost password\" request at WWW.LINUX.ORG.RU website.\n\nHere is your password: " + password + "\n\nBest wishes!");

      Transport.send(msg);

      db.commit();
      out.print("Ваш пароль был выслан по вашему email'у");
      st.close();
    } finally {
      if (db != null) {
        db.close();
      }
    }
  }
%>
<jsp:include page="WEB-INF/jsp/footer.jsp"/>
