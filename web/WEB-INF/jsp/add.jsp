<%@ page pageEncoding="koi8-r" contentType="text/html; charset=utf-8" import="java.sql.Connection,org.apache.commons.lang.StringUtils"  %>
<%@ page import="ru.org.linux.site.*"%>
<%@ page import="ru.org.linux.util.HTMLFormatter"%>
<% Template tmpl = Template.getTemplate(request);%>
<jsp:include page="/WEB-INF/jsp/head.jsp"/>
<%@ taglib tagdir="/WEB-INF/tags" prefix="lor" %>

<%
  Connection db = null;

  try {
    Exception error = (Exception) request.getAttribute("error");

    Message previewMsg = (Message) request.getAttribute("message");
    boolean preview = previewMsg!=null;

    db = LorDataSource.getConnection();

    if (!preview) {
      try {
        previewMsg = new Message(db,tmpl,session,request);
      } catch (MessageNotFoundException e) { }
    }

    Integer groupId = (Integer)request.getAttribute("group");

    Group group = new Group(db, groupId);

    User currentUser = User.getCurrentUser(db, session);

    if (!group.isTopicPostingAllowed(currentUser)) {
      throw new AccessViolationException("Не достаточно прав для постинга тем в эту группу");
    }

    String mode = (String)request.getAttribute("mode");
    boolean autourl = (Boolean) request.getAttribute("autourl");

%>

<title>Добавить сообщение</title>
  <jsp:include page="/WEB-INF/jsp/header.jsp"/>

<%	int section=group.getSectionId();
	if (request.getAttribute("noinfo")==null || !"1".equals(request.getAttribute("noinfo"))) {
          out.print(tmpl.getObjectConfig().getStorage().readMessageDefault("addportal", String.valueOf(section), ""));
        }
%>
<% if (preview && previewMsg!=null) { %>
<h1>Предпросмотр</h1>
<div class=messages>
  <lor:message db="<%= db %>" message="<%= previewMsg %>" showMenu="false" user="<%= Template.getNick(session) %>"/>
</div>
<% } %>
<% if (error==null) { %>
<h1>Добавить</h1>
<% } else { out.println("<h1>Ошибка: "+error.getMessage()+"</h1>"); } %>
<% if (tmpl.getProf().getBoolean("showinfo") && !Template.isSessionAuthorized(session)) { %>
<font size=2>Чтобы просто поместить сообщение, используйте login `anonymous',
без пароля. Если вы собираетесь активно участвовать в форуме,
помещать новости на главную страницу,
<a href="register.jsp">зарегистрируйтесь</a></font>.
<p>
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
<%  if (request.getAttribute("noinfo")!=null) {
  %>
  <input type="hidden" name="noinfo" value="<%= request.getAttribute("noinfo") %>">
 <% }
%>
<% if (session == null || session.getValue("login") == null || !(Boolean) session.getValue("login")) { %>
Имя:
<input type=text name=nick value="<%= request.getAttribute("nick")==null?"anonymous":HTMLFormatter.htmlSpecialChars((String)request.getAttribute("nick")) %>" size=40><br>
Пароль:
<input type=password name=password size=40><br>
<% } %>
<input type=hidden name=group value="<%= groupId %>">

<% if (request.getAttribute("return")!=null) { %>
<input type=hidden name=return value="<%= HTMLFormatter.htmlSpecialChars((String)request.getAttribute("return")) %>">
<% } %>

Заглавие:
<input type=text name=title size=40 value="<%= request.getAttribute("title")==null?"":HTMLFormatter.htmlSpecialChars((String)request.getAttribute("title")) %>" ><br>

  <% if (group.isImagePostAllowed()) { %>
  Изображение:
  <input type="file" name="image"><br>
  <% } %>

Сообщение:<br>
<font size=2>(В режиме <i>Tex paragraphs</i> игнорируются переносы строк.<br> Пустая строка (два раза Enter) начинает новый абзац)</font><br>
<textarea name=msg cols=70 rows=20><%
    if (request.getAttribute("msg")!=null) {
      out.print(HTMLFormatter.htmlSpecialChars((String)request.getAttribute("msg")));
    }
  %></textarea><br>

<% if (group.isLinksAllowed()) { %>
Текст ссылки:
<input type=text name=linktext size=60 value="<%= request.getAttribute("linktext")==null?group.getDefaultLinkText():HTMLFormatter.htmlSpecialChars((String)request.getAttribute("linktext")) %>"><br>
Ссылка (не забудьте <b>http://</b>)
<input type=text name=url size=70 value="<%= request.getAttribute("url")==null?"":HTMLFormatter.htmlSpecialChars((String)request.getAttribute("url")) %>"><br>
<% } %>
<% if (group.getSectionId()==1) { %>
Метки (разделенные запятой) 
<input type=text name=tags id="tags" size=70 value="<%= request.getAttribute("tags")==null?"":StringUtils.strip((String)request.getAttribute("tags")) %>"><br>
  Популярные теги: <%= Tags.getEditTags(Tags.getTopTags(db)) %> <br>
<% } %>
<% if (!group.isLineOnly() || group.isPreformatAllowed()) {%>
<select name=mode>
<% if (!group.isLineOnly()) { %>
<option value=tex <%= (preview && mode.equals("tex"))?"selected":""%> >TeX paragraphs
<option value=ntobr <%= (preview && mode.equals("ntobr"))?"selected":""%> >User line break
<% } %>
<% if (group.isPreformatAllowed()) { %>
<option value=pre <%= (preview && mode.equals("pre"))?"selected":""%> >Preformatted text
<% } %>
<% } else { %>
<input type=hidden name=mode value=html>
<% } %>
</select>

<select name=autourl>
<option value=1 <%= (preview && autourl)?"selected":""%> >Auto URL
<option value=0 <%= (preview && !autourl)?"selected":""%> >No Auto URL
</select>

<%
  if (!Template.isSessionAuthorized(session)) {
    out.print("<p><img src=\"/jcaptcha.jsp\"><input type='text' name='j_captcha_response' value=''>");
  }
%>
<br>
<input type=submit value="Поместить">
<input type=submit name=preview value="Предпросмотр">
</form>
<%
  } finally {
    if (db!=null) {
      db.close();
    }
  }
%>
<jsp:include page="/WEB-INF/jsp/footer.jsp"/>
