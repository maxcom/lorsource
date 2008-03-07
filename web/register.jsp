<%@ page pageEncoding="koi8-r" contentType="text/html; charset=utf-8"%>
<%@ page import="java.io.File,java.io.IOException,java.net.URLEncoder,java.sql.Connection,java.sql.PreparedStatement"  %>
<%@ page import="java.sql.ResultSet"%>
<%@ page import="java.sql.Statement"%>
<%@ page import="java.util.Date"%>
<%@ page import="java.util.List"%>
<%@ page import="java.util.Properties"%>
<%@ page import="java.util.Random"%>
<%@ page import="java.util.logging.Logger"%>
<%@ page import="javax.mail.Session"%>
<%@ page import="javax.mail.Transport"%>
<%@ page import="javax.mail.internet.InternetAddress"%>
<%@ page import="javax.mail.internet.MimeMessage"%>
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
<% Template tmpl = new Template(request, config.getServletContext(), response);
  Logger logger = Logger.getLogger("ru.org.linux");
%>
<%= tmpl.getHead() %>
<title>Регистрация пользователя</title>
<jsp:include page="WEB-INF/jsp/header.jsp"/>
<%
  response.addHeader("Cache-Control", "no-store, no-cache, must-revalidate");

   if (request.getParameter("mode")==null) {
     session.setAttribute("register-visited", Boolean.TRUE);

%>
<H1>Регистрация</H1>
Если вы уже регистрировались на нашем сайте и забыли пароль - вам
<a href="lostpwd.jsp">сюда</a>.

<form method=POST action="register.jsp">
<input type=hidden name=mode value=new>
<b>Login:</b>
<input type=text name=nick size=40><br>
Полное имя:
<input type=text name=name size=40><br>
<b>Пароль:</b>
<input type=password name=password size=20><br>
<b>Повторите Пароль:</b>
<input type=password name=password2 size=20><br>
URL (не забудьте добавить <b>http://</b>): <br>
<input type=text name=url size="50"><br>
<b>E-mail</b> (ваш email не будет публиковаться на сайте):<br>
<input type=text name=email size="50"><br>
Город (просьба писать русскими буквами без сокращений, например: <b>Москва</b>,
<b>Нижний Новгород</b>, <b>Троицк (Московская область)</b>):
<input type=text name=town size=50><br>
Дополнительная информация:<br>
<textarea name=info cols=50 rows=5></textarea><br>
<p><img src="/jcaptcha.jsp">
<input type='text' name='j_captcha_response' value=''>

<input type=submit value="Register/Зарегистрироваться">
</form>
<% } else if (("new".equals(request.getParameter("mode")) || "update".equals(request.getParameter("mode")))
    && "POST".equals(request.getMethod())) {
  Connection db = null;
  try {
    boolean changeMode = "update".equals(request.getParameter("mode"));

    String nick = request.getParameter("nick");

    if (!StringUtil.checkLoginName(nick))
      throw new BadInputException("некорректное имя пользователя");

    if (nick.length() > 40)
      throw new BadInputException("слишком длинное имя пользователя");

    if (!changeMode) {
      CaptchaSingleton.checkCaptcha(session, request);

      if (session.getAttribute("register-visited") == null) {
        logger.info("Flood protection (not visited register.jsp) " + request.getRemoteAddr());
        throw new BadInputException("сбой");
      }
    }

    String town = request.getParameter("town");
    String info = request.getParameter("info");
    String name = request.getParameter("name");
    String url = request.getParameter("url");
    String password = request.getParameter("password");
    String password2 = request.getParameter("password2");
    String email = request.getParameter("email");

    if (password != null && password.length() == 0) {
      password = null;
    }

    if (password2 != null && password2.length() == 0) {
      password2 = null;
    }

    if (email == null || "".equals(email))
      throw new BadInputException("Не указан e-mail");

    InternetAddress mail = new InternetAddress(email);
    if (url != null && "".equals(url)) url = null;

    if (!changeMode) {
      if (password == null) {
        throw new BadInputException("пароль не может быть пустым");
      }

      if (password2 == null || !password.equals(password2)) {
        throw new BadInputException("введенные пароли не совпадают");
      }
    } else {
      if (password2 != null && password != null && !password.equals(password2)) {
        throw new BadInputException("введенные пароли не совпадают");
      }
    }

    if (name != null && "".equals(name)) name = null;
    if (town != null && "".equals(town)) town = null;
    if (info != null && "".equals(info)) info = null;

    if (name != null) name = HTMLFormatter.htmlSpecialChars(name);
    if (town != null) town = HTMLFormatter.htmlSpecialChars(town);
    if (info != null) info = HTMLFormatter.htmlSpecialChars(info);

    db = LorDataSource.getConnection();
    db.setAutoCommit(false);

    IPBlockInfo.checkBlockIP(db, request.getRemoteAddr());

    int userid;

    if (changeMode) {
      User user = User.getUser(db, nick);
      userid = user.getId();
      user.checkPassword(request.getParameter("oldpass"));
      user.checkAnonymous();

      PreparedStatement ist = db.prepareStatement("UPDATE users SET  name=?, passwd=?, url=?, email=?, town=? WHERE id=" + userid);
      ist.setString(1, name);
      if (password == null)
        ist.setString(2, request.getParameter("oldpass"));
      else
        ist.setString(2, password);

      if (url != null)
        ist.setString(3, URLUtil.fixURL(url));
      else
        ist.setString(3, null);

      ist.setString(4, mail.getAddress());

      ist.setString(5, town);

      ist.executeUpdate();

      ist.close();

      if (info != null) {
        try {
          tmpl.getObjectConfig().getStorage().updateMessage("userinfo", String.valueOf(userid), info);
        } catch (StorageNotFoundException e) {
          tmpl.getObjectConfig().getStorage().writeMessage("userinfo", String.valueOf(userid), info);
        }
      }
    } else {
      PreparedStatement pst = db.prepareStatement("SELECT count(*) as c FROM users WHERE nick=?");
      pst.setString(1, nick);
      ResultSet rs = pst.executeQuery();
      rs.next();
      if (rs.getInt("c") != 0) {
        throw new BadInputException("пользователь " + nick + " уже существует");
      }
      rs.close();
      pst.close();

      PreparedStatement pst2 = db.prepareStatement("SELECT count(*) as c FROM users WHERE email=?");
      pst2.setString(1, mail.getAddress());
      rs = pst2.executeQuery();
      rs.next();
      if (rs.getInt("c") != 0) {
        throw new BadInputException("пользователь с таким e-mail уже зарегистрирован");
      }
      rs.close();
      pst2.close();

      Statement st = db.createStatement();
      rs = st.executeQuery("select nextval('s_uid') as userid");
      rs.next();
      userid = rs.getInt("userid");
      rs.close();
      st.close();

      PreparedStatement ist = db.prepareStatement("INSERT INTO users (id, name, nick, passwd, url, email, town, score, max_score,regdate) VALUES (?,?,?,?,?,?,?,50,50,current_timestamp)");
      ist.setInt(1, userid);
      ist.setString(2, name);
      ist.setString(3, nick);
      ist.setString(4, password);
      if (url != null) {
        ist.setString(5, URLUtil.fixURL(url));
      } else {
        ist.setString(5, null);
      }

      ist.setString(6, mail.getAddress());

      ist.setString(7, town);
      ist.executeUpdate();
      ist.close();

      String logmessage = "Зарегистрирован пользователь " + nick + " (id=" + userid + ") " + LorHttpUtils.getRequestIP(request);
      logger.info(logmessage);

      if (info != null) {
        tmpl.getObjectConfig().getStorage().writeMessage("userinfo", String.valueOf(userid), info);
      }

      StringBuffer text = new StringBuffer();

      text.append("Здравствуйте!\n\n");
      text.append("\tВ форуме по адресу http://www.linux.org.ru/ появилась регистрационная запись,\n");
      text.append("в которой был указал ваш электронный адрес (e-mail).\n\n");
      text.append("При заполнении регистрационной формы было указано следующее имя пользователя: '");
      text.append(nick);
      text.append("'\n\n");
      text.append("Если вы не понимаете, о чем идет речь - просто проигнорируйте это сообщение!\n\n");
      text.append("Если же именно вы решили зарегистрироваться в форуме по адресу http://www.linux.org.ru/,\n");
      text.append("то вам следует подтвердить свою регистрацию и тем самым активировать вашу учетную запись.\n\n");

      String regcode = StringUtil.md5hash(tmpl.getSecret() + ":" + nick + ":" + password);

      text.append("Для активации перейдите по ссылке http://www.linux.org.ru/activate.jsp\n\n");
      text.append("Код активации: ").append(regcode).append("\n\n");
      text.append("Благодарим за регистрацию!\n");

      Properties props = new Properties();
      props.put("mail.smtp.host", "localhost");
      Session mailSession = Session.getDefaultInstance(props, null);

      MimeMessage emailMessage = new MimeMessage(mailSession);
      emailMessage.setFrom(new InternetAddress("no-reply@linux.org.ru"));

      emailMessage.addRecipient(MimeMessage.RecipientType.TO, new InternetAddress(email));
      emailMessage.setSubject("Linux.org.ru registration");
      emailMessage.setSentDate(new Date());
      emailMessage.setText(text.toString(), "UTF-8");

      Transport.send(emailMessage);
    }

    db.commit();

    if (changeMode)
      out.print("Обновление регистрации прошло успешно");
    else
      out.print("Добавление пользователя прошло успешно");
  } finally {
    if (db != null) db.close();
  }
} else if ("change".equals(request.getParameter("mode"))) {
%>
  <table class=nav><tr>
    <td align=left valign=middle>
      Изменение регистрации
    </td>

    <td align=right valign=middle>
      [<a style="text-decoration: none" href="addphoto.jsp">Добавить фотографию</a>]
      [<a style="text-decoration: none" href="rules.jsp">Правила форума</a>]
     </td>
    </tr>
 </table>
<h1>Изменение регистрации</h1>
<%
  if (!Template.isSessionAuthorized(session)) {
    throw new IllegalAccessException("Not authorized");
  }

  Connection db = null;
  try {
    String nick = (String) session.getAttribute("nick");

    db = LorDataSource.getConnection();
    db.setAutoCommit(false);

    User user = User.getUser(db, nick);
    user.checkAnonymous();

    Statement st = db.createStatement();
    ResultSet rs = st.executeQuery("SELECT * FROM users WHERE id=" + user.getId());
    rs.next();
%>

<form method=POST action="register.jsp">
<input type=hidden name=mode value=update>
Nick: <%= nick %><br>
<input type=hidden name=nick value="<%= nick %>"><br>
Полное имя:
<input type=text name="name" size="40" value="<%= rs.getString("name") %>"><br>
Пароль:
<input type=password name="oldpass" size="20"><br>
Новый пароль:
<input type=password name="password" size="20"> (не заполняйте если не хотите менять)<br>
Повторите новый пароль:
<input type=password name="password2" size="20"><br>
URL:
<input type=text name="url" size="50" value="<%
	if (rs.getString("url")!=null) out.print(rs.getString("url"));
%>"><br>
(не забудьте добавить <b>http://</b>)<br>
Email:
<input type=text name="email" size="50" value="<%= rs.getString("email") %>"><br>
Город (просьба писать русскими буквами без сокращений, например: <b>Москва</b>,
<b>Нижний Новгород</b>, <b>Троицк (Московская область)</b>):
<input type=text name="town" size="50" value="<%= rs.getString("town") %>"><br>
Дополнительная информация:<br>
<textarea name=info cols=50 rows=5>
<%= tmpl.getObjectConfig().getStorage().readMessageDefault("userinfo", String.valueOf(user.getId()), "") %>
</textarea>
<br>
<input type=submit value="Update/Обновить">
</form>
<%
    rs.close();
    st.close();
  } finally {
    if (db!=null) db.close();
  }
%>

<% } %>
<jsp:include page="WEB-INF/jsp/footer.jsp"/>
