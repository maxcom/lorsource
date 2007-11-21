<%@ page pageEncoding="koi8-r" contentType="text/html; charset=utf-8"%>
<%@ page import="java.sql.Connection,ru.org.linux.site.AccessViolationException,ru.org.linux.site.Message,ru.org.linux.site.Template,ru.org.linux.util.HTMLFormatter" errorPage="/error.jsp" buffer="200kb"%>
<% Template tmpl = new Template(request, config, response); %>
<%= tmpl.head() %>
<%
  Connection db = null;

  try {
    int msgid = tmpl.getParameters().getInt("msgid");

    db = tmpl.getConnection();

    Message message = new Message(db, msgid);

    if (message.isExpired())
      throw new AccessViolationException("������ �������������� ���������� ����");

    if (message.isDeleted())
      throw new AccessViolationException("������ �������������� ��������� ���������");

    if (!message.isCommentEnabled()) {
      throw new AccessViolationException("������ �������������� ����");
    }

    out.print("<title>" + message.getSectionTitle() + " - " + message.getGroupTitle() + " - " + message.getTitle() + "</title>");
%>
<%= tmpl.DocumentHeader() %>

<div class=messages>

<%
   out.print(message.printMessage(tmpl, db, true, Template.getNick(session)));
%>
</div>

<% if (message.isCommentEnabled()) { %>

<h2><a name=rep>�������� ���������:</a></h2>
<% if (tmpl.getProf().getBoolean("showinfo") && !Template.isSessionAuthorized(session)) { %>
<font size=2>����� ������ ��������� ���������, ����������� login `anonymous',
��� ������. ���� �� ����������� ������� ����������� � ������,
�������� ������� �� ������� ��������,
<a href="register.jsp">�����������������</a></font>.
<p>

<% } %>
<font size=2><strong>��������!</strong> ����� ���������� ����������� ������������ �
<a href="rules.jsp">���������</a> �����.</font><p>

<%
  out.print(Message.getPostScoreInfo(message.getPostScore()));
%>

<form method=POST action="add_comment.jsp">
  <input type="hidden" name="session" value="<%= HTMLFormatter.htmlSpecialChars(session.getId()) %>">  
<% if (session == null || session.getAttribute("login") == null || !(Boolean) session.getAttribute("login")) { %>
���:
<input type=text name=nick value="<%= tmpl.getCookie("NickCookie","anonymous") %>" size=40><br>
������:
<input type=password name=password size=40><br>
<% } %>
<% out.print("<input type=hidden name=topic value="+msgid+ '>'); %>
��������:
<input type=text name=title size=40 value="Re: <%= message.getTitle() %>"><br>
���������:<br>
<font size=2>(� ������ <i>Tex paragraphs</i> ������������ �������� �����.<br> ������ ������ (��� ���� Enter) �������� ����� �����.<br> ���� '&gt;' � ������ ������ �������� ����� �������� �����������)</font><br>
<textarea name=msg cols=70 rows=20 onkeypress="return ctrl_enter(event, this.form);"></textarea><br>

<select name=mode>
<option value=quot>TeX paragraphs w/quoting
<option value=tex>TeX paragraphs w/o quoting
<option value=ntobr>User line breaks
<option value=pre>Preformatted text
</select>

<select name=autourl>
<option value=1>Auto URL
<option value=0>No Auto URL
</select>

<input type=hidden name=texttype value=0>
<br>

<%
  out.print(Message.getPostScoreInfo(message.getPostScore()));
%>

<br>

  <%  
  if (!Template.isSessionAuthorized(session)) {
    out.print("<p><img src=\"/jcaptcha.jsp\"><input type='text' name='j_captcha_response' value=''>");
  }
%>

<input type=submit value="���������">
<input type=submit name=preview value="������������">  
</form>

<% } %>
<%
  } finally {
    if (db!=null) db.close();
  }
%>
<%=	tmpl.DocumentFooter() %>
