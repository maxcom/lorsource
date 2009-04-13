<%@ page contentType="text/html; charset=utf-8"%>
<%@ page import="java.sql.Connection,java.sql.ResultSet,java.sql.Statement,java.util.Date,ru.org.linux.site.AccessViolationException"  %>
<%@ page import="ru.org.linux.site.LorDataSource"%>
<%@ page import="ru.org.linux.site.Template"%>
<%@ page import="ru.org.linux.site.User"%>
<%@ taglib tagdir="/WEB-INF/tags" prefix="lor" %>

<%--
  ~ Copyright 1998-2009 Linux.org.ru
  ~    Licensed under the Apache License, Version 2.0 (the "License");
  ~    you may not use this file except in compliance with the License.
  ~    You may obtain a copy of the License at
  ~
  ~        http://www.apache.org/licenses/LICENSE-2.0
  ~
  ~    Unless required by applicable law or agreed to in writing, software
  ~    distributed under the License is distributed on an "AS IS" BASIS,
  ~    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
  ~    See the License for the specific language governing permissions and
  ~    limitations under the License.
  --%>

<% Template tmpl = Template.getTemplate(request); %>
<jsp:include page="head.jsp"/>

<title>Регистрация пользователя</title>
<jsp:include page="header.jsp"/>
<%
  response.addHeader("Cache-Control", "no-store, no-cache, must-revalidate");

   if (request.getParameter("mode")==null) {
     session.setAttribute("register-visited", Boolean.TRUE);

%>
<H1>Регистрация</H1>
Если вы уже регистрировались на нашем сайте и забыли пароль - вам
<a href="../../lostpwd.jsp">сюда</a>.

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
<p>
  <lor:captcha/>

<br>
<input type=submit value="Зарегистрироваться">
</form>
<%
} else if ("change".equals(request.getParameter("mode"))) {
%>
  <table class=nav><tr>
    <td align=left valign=middle>
      Изменение регистрации
    </td>

    <td align=right valign=middle>
      [<a style="text-decoration: none" href="../../addphoto.jsp">Добавить фотографию</a>]
      [<a style="text-decoration: none" href="../../rules.jsp">Правила форума</a>]
     </td>
    </tr>
 </table>
<h1>Изменение регистрации</h1>
<%
  if (!Template.isSessionAuthorized(session)) {
    throw new AccessViolationException("Not authorized");
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
	if (rs.getString("url")!=null) {
      out.print(rs.getString("url"));
    }
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
    if (db!=null) {
      db.close();
    }
  }
%>

<% } %>
<jsp:include page="footer.jsp"/>
