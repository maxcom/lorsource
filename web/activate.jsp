<%@ page contentType="text/html; charset=utf-8" pageEncoding="koi8-r"%>
<%@ page import="ru.org.linux.site.Template" errorPage="/error.jsp"%>
<% Template tmpl = new Template(request, config, response); %>
<%= tmpl.head() %>
<title>Активация</title>
<%= tmpl.DocumentHeader() %>
<h1>Активация</h1>

<form method=POST action="login.jsp">
  <table>
    <tr>
      <td>Nick:</td>
      <td><input type=text name=nick></td>
    </tr>

    <tr>
      <td>Пароль:</td>
      <td><input type=password name=passwd></td>
    </tr>

    <tr>
      <td>Код активациии:</td>
      <td><input type=text name=activate></td>
    </tr>

  </table>

  <input type=submit value="Активировать">
</form>

<%=	tmpl.DocumentFooter() %>
