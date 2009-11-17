<%@ page contentType="text/html; charset=utf-8"%>
<%@ page import="javax.servlet.http.Cookie,javax.servlet.http.HttpServletResponse"   buffer="20kb" %>
<%@ page import="ru.org.linux.site.AccessViolationException" %>
<%@ page import="ru.org.linux.site.BadInputException" %>
<%@ page import="ru.org.linux.site.DefaultProfile" %>
<%@ page import="ru.org.linux.site.Template" %>
<%@ page import="ru.org.linux.util.ProfileHashtable" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
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
<jsp:include page="WEB-INF/jsp/head.jsp"/>

        <title>Настройки профиля</title>
<jsp:include page="WEB-INF/jsp/header.jsp"/>

  <table class=nav><tr>
    <td align=left valign=middle id="navPath">
      Настройки профиля
    </td>

    <td align=right valign=middle>
      [<a style="text-decoration: none" href="addphoto.jsp">Добавить фотографию</a>]
      [<a style="text-decoration: none" href="register.jsp?mode=change">Изменение регистрации</a>]
      [<a style="text-decoration: none" href="rules.jsp">Правила форума</a>]
     </td>
    </tr>
 </table>

<h1 class="optional">Настройки профиля</h1>

<%
  if (request.getParameter("mode") == null) {
    if (!tmpl.isSessionAuthorized()) {
      throw new AccessViolationException("Not authorized");
    }

    if (tmpl.isUsingDefaultProfile()) {
      out.print("Используется профиль по умолчанию");
    } else {
      out.print("Используется профиль: <i>" + tmpl.getProfileName() + "</i>");
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
<tr><td>Число меток в облаке</td>
<td><input type=text name=tags value=<%= profHash.getInt("tags")%>></td></tr>
<tr><td>Верстка главной страницы в 3 колонки</td>
<td><input type=checkbox name=3column <%= profHash.getBooleanPropertyHTML("main.3columns")%>></td></tr>
<tr><td>Показывать информацию о регистрации перед формами добавления сообщений</td>
<td><input type=checkbox name=showinfo <%= profHash.getBooleanPropertyHTML("showinfo")%>></td></tr>
<tr><td>Показывать анонимные комментарии</td>
<td><input type=checkbox name=showanonymous <%= profHash.getBooleanPropertyHTML("showanonymous")%>></td></tr>
<tr><td>Подсветка строчек в таблицах сообщений (tr:hover)</td>
<td><input type=checkbox name=hover <%= profHash.getBooleanPropertyHTML("hover")%>></td></tr>  
<tr><td>Показывать меньше рекламы</td>
<td><input type=checkbox name=hideAdsense <%= profHash.getBooleanPropertyHTML(DefaultProfile.HIDE_ADSENSE)%>></td></tr>
  <tr><td colspan=2><hr></td></tr>
<tr>
  <td valign=top>Тема</td>
  <td>
    <c:set value="<%= tmpl.getStyle() %>" var="style"/>

    <c:forEach var="s" items="<%= DefaultProfile.getStyleList() %>">
      <c:if test="${s == style}">
        <input type=radio name=style value="${s}" id="style-${s}" checked><label for="style-${s}">${s}</label><br>
      </c:if>
      <c:if test="${s != style}">
        <input type=radio name=style id="style-${s}" value="${s}"><label for="style-${s}">${s}</label><br>
      </c:if>
    </c:forEach>
    Внимание! Тема "tango" находится в состоянии разработки
  </td>
</tr>
  <tr><td colspan=2><hr></td></tr>
  <tr>
    <td valign=top>При отсутствии аватара показывать</td>
    <td>
      <c:set value="<%= profHash.getString(&quot;avatar&quot;) %>" var="avatar"/>

      <c:forEach var="s" items="<%= DefaultProfile.getAvatars() %>">
        <c:if test="${s == avatar}">
          <input type=radio name=avatar value="${s}" checked id="avatar-${s}"><label for="avatar-${s}">${s}</label><br>
        </c:if>
        <c:if test="${s != avatar}">
          <input type=radio name=avatar value="${s}" id="avatar-${s}"><label for="avatar-${s}">${s}</label><br>
        </c:if>
      </c:forEach>
    </td>
  </tr>

  <tr><td colspan=2><hr></td></tr>
<tr>
  <td valign=top>Форматирование по умолчанию</td>
  <td>
    <% String formatMode=tmpl.getFormatMode(); %>
    <input type=radio name=format_mode id="format-ntobrq" value=ntobrq <%= "ntobrq".equals(formatMode)?"checked":"" %>><label for="format-ntobrq">User line break w/quoting</label><br>
    <input type=radio name=format_mode id="format-quot"  value=quot   <%= "quot".equals(formatMode)?"checked":"" %>><label for="format-quot">TeX paragraphs w/quoting (default)</label><br>
    <input type=radio name=format_mode id="format-tex" value=tex    <%= "tex".equals(formatMode)?"checked":"" %>><label for="format-tex">TeX paragraphs w/o quoting</label><br>
    <input type=radio name=format_mode id="format-ntobr" value=ntobr  <%= "ntobr".equals(formatMode)?"checked":"" %>><label for="format-ntobr">User line break w/o quoting</label><br>
    <input type=radio name=format_mode id="format-lorcode" value=lorcode  <%= "lorcode".equals(formatMode)?"checked":"" %>><label for="format-lorcode">LORCODE</label><br>
  </td>
</tr>

</table>

<input type=submit value="Setup/Установить">
</form>

<h2>Настройка главной страницы</h2>
После того, как вы создали свой собственный профиль, вы можете
настройть под себя содержимое стартовой страницы.
<ul>
<li><a href="/edit-boxes.jsp">настройка стартовой страницы</a>
</ul>

<h2>Настройка фильтрации сообщений</h2>
<ul>
<li><a href="ignore-list.jsp">настройка фильтрации сообщений</a>
</ul>

<%
  } else if ("set".equals(request.getParameter("mode"))) {
    String profile;

    if (!Template.isSessionAuthorized(session)) {
      throw new AccessViolationException("Not authorized");
    } else {
      profile = (String) session.getAttribute("nick");
    }

    int topics = Integer.parseInt(request.getParameter("topics"));
    int messages = Integer.parseInt(request.getParameter("messages"));
    int tags = Integer.parseInt(request.getParameter("tags"));

    if (topics <= 0 || topics > 1000)
      throw new BadInputException("некорректное число тем");
    if (messages <= 0 || messages > 1000)
      throw new BadInputException("некорректное число сообщений");
    if (tags<=0 || tags>100)
      throw new BadInputException("некорректное число меток в облаке");

    if (tmpl.getProf().setInt("topics", topics)) ;
    out.print("Установлен параметр <i>topics</i><br>");
    if (tmpl.getProf().setInt("messages", messages)) ;
    out.print("Установлен параметр <i>messages</i><br>");
    if (tmpl.getProf().setInt("tags", tags)) ;
    out.print("Установлен параметр <i>tags</i><br>");
    if (tmpl.getProf().setBoolean("newfirst", request.getParameter("newfirst")))
      out.print("Установлен параметр <i>newfirst</i><br>");
    if (tmpl.getProf().setBoolean("photos", request.getParameter("photos")))
      out.print("Установлен параметр <i>photos</i><br>");
    if (tmpl.getProf().setBoolean("sortwarning", request.getParameter("sortwarning")))
      out.print("Установлен параметр <i>sortwarning</i><br>");
    if (tmpl.getProf().setBoolean(DefaultProfile.HIDE_ADSENSE, request.getParameter(DefaultProfile.HIDE_ADSENSE)));
      out.print("Установлен параметр <i>hideAdsense</i><br>");
    if (tmpl.getProf().setString("format.mode", request.getParameter("format_mode")))
      out.print("Установлен параметр <i>format.mode</i><br>");
    if (tmpl.getProf().setString("style", request.getParameter("style")))
      out.print("Установлен параметр <i>style</i><br>");

    String avatar = request.getParameter("avatar");

    if (!DefaultProfile.getAvatars().contains(avatar)) {
      throw new BadInputException("invalid avatar value");
    }

    if (tmpl.getProf().setString("avatar", avatar))
      out.print("Установлен параметр <i>avatar</i><br>");
    if (tmpl.getProf().setBoolean("main.3columns", request.getParameter("3column")))
      out.print("Установлен параметр <i>main.3columns</i><br>");
    if (tmpl.getProf().setBoolean("showinfo", request.getParameter("showinfo")))
      out.print("Установлен параметр <i>showinfo</i><br>");
    if (tmpl.getProf().setBoolean("showanonymous", request.getParameter("showanonymous")))
      out.print("Установлен параметр <i>showanonymous</i><br>");
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

  <jsp:include page="WEB-INF/jsp/footer.jsp"/>
