<%@ page contentType="text/html; charset=koi8-r"%>
<%@ page contentType="text/html; charset=koi8-r" import="java.sql.Connection,ru.org.linux.util.ImageInfo" errorPage="error.jsp"%>
<%@ page import="ru.org.linux.site.*"%>
<%@ page import="ru.org.linux.util.StringUtil"%>
<%@ page import="ru.org.linux.util.HTMLFormatter"%>
<% Template tmpl = new Template(request, config, response);%>
<%= tmpl.head() %>
<%
  Connection db = null;

  try {
    int groupId = tmpl.getParameters().getInt("group");

    db = tmpl.getConnection("add");
    Group group = new Group(db, groupId);

    User currentUser = User.getCurrentUser(db, session);

    if (!group.isTopicPostingAllowed(currentUser)) {
      throw new AccessViolationException("Не достаточно прав для постинга тем в эту группу");
    }

    session.setAttribute("add-visited", Boolean.TRUE);
    response.addHeader("Cache-Control", "no-store, no-cache, must-revalidate");
    response.addHeader("Pragma", "no-cache");
%>
<title>Добавить сообщение</title>
<%= tmpl.DocumentHeader() %>
<%	int section=group.getSectionId();
	if (request.getParameter("noinfo")==null || !"1".equals(request.getParameter("noinfo")))
		out.print(tmpl.getObjectConfig().getStorage().readMessageDefault("addportal", String.valueOf(section), ""));
%>
<h1>Добавить</h1>
<% if (tmpl.getProf().getBooleanProperty("showinfo") && !tmpl.isSessionAuthorized(session)) { %>
<font size=2>Чтобы просто поместить сообщение, используйте login `anonymous',
без пароля. Если вы собираетесь активно участвовать в форуме,
помещать новости на главную страницу,
<a href="register.jsp">зарегистрируйтесь</a></font>.
<p>

<% if (!group.isLineOnly()) { %>
<font size=2>В HTML режиме можно использовать теги &lt;a&gt; &lt;p&gt; &lt;li&gt; </font>
<% } %>
<% } %>

<% if (group.isImagePostAllowed()) {
	if (request.getParameter("icon")==null) throw new MissingParameterException("icon");
	if (request.getParameter("url")==null) throw new MissingParameterException("url");
	if ("".equals(request.getParameter("icon"))) throw new MissingParameterException("icon");
	if ("".equals(request.getParameter("url"))) throw new MissingParameterException("url");

	String linktext=request.getParameter("icon");

	ImageInfo info=new ImageInfo(tmpl.getObjectConfig().getHTMLPathPrefix()+linktext);
        out.print("<img src=\"/" + linktext + "\" ALT=icon " + info.getCode() +  " >");
   }
%>
<form method=POST action="add2.jsp">
  <input type="hidden" name="session" value="<%= HTMLFormatter.htmlSpecialChars(session.getId()) %>">

<% if (session==null || session.getValue("login")==null || !((Boolean) session.getValue("login")).booleanValue()) { %>
Имя:
<input type=text name=nick value="<%= tmpl.getCookie("NickCookie","anonymous") %>" size=40><br>
Пароль:
<input type=password name=password size=40><br>
<% } %>
<input type=hidden name=guid value="<%= groupId %>">

<% if (request.getParameter("return")!=null) { %>
<input type=hidden name=return value="<%= request.getParameter("return") %>">
<% } %>

Заглавие:
<input type=text name=title size=40><br>

<% if (group.isLinksAllowed() && group.isLinksUp()) { %>
<input type=hidden name=linktext value="<%= group.getDefaultLinkText() %> ">
Ссылка (не забудьте <b>http://</b>)
<input type=text name=url size=70><br>
<% } %>

Сообщение:<br>
<font size=2>(В режиме <i>Tex paragraphs</i> игнорируются переносы строк.<br> Пустая строка (два раза Enter) начинает новый абзац)</font><br>
<textarea name=msg cols=70 rows=20></textarea><br>

<% if (group.isLinksAllowed() && !group.isLinksUp()) { %>
Текст ссылки:
<input type=text name=linktext size=60 value="<%= group.getDefaultLinkText() %>"><br>
Ссылка (не забудьте <b>http://</b>)
<input type=text name=url size=70><br>
<% } %>

<% if (group.isImagePostAllowed()) { %>
<input type=hidden name=linktext value="<%= request.getParameter("icon") %>">
<input type=hidden name=url value="<%= request.getParameter("url") %>">
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
<%
  } finally {
    if (db!=null) db.close();
  }
%>
<%=	tmpl.DocumentFooter() %>
