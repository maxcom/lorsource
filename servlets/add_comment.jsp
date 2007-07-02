<%@ page contentType="text/html; charset=koi8-r"%>
<%@ page import="java.sql.*,java.util.Random,javax.servlet.http.Cookie,javax.servlet.http.HttpServletResponse,ru.org.linux.site.*,ru.org.linux.util.HTMLFormatter" errorPage="error.jsp"%>
<%@ page import="ru.org.linux.util.UtilBadHTMLException"%>
<%@ page import="ru.org.linux.util.UtilBadURLException" %>
<% Template tmpl = new Template(request, config, response);%>
<%
	int topic = tmpl.getParameters().getInt("topic");
	boolean showform = request.getParameter("msg")==null;

  if (!"POST".equals(request.getMethod()))
    showform=true;
%>
<%= tmpl.head() %>

<%
  Connection db = null;
  try {
%>

<% Exception error = null;
  if (!showform) { // add2

    String returnUrl = request.getParameter("return");
    String msg = request.getParameter("msg");
    int replyto = 0;

    if (request.getParameter("replyto") != null) {
      replyto = tmpl.getParameters().getInt("replyto");
    }

    if (!session.getId().equals(request.getParameter("session"))) {
      tmpl.getLogger().notice("add2", "Flood protection (session variable differs: "+ session.getId()+ ") " + request.getRemoteAddr());
      throw new BadInputException("сбой добавления");
    }

    try {
      if (returnUrl == null) {
        returnUrl = "";
      }

      String title = request.getParameter("title");
      if (title == null) {
        title = "";
      }

      title = HTMLFormatter.htmlSpecialChars(title);
      if ("".equals(title)) {
        throw new BadInputException("заголовок сообщения не может быть пустым");
      }

      if ("".equals(msg)) {
        throw new BadInputException("комментарий не может быть пустым");
      }

      boolean autourl = tmpl.getParameters().getBoolean("autourl");

      if (!Template.isSessionAuthorized(session)) {
        CaptchaSingleton.checkCaptcha(session, request, tmpl.getLogger());
      }

      // prechecks is over

      db = tmpl.getConnection("add_comment");
      db.setAutoCommit(false);
      IPBlockInfo.checkBlockIP(db, request.getRemoteAddr());

      User user;

      if (!Template.isSessionAuthorized(session)) {
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

      if (replyto != 0) {
        Comment reply = new Comment(db, replyto);
        if (reply.isDeleted()) {
          throw new AccessViolationException("Комментарий был удален");
        }

        if (reply.getTopic() != topic) {
          throw new AccessViolationException("Некорректная тема?!");
        }
      }

      Statement st = db.createStatement();
      ResultSet rs = null;

      rs = st.executeQuery("SELECT sections.comment, deleted, postscore FROM groups, sections, topics WHERE groups.id=topics.groupid AND topics.id=" + topic + " AND groups.section=sections.id");
      if (!rs.next()) {
        rs = st.executeQuery("SELECT 't' as comment, 'f' as deleted, 0 as postscore FROM votenames WHERE topic=" + topic);
        if (!rs.next()) {
          throw new MessageNotFoundException(topic);
        }
      }

      int postscore = rs.getInt("postscore");

      if (postscore != 0) {
        if (user.getScore() < postscore || user.isAnonymous()) {
          throw new AccessViolationException("Вы не можете добавлять комментарии в эту тему");
        }
      }

      String mode = tmpl.getParameters().getString("mode");

      if (rs.getBoolean("deleted")) {
        throw new AccessViolationException("Нельзя добавлять комментарии к удаленному сообщению");
      }
      if (!rs.getBoolean("comment")) {
        throw new AccessViolationException("В эту группу нельзя добавлять комментарии");
      }

      int maxlength = 80; // TODO: remove this hack
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
      if ("quot".equals(mode)) {
        form.enableTexNewLineMode();
        form.enableQuoting();
      }

      form.enablePlainTextMode();

      try {
        msg = form.process();
      } catch (UtilBadHTMLException e) {
        throw new BadInputException(e);
      }

      rs.close();

      // section EXPIRE
      rs = st.executeQuery("SELECT topics.postdate<(CURRENT_TIMESTAMP-sections.expire) as expired FROM topics, groups, sections  WHERE sections.id=groups.section AND topics.id=" + topic + " AND topics.groupid=groups.id");
      if (rs.next()) {
        if (rs.getBoolean("expired")) {
          throw new AccessViolationException("группа уже устарела");
        }
      }
      rs.close();

      DupeProtector.getInstance().checkDuplication(request.getRemoteAddr());

      // allocation MSGID
      rs = st.executeQuery("select nextval('s_msgid') as msgid");
      rs.next();
      int msgid = rs.getInt("msgid");

      // insert headers
      PreparedStatement pst = db.prepareStatement("INSERT INTO comments (id, userid, title, postdate, replyto, deleted, topic, postip) VALUES (?, ?, ?, CURRENT_TIMESTAMP, ?, 'f', ?, '" + request.getRemoteAddr() + "')");
      pst.setInt(1, msgid);
      pst.setInt(2, user.getId());
      pst.setString(3, title);
      pst.setInt(5, topic);

      if (replyto != 0) {
        pst.setInt(4, replyto);
      } else {
        pst.setNull(4, Types.INTEGER);
      }

      //pst.setString(6, request.getRemoteAddr());
      pst.executeUpdate();
      pst.close();

      // insert message text
      PreparedStatement pstMsgbase = db.prepareStatement("INSERT INTO msgbase (id, message) values (?,?)");
      pstMsgbase.setLong(1, msgid);
      pstMsgbase.setString(2, msg);
      pstMsgbase.executeUpdate();
      pstMsgbase.close();

      // write to storage
//        tmpl.getObjectConfig().getStorage().writeMessage("msgbase", String.valueOf(msgid), msg);
      String logmessage = "Написан комментарий " + msgid + " ip:" + request.getRemoteAddr();
      if (request.getHeader("X-Forwarded-For") != null) {
        logmessage = logmessage + " XFF:" + request.getHeader(("X-Forwarded-For"));
      }

      tmpl.getLogger().notice("add2", logmessage);

      Random random = new Random();

      response.setHeader("Location", tmpl.getRedirectUrl() + returnUrl + "&nocache=" + random.nextInt());
      response.setStatus(HttpServletResponse.SC_MOVED_TEMPORARILY);

      db.commit();
      rs.close();
      st.close();

%>
<title>Добавление сообщения прошло успешно</title>
<%= tmpl.DocumentHeader() %>
<p>Сообщение помещено успешно

<p><a href="<%= tmpl.getRedirectUrl()+returnUrl %>">Возврат</a>

<p><b>Пожалуйста, не нажимайте кнопку "ReLoad" вашего броузера на этой страничке и не возвращайтесь на нее по средством кнопки Back</b>
<%
} catch (UserErrorException e) {
	error=e;
	showform=true;
	if (db!=null) {
		db.rollback();
		db.setAutoCommit(true);
	}
} catch (UserNotFoundException e) {
	error=e;
	showform=true;
	if (db!=null) {
		db.rollback();
		db.setAutoCommit(true);
	}
} catch (UtilBadURLException e) {
	error=e;
	showform=true;
	if (db!=null) {
		db.rollback();
		db.setAutoCommit(true);
	}
}
%>

<% }

if (showform) { // show form
	if (db==null) db = tmpl.getConnection("add_comment");
	Statement st=db.createStatement();
	ResultSet grinfo=st.executeQuery("SELECT postdate<(CURRENT_TIMESTAMP-sections.expire) as expired, postscore FROM topics, sections, groups WHERE topics.id="+topic+" AND groups.id = topics.groupid AND groups.section=sections.id");
	if (!grinfo.next()) {
		grinfo.close();
		grinfo=st.executeQuery("SELECT id, 0 as postscore FROM votenames WHERE votenames.topic="+topic);
		if (!grinfo.next())
			throw new BadGroupException("Тема "+topic+" не существует");
	} else {
		if (grinfo.getBoolean("expired"))
   			throw new AccessViolationException("нельзя добавлять в устаревшие темы");
	}
        int postscore = grinfo.getInt("postscore");
	grinfo.close();
%>

<title>Добавить сообщение</title>
<%= tmpl.DocumentHeader() %>
<% if (error==null) { %>
<h1>Добавить комментарий</h1>
<% } else { out.println("<h1>Ошибка: "+error.getMessage()+"</h1>"); } %>

<% if (tmpl.getProf().getBooleanProperty("showinfo") && !tmpl.isSessionAuthorized(session)) { %>
<font size=2>Чтобы просто поместить сообщение, используйте login `anonymous',
без пароля. Если вы собираетесь активно участвовать в форуме,
помещать новости на главную страницу,
<a href="register.jsp">зарегистрируйтесь</a></font>.
<p>

<% } %>
<font size=2><strong>Внимание!</strong> Перед написанием комментария ознакомьтесь с
<a href="rules.jsp">правилами</a> сайта.</font><p>

<%
  out.print(Message.getPostScoreInfo(postscore));
%>

<form method=POST action="add_comment.jsp">
  <input type="hidden" name="session" value="<%= HTMLFormatter.htmlSpecialChars(session.getId()) %>">
<% if (!tmpl.isSessionAuthorized(session)) { %>
Имя:
<input type=text name=nick value="<%= "anonymous" %>" size=40><br>
Пароль:
<input type=password name=password size=40><br>
<% } %>
<input type=hidden name=topic value="<%= topic %>">

<% if (request.getParameter("return")!=null) { %>
<input type=hidden name=return value="<%= HTMLFormatter.htmlSpecialChars(request.getParameter("return")) %>">
<% } %>

<% if (request.getParameter("replyto")!=null) {
        int replyto=Integer.parseInt(request.getParameter("replyto"));
%>
<input type=hidden name=replyto value="<%= replyto %>">
<% // show message here
  Comment comment = new Comment(db, replyto);
//        ResultSet rs=st.executeQuery("SELECT postdate, topic, nick, score, max_score, comments.id as msgid, comments.title, photo, 'f', deleted, 'f' as expired, replyto, message FROM comments, users, msgbase WHERE comments.id=msgbase.id AND comments.id="+replyto+" AND comments.userid=users.id AND comments.topic="+topic);
//           if (!rs.next()) throw new MessageNotFoundException(replyto);
  if (comment.isDeleted()) throw new MessageNotFoundException(replyto);
  String title=comment.getTitle();
  if (!title.startsWith("Re:")) title="Re: "+title;

  out.print("<div class=messages>");
  out.print(comment.printMessage(tmpl, db, false, true, "", ""));
  out.print("</div>");

  if (request.getParameter("title")!=null) title=request.getParameter("title");
%>
Заглавие:
<input type=text name=title size=40 value="<%= title %>"><br>
<% } else if (request.getParameter("title")==null) {%>
Заглавие:
<input type=text name=title size=40><br>
<% } else { %>
Заглавие:
<input type=text name=title size=40 value="<%= HTMLFormatter.htmlSpecialChars(request.getParameter("title")) %>"><br>
<% } %>

Сообщение:<br>
<font size=2>(В режиме <i>Tex paragraphs</i> игнорируются переносы строк.<br> Пустая строка (два раза Enter) начинает новый абзац.<br> Знак '&gt;' в начале абзаца выделяет абзац курсивом цитирования)</font><br>
<textarea name="msg" cols="70" rows="20" onkeypress="return ctrl_enter(event, this.form);"><%= request.getParameter("msg")==null?"":HTMLFormatter.htmlSpecialChars(request.getParameter("msg")) %></textarea><br>

<select name=mode>
<option value=quot>TeX paragraphs w/quoting
<option value=tex>TeX paragraphs w/o quoting
<option value=ntobr>User line break
<option value=html>Ignore line breaks
<option value=pre>Preformatted text
</select>

<select name=autourl>
<option value=1>Auto URL
<option value=0>No Auto URL
</select>

<input type=hidden value=0 name=texttype>

<br>

<%
  out.print(Message.getPostScoreInfo(postscore));
%>

<br>
<%
  if (!Template.isSessionAuthorized(session)) {
    out.print("<p><img src=\"/jcaptcha.jsp\"><input type='text' name='j_captcha_response' value=''>");
  }
%>
<input type=submit value="Post/Поместить">


</form>
<%
   st.close();
   }

} finally {
  if (db!=null) db.close();
}
%>
<%=	tmpl.DocumentFooter() %>
