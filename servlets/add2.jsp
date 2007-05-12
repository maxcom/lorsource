<%@ page contentType="text/html; charset=koi8-r"%>
<%@ page  import="java.io.File,java.sql.Connection,java.sql.PreparedStatement,java.sql.ResultSet,java.sql.Statement, java.util.Date, java.util.Properties, java.util.Random, javax.mail.Session" errorPage="error.jsp"%>
<%@ page import="javax.mail.internet.InternetAddress"%>
<%@ page import="javax.mail.internet.MimeMessage"%>
<%@ page import="javax.servlet.http.Cookie"%>
<%@ page import="javax.servlet.http.HttpServletResponse"%>
<%@ page import="ru.org.linux.site.*"%>
<%@ page import="ru.org.linux.util.HTMLFormatter"%>
<%@ page import="ru.org.linux.util.URLUtil"%>
<%@ page import="ru.org.linux.util.UtilBadHTMLException"%>
<%@ page import="ru.org.linux.util.LorHttpUtils"%>
<% Template tmpl = new Template(request, config, response);%>
<%= tmpl.head() %>
<%
  String returnUrl = request.getParameter("return");
  if (returnUrl == null) {
    returnUrl = "";
  }

  Connection db = null;
  try {
    String title = request.getParameter("title");
    if (title == null) {
      title = "";
    }
    title = HTMLFormatter.htmlSpecialChars(title);
    if ("".equals(title.trim())) {
      throw new BadInputException("заголовок сообщения не может быть пустым");
    }

    String linktext = request.getParameter("linktext");
    if (linktext != null && "".equals(linktext)) {
      linktext = null;
    }
    if (linktext != null) {
      linktext = HTMLFormatter.htmlSpecialChars(linktext);
    }

    String msg = request.getParameter("msg");

    boolean userhtml = tmpl.getParameters().getBoolean("texttype");
    boolean autourl = tmpl.getParameters().getBoolean("autourl");

    String url = request.getParameter("url");
    if (url != null && "".equals(url)) {
      url = null;
    }

    if (!session.getId().equals(request.getParameter("session"))) {
      tmpl.getLogger().notice("add2", "Flood protection (session variable differs) " + request.getRemoteAddr());
      throw new BadInputException("сбой добавления");
    }

    if (session.getAttribute("add-visited") == null) {
      tmpl.getLogger().notice("add2", "Flood protection (no session) " + request.getRemoteAddr());
      throw new BadInputException("сбой добавления");
    }

    if (!"POST".equals(request.getMethod())) {
      tmpl.getLogger().notice("add2", "Flood protection (not POST method) " + request.getRemoteAddr());
      throw new BadInputException("сбой добавления");
    }

    if (!Template.isSessionAuthorized(session)) {
      CaptchaSingleton.checkCaptcha(session, request, tmpl.getLogger());
    }

    // checks is over
    db = tmpl.getConnection("add2");
    db.setAutoCommit(false);

    IPBlockInfo.checkBlockIP(db, request.getRemoteAddr());

    User user;

    if (session == null || session.getAttribute("login") == null || !((Boolean) session.getAttribute("login")).booleanValue())     {
      if (request.getParameter("nick") == null) {
        throw new BadInputException("Вы уже вышли из системы");
      }
      user = new User(db, request.getParameter("nick"));
      user.checkPassword(request.getParameter("password"));
    } else {
      user = new User(db, (String) session.getAttribute("nick"));
      user.checkBlocked();
    }

    if (user.isAnonymous()) {
      if (msg.length() > 4096) {
        throw new BadInputException("Слишком большое сообщение");
      }
    } else {
      if (msg.length() > 8192) {
        throw new BadInputException("Слишком большое сообщение");
      }
    }

    Cookie nickCookie = new Cookie("NickCookie", user.getNick());
    nickCookie.setMaxAge(60 * 60 * 24 * 31 * 24);
    nickCookie.setPath("/");
    response.addCookie(nickCookie);

    int guid = tmpl.getParameters().getInt("guid");

    Statement st = db.createStatement();

    Group group = new Group(db, guid);

    if (!group.isTopicPostingAllowed(user)) {
      throw new AccessViolationException("Не достаточно прав для постинга тем в эту группу");
    }

    String mode = tmpl.getParameters().getString("mode");

    if ("pre".equals(mode) && !group.isPreformatAllowed()) {
      throw new AccessViolationException("В группу нельзя добавлять преформатированные сообщения");
    }
    if (("ntobr".equals(mode) || "tex".equals(mode) || "quot".equals(mode)) && group.isLineOnly()) {
      throw new AccessViolationException("В группу нельзя добавлять сообщения с переносом строк");
    }
    if (userhtml && group.isLineOnly()) {
      throw new AccessViolationException("В группу нельзя добавлять сообщения с переносом строк");
    }

    if (!group.isImagePostAllowed()) {
      if (url != null) {
        if (linktext == null) {
          throw new BadInputException("указан URL без текста");
        }
        url = URLUtil.fixURL(url);
      }
    } else {
      url = "gallery/" + new File(url).getName();
      linktext = "gallery/" + new File(linktext).getName();
    }

    int maxlength = 80; // TODO: remove this hack
    if (group.getSectionId() == 1) {
      maxlength = 40;
    }
    HTMLFormatter form = new HTMLFormatter(msg);
    form.setMaxLength(maxlength);
    if ("pre".equals(mode)) {
      form.enablePreformatMode();
    }
    if (autourl) {
      form.enableUrlHighLightMode();
    }
    if ("ntobr".equals(mode)) {
      form.enableNewLineMode();
    }
    if ("tex".equals(mode)) {
      form.enableTexNewLineMode();
    }

    if (!userhtml) {
      form.enablePlainTextMode();
    } else {
      form.enableCheckHTML();
    }

    try {
      msg = form.process();
    } catch (UtilBadHTMLException e) {
      throw new BadInputException(e);
    }

    Random random = new Random();

    ResultSet rs = null;

    if (DupeProtector.getInstance().check(request.getRemoteAddr())) {
      // allocation MSGID
      rs = st.executeQuery("select nextval('s_msgid') as msgid");
      rs.next();
      int msgid = rs.getInt("msgid");

      PreparedStatement pst = db.prepareStatement("INSERT INTO topics (postip, groupid, userid, title, url, moderate, postdate, id, linktext, deleted) VALUES ('" + request.getRemoteAddr() + "',?, ?, ?, ?, 'f', CURRENT_TIMESTAMP, ?, ?, 'f')");
//                pst.setString(1, request.getRemoteAddr());
      pst.setInt(1, guid);
      pst.setInt(2, user.getId());
      pst.setString(3, title);
      pst.setString(4, url);
      pst.setInt(5, msgid);
      pst.setString(6, linktext);
      pst.executeUpdate();
      pst.close();

      // insert message text
      PreparedStatement pstMsgbase = db.prepareStatement("INSERT INTO msgbase (id, message) values (?,?)");
      pstMsgbase.setLong(1, msgid);
      pstMsgbase.setString(2, msg);
      pstMsgbase.executeUpdate();
      pstMsgbase.close();

      // write to storage
//      tmpl.getObjectConfig().getStorage().writeMessage("msgbase", String.valueOf(msgid), msg);

      if (!group.isModerated()) {
        response.setHeader("Location", tmpl.getRedirectUrl() + returnUrl + "&nocache=" + random.nextInt());
        response.setStatus(HttpServletResponse.SC_MOVED_TEMPORARILY);
      } else {
        Statement myst = db.createStatement();
        ResultSet myrs = st.executeQuery("SELECT email FROM users WHERE id=" + user.getId());
        myrs.next();

        StringBuffer mail = new StringBuffer("User: " + user.getNick() + " (" + myrs.getString("email") + ")\n");
        myrs.close();

        mail.append("Title: " + title + '\n');

        myrs = myst.executeQuery("SELECT groups.title as gname, sections.name as pname FROM groups, sections WHERE groups.id=" + guid + " AND sections.id=groups.section");
        myrs.next();
        mail.append("Group: " + myrs.getString("gname") + " (" + myrs.getString("pname") + ")\n");
        myst.close();

        mail.append('\n');
        mail.append(msg);
        mail.append("\n\n");
        if (url != null) {
          mail.append("Url: ");
          mail.append(url);
          mail.append('\n');
        }

        Properties props = new Properties();
        props.put("mail.smtp.host", "localhost");
        Session mailSession = Session.getDefaultInstance(props, null);

        MimeMessage email = new MimeMessage(mailSession);
        email.setFrom(new InternetAddress("no-reply@linux.org.ru"));

        email.addRecipient(MimeMessage.RecipientType.TO, new InternetAddress("newsmaster@linux.org.ru"));
        email.setSubject("New Post: " + title);
        email.setSentDate(new Date());
        email.setText(mail.toString());

//			Transport.send(email);
      }

      String logmessage = "Написана тема " + msgid + " " + LorHttpUtils.getRequestIP(request);
      tmpl.getLogger().notice("add2", logmessage);
    } else {
      response.setHeader("Location", tmpl.getRedirectUrl() + returnUrl + "&nocache=" + random.nextInt());
      response.setStatus(HttpServletResponse.SC_MOVED_TEMPORARILY);
    }

    db.commit();

    if (rs != null) {
      rs.close();
    }
    st.close();
%>
<title>Добавление сообщения прошло успешно</title>
<%= tmpl.DocumentHeader() %>
<% if (group.isModerated()) { %>
Вы поместили сообщение в защищенный раздел. Подождите, пока ваше сообщение проверят. Послано почтовое уведомление администраторам сайта.
<% } %>
<p>Сообщение помещено успешно
<% if (group.isModerated()) { %>
<p>Пожалуйста, проверьте свое сообщение и работоспособность ссылок в нем в <a href="view-all.jsp?nocache=<%= random.nextInt()%>">буфере неподтвержденных сообщений</a>
<% } %>

<p><a href="<%= tmpl.getRedirectUrl()+returnUrl %>">Возврат</a>

<p><b>Пожалуйста, не нажимайте кнопку "ReLoad" вашего броузера на этой страничке и не возвращайтесь на нее по средством кнопки Back</b>

<%
  } finally {
    if (db!=null) {
      db.close();
    }
  }
%>
<%=	tmpl.DocumentFooter() %>
