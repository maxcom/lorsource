<%@ page contentType="text/html; charset=koi8-r"%>
<%@ page contentType="text/html; charset=koi8-r" import="java.sql.Connection,java.util.Random" errorPage="/error.jsp"%>
<%@ page import="java.util.logging.Logger"%>
<%@ page import="ru.org.linux.site.*"%>
<%@ page import="ru.org.linux.util.HTMLFormatter"%>
<%@ page import="ru.org.linux.util.UtilBadURLException" %>
<% Template tmpl = new Template(request, config, response);%>
<%= tmpl.head() %>
<%
  Connection db = null;
  try {

    boolean showform = request.getMethod().equals("GET");
    Exception error = null;

    if (request.getMethod().equals("POST")) {
      try {
        String returnUrl = request.getParameter("return");
        if (returnUrl == null) {
          returnUrl = "";
        }

        if (!session.getId().equals(request.getParameter("session"))) {
          Logger.getLogger("ru.org.linux").info("Flood protection (session variable differs) " + request.getRemoteAddr());
          throw new BadInputException("сбой добавления");
        }

        if (!Template.isSessionAuthorized(session)) {
          CaptchaSingleton.checkCaptcha(session, request);
        }

        db = tmpl.getConnection("add");
        db.setAutoCommit(false);

        IPBlockInfo.checkBlockIP(db, request.getRemoteAddr());

        int guid = tmpl.getParameters().getInt("group");

        Group group = new Group(db, guid);

        Message.addTopic(db, tmpl, session, request, group);

        Random random = new Random();

        if (!group.isModerated()) {
          response.setHeader("Location", tmpl.getMainUrl() + returnUrl + "&nocache=" + random.nextInt());
          response.setStatus(HttpServletResponse.SC_MOVED_TEMPORARILY);
        }
%>
<title>Добавление сообщения прошло успешно</title>
<%= tmpl.DocumentHeader() %>
<% if (group.isModerated()) { %>
Вы поместили сообщение в защищенный раздел. Подождите, пока ваше сообщение проверят.
<% } %>
<p>Сообщение помещено успешно
<% if (group.isModerated()) { %>
<p>Пожалуйста, проверьте свое сообщение и работоспособность ссылок в нем в <a href="view-all.jsp?nocache=<%= random.nextInt()%>">буфере неподтвержденных сообщений</a>
<% } %>

<p><a href="<%= tmpl.getMainUrl()+returnUrl %>">Возврат</a>

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
  }

  if (showform) {
      int groupId = tmpl.getParameters().getInt("group");

      db = tmpl.getConnection("add");
      Group group = new Group(db, groupId);

      User currentUser = User.getCurrentUser(db, session);

      if (!group.isTopicPostingAllowed(currentUser)) {
        throw new AccessViolationException("Не достаточно прав для постинга тем в эту группу");
      }
%>

<title>Добавить сообщение</title>
<%= tmpl.DocumentHeader() %>
<%	int section=group.getSectionId();
	if (request.getParameter("noinfo")==null || !"1".equals(request.getParameter("noinfo")))
		out.print(tmpl.getObjectConfig().getStorage().readMessageDefault("addportal", String.valueOf(section), ""));
%>
<% if (error==null) { %>
<h1>Добавить</h1>
<% } else { out.println("<h1>Ошибка: "+error.getMessage()+"</h1>"); } %>
<% if (tmpl.getProf().getBoolean("showinfo") && !tmpl.isSessionAuthorized(session)) { %>
<font size=2>Чтобы просто поместить сообщение, используйте login `anonymous',
без пароля. Если вы собираетесь активно участвовать в форуме,
помещать новости на главную страницу,
<a href="register.jsp">зарегистрируйтесь</a></font>.
<p>

<% if (!group.isLineOnly()) { %>
<font size=2>В HTML режиме можно использовать теги &lt;a&gt; &lt;p&gt; &lt;li&gt; </font>
<% } %>
<% } %>

<% if (group.isImagePostAllowed()) { %>
<p>
  Технические требования к изображению:
  <ul>
    <li>Ширина x Высота: от 400x400 до 2048x2048 пикселей</li>
    <li>Тип: jpeg, gif, png</li>
    <li>Размер не более 300 Kb</li>
  </ul>
</p>
<%   } %>
<form method=POST action="add.jsp" <%= group.isImagePostAllowed()?"enctype=\"multipart/form-data\"":"" %> >
  <input type="hidden" name="session" value="<%= HTMLFormatter.htmlSpecialChars(session.getId()) %>">
<%  if (request.getParameter("noinfo")!=null) {
  %>
  <input type="hidden" name="noinfo" value="<%= request.getParameter("noinfo") %>">
 <% }
%>
<% if (session==null || session.getValue("login")==null || !((Boolean) session.getValue("login")).booleanValue()) { %>
Имя:
<input type=text name=nick value="<%= request.getParameter("nick")==null?"anonymous":HTMLFormatter.htmlSpecialChars(request.getParameter("nick")) %>" size=40><br>
Пароль:
<input type=password name=password size=40><br>
<% } %>
<input type=hidden name=group value="<%= groupId %>">

<% if (request.getParameter("return")!=null) { %>
<input type=hidden name=return value="<%= HTMLFormatter.htmlSpecialChars(request.getParameter("return")) %>">
<% } %>

Заглавие:
<input type=text name=title size=40 value="<%= request.getParameter("title")==null?"":HTMLFormatter.htmlSpecialChars(request.getParameter("title")) %>" ><br>

  <% if (group.isImagePostAllowed()) { %>
  Изображение:
  <input type="file" name="image"><br>
  <% } %>

<% if (group.isLinksAllowed() && group.isLinksUp()) { %>
<input type=hidden name=linktext value="<%= group.getDefaultLinkText() %> ">
Ссылка (не забудьте <b>http://</b>)
<input type=text name=url size=70><br>
<% } %>

Сообщение:<br>
<font size=2>(В режиме <i>Tex paragraphs</i> игнорируются переносы строк.<br> Пустая строка (два раза Enter) начинает новый абзац)</font><br>
<textarea name=msg cols=70 rows=20><%
    if (request.getParameter("msg")!=null) {
      out.print(HTMLFormatter.htmlSpecialChars(request.getParameter("msg")));
    }
  %></textarea><br>

<% if (group.isLinksAllowed() && !group.isLinksUp()) { %>
Текст ссылки:
<input type=text name=linktext size=60 value="<%= request.getParameter("linktext")==null?group.getDefaultLinkText():HTMLFormatter.htmlSpecialChars(request.getParameter("linktext")) %>"><br>
Ссылка (не забудьте <b>http://</b>)
<input type=text name=url size=70 value="<%= request.getParameter("url")==null?"":HTMLFormatter.htmlSpecialChars(request.getParameter("url")) %>"><br>
<% } %>

<% if (!group.isLineOnly() || group.isPreformatAllowed()) {%>
<select name=mode>
<% if (!group.isLineOnly()) { %>
<option value=tex>TeX paragraphs
<option value=ntobr>User line break
<% } %>
<option value=html>Ignore line breaks
<% if (group.isPreformatAllowed()) { %>
<option value=pre>Preformatted text
<% } %>
<% } else { %>
<input type=hidden name=mode value=html>
<% } %>

</select>

<select name=autourl>
<option value=1>Auto URL
<option value=0>No Auto URL
</select>

<% if (!group.isLineOnly()) { %>
<select name=texttype>
<option value=0>Plain text
<option value=1>HTML (limited)
</select>
<% } else { %>
<input type=hidden name=texttype value=0>
<% } %>

<%
  if (!Template.isSessionAuthorized(session)) {
    out.print("<p><img src=\"/jcaptcha.jsp\"><input type='text' name='j_captcha_response' value=''>");
  }
%>

<br>
<input type=submit value="Post/Поместить">

</form>
<%}
  } finally {
    if (db!=null) db.close();
  }
%>
<%=	tmpl.DocumentFooter() %>
