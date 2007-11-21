<%@ page pageEncoding="koi8-r" contentType="text/html; charset=utf-8"%>
<%@ page import="javax.servlet.http.Cookie,javax.servlet.http.HttpServletResponse,ru.org.linux.site.BadInputException, ru.org.linux.site.Template, ru.org.linux.site.UserErrorException" errorPage="/error.jsp" buffer="20kb" %>
<%@ page import="ru.org.linux.util.ProfileHashtable"%>
<%@ page import="ru.org.linux.util.StringUtil" %>
<% Template tmpl = new Template(request, config, response); %>
<%= tmpl.head() %>
	<title>Настройки профиля</title>
<%= tmpl.DocumentHeader() %>

  <table class=nav><tr>
    <td align=left valign=middle>
      Настройки профиля
    </td>

    <td align=right valign=middle>
      [<a style="text-decoration: none" href="addphoto.jsp">Добавить фотографию</a>]
      [<a style="text-decoration: none" href="register.jsp?mode=change">Изменение регистрации</a>]
      [<a style="text-decoration: none" href="rules.jsp">Правила форума</a>]
     </td>
    </tr>
 </table>

<h1>Настройки профиля</h1>

<%
   if (request.getParameter("mode")==null) {
	if (tmpl.isUsingDefaultProfile())
		out.print("Используется профиль по умолчанию");
	else
		out.print("Используется профиль: <i>" + tmpl.getProfileName()+"</i>");

%>
<%
  if (!tmpl.isSessionAuthorized(session)) {
%>

<h2>Коротко о...</h2>
<ol>
<li>Профиль содержит различные настройки отображения сайта и хранится у нас
на сервере
<li>Настройки профиля ассоциируются с регистрационным именем пользователя
(если вы еще не зарегистрировались у нас на сайте - вам <a href="register.jsp">сюда</a>).
<li>Информация о наличие профиля запоминается в Cookie, при смене броузера
или местоположения вы можете востановить свой профиль без полной перенастройки.
<li>Использовать ваш профиль могут любые посетители сайта, но модифицировать его
можете только вы.
</ol>

<h2>Установить профиль</h2>
Востановить профиль при смене местоположения или выбрать другой (существующий)
профиль:
<form method=POST action="edit-profile.jsp">
<input type=hidden name=mode value=setup>
Профиль:
<input type=text name=profile><br>
Ваш nick совпадает с именем выбранного профиля?
<input type=checkbox name=setnick><br>
<input type=submit value="Setup/Установить">
</form>
<%
  }
%>

<h2>Параметры профиля</h2>
<% ProfileHashtable profHash=tmpl.getProf(); %>
<form method=POST action="edit-profile.jsp">
<input type=hidden name=mode value=set>
<table>
<tr><td colspan=2><hr></td></tr>
<tr><td>Новые комментарии в начале</td>
<td><input type=checkbox name=newfirst <%= profHash.getBooleanPropertyHTML("newfirst")%>></td></tr>
<tr><td>Показывать фотографии</td>
<td><input type=checkbox name=photos <%= profHash.getBooleanPropertyHTML("photos")%>></td></tr>
<tr><td>Показывать сообщение о порядке сортировки комментариев</td>
<td><input type=checkbox name=sortwarning <%= profHash.getBooleanPropertyHTML("sortwarning")%>></td></tr>
<tr><td>Число тем форума на странице</td>
<td><input type=text name=topics value=<%= profHash.getInt("topics")%>></td></tr>
<tr><td>Число комментариев на странице</td>
<td><input type=text name=messages value=<%= profHash.getInt("messages")%>></td></tr>
<tr><td>Верстка главной страницы в 3 колонки</td>
<td><input type=checkbox name=3column <%= profHash.getBooleanPropertyHTML("main.3columns")%>></td></tr>
<tr><td>Показывать информацию о регистрации перед формами добавления сообщений</td>
<td><input type=checkbox name=showinfo <%= profHash.getBooleanPropertyHTML("showinfo")%>></td></tr>
<tr><td>Показывать анонимные комментарии</td>
<td><input type=checkbox name=showanonymous <%= profHash.getBooleanPropertyHTML("showanonymous")%>></td></tr>
<tr><td>Показывать комментарии игнорируемых пользователей</td>
<td><input type=checkbox name=showignored <%= profHash.getBooleanPropertyHTML("showignored")%>></td></tr>
<tr><td>Подсветка строчек в таблицах сообщений (tr:hover)</td>
<td><input type=checkbox name=hover <%= profHash.getBooleanPropertyHTML("hover")%>></td></tr>  
  <tr><td colspan=2><hr></td></tr>
<tr>
  <td valign=top>Тема</td>
  <td>
    <% String style=tmpl.getStyle(); %>
    <input type=radio name=style value=white <%= "white".equals(style)?"checked":"" %>> White (old)<br>
    <input type=radio name=style value=black <%= "black".equals(style)?"checked":"" %>> Black (default)<br>
    <input type=radio name=style value=white2 <%= "white2".equals(style)?"checked":"" %>> White2<br>
  </td>
</tr>

<% if (!tmpl.isSessionAuthorized(session)) { %>

<tr><td colspan=2><hr></td></tr>
<tr><td>Профиль (имя пользователя)</td><td>
<input type=text name=profile value="<%= tmpl.getCookie("NickCookie", "")%>"></td></tr>
<tr><td>Пароль</td><td>
<input type=password name=password></td></tr>
<% } %>

</table>

<input type=submit value="Setup/Установить">
</form>

<h2>Настройка главной страницы</h2>
После того, как вы создали свой собственный профиль, вы можете
настройть под себя содержимое стартовой страницы.
<ul>
<li><a href="edit-boxes.jsp">настройка стартовой страницы</a>
</ul>

<h2>Настройка фильтрации сообщений</h2>
<ul>
<li><a href="ignore-list.jsp">настройка фильтрации сообщений</a>
</ul>

<%
  } else if ("setup".equals(request.getParameter("mode"))) {
    String name = StringUtil.getFileName(request.getParameter("profile"));
    if (name.length()!=0 && !Template.isAnonymousProfile(name)) {
      throw new UserErrorException("Данный профиль не может быть выбран");
    }

    out.print("Выбран профиль: " + name);

    response.setHeader("Location", tmpl.getMainUrl());
    response.setStatus(HttpServletResponse.SC_MOVED_TEMPORARILY);

    Cookie prof = new Cookie("profile", name);
    if (name.length()==0) {
      prof.setMaxAge(0);
    } else {
      prof.setMaxAge(60 * 60 * 24 * 31 * 12);
    }

    prof.setPath("/");
    response.addCookie(prof);
  } else if ("set".equals(request.getParameter("mode"))) {
    String profile;

    if (!tmpl.isSessionAuthorized(session)) {
      throw new IllegalAccessException("Not authorized");
    } else {
      profile = (String) session.getAttribute("nick");
    }

    int topics = Integer.parseInt(request.getParameter("topics"));
    int messages = Integer.parseInt(request.getParameter("messages"));

    if (topics <= 0 || topics > 1000)
      throw new BadInputException("некорректное число тем");
    if (messages <= 0 || messages > 1000)
      throw new BadInputException("некорректное число сообщений");

    if (tmpl.getProf().setInt("topics", new Integer(topics))) ;
    out.print("Установлен параметр <i>topics</i><br>");
    if (tmpl.getProf().setInt("messages", new Integer(messages))) ;
    out.print("Установлен параметр <i>messages</i><br>");
    if (tmpl.getProf().setBoolean("newfirst", request.getParameter("newfirst")))
      out.print("Установлен параметр <i>newfirst</i><br>");
    if (tmpl.getProf().setBoolean("photos", request.getParameter("photos")))
      out.print("Установлен параметр <i>photos</i><br>");
    if (tmpl.getProf().setBoolean("sortwarning", request.getParameter("sortwarning")))
      out.print("Установлен параметр <i>sortwarning</i><br>");
    if (tmpl.getProf().setString("style", request.getParameter("style")))
      out.print("Установлен параметр <i>style</i><br>");
    if (tmpl.getProf().setBoolean("main.3columns", request.getParameter("3column")))
      out.print("Установлен параметр <i>main.3columns</i><br>");
    if (tmpl.getProf().setBoolean("showinfo", request.getParameter("showinfo")))
      out.print("Установлен параметр <i>showinfo</i><br>");
    if (tmpl.getProf().setBoolean("showanonymous", request.getParameter("showanonymous")))
      out.print("Установлен параметр <i>showanonymous</i><br>");
    if (tmpl.getProf().setBoolean("showignored", request.getParameter("showignored")))
      out.print("Установлен параметр <i>showignored</i><br>");
    if (tmpl.getProf().setBoolean("hover", request.getParameter("hover")))
      out.print("Установлен параметр <i>hover</i><br>");

    tmpl.writeProfile(profile);

    Cookie prof = new Cookie("profile", profile);
    prof.setMaxAge(60 * 60 * 24 * 31 * 12);
    prof.setPath("/");
    response.addCookie(prof);

    response.setHeader("Location", tmpl.getMainUrl());
    response.setStatus(HttpServletResponse.SC_MOVED_TEMPORARILY);

    out.print("Ok");
  }
%>
<p><b>Внимание!</b> настройки на некоторых уже посещенных страницах могут
не отображаться. Используйте кнопку <i>Reload</i> вашего броузера.

<%= tmpl.DocumentFooter() %>
