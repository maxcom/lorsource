<%@ page contentType="text/html; charset=koi8-r"%>
<%@ page import="java.sql.Connection,java.sql.PreparedStatement" errorPage="/error.jsp" buffer="60kb" %>
<%@ page import="java.sql.ResultSet"%>
<%@ page import="java.sql.Timestamp"%>
<%@ page import="java.util.Map"%>
<%@ page import="ru.org.linux.site.*" %>
<%@ page import="ru.org.linux.util.BadImageException" %>
<%@ page import="ru.org.linux.util.HTMLFormatter" %>
<%@ page import="ru.org.linux.util.ImageInfo" %>
<% Template tmpl = new Template(request, config, response); %>
<%
  response.setDateHeader("Expires", System.currentTimeMillis()+120000);
%>
<%= tmpl.head() %>
<% String nick=request.getParameter("nick");

  if (nick == null) {
    throw new MissingParameterException("nick");
  }
%>
<title>Информация о пользователе <%= nick %></title>
<%= tmpl.DocumentHeader() %>
<% Connection db = null;
  try {
    db = tmpl.getConnectionWhois();

    User user = new User(db, nick);
%>

<h1>Информация о пользователе <%= nick %></h1>
<table><tr>
<%
  PreparedStatement userInfo = db.prepareStatement("SELECT url, town, lastlogin, email, name, regdate FROM users WHERE nick=?");
  PreparedStatement stat1 = db.prepareStatement("SELECT count(*) as c FROM comments WHERE userid=?");
  PreparedStatement stat2 = db.prepareStatement("SELECT sections.name as pname, count(*) as c FROM topics, groups, sections WHERE topics.userid=? AND groups.id=topics.groupid AND sections.id=groups.section GROUP BY sections.name");
  PreparedStatement stat3 = db.prepareStatement("SELECT min(postdate) as first,max(postdate) as last FROM topics WHERE topics.userid=?");
  PreparedStatement stat4 = db.prepareStatement("SELECT min(postdate) as first,max(postdate) as last FROM comments WHERE comments.userid=?");
  PreparedStatement stat5 = db.prepareStatement("SELECT count(*) as inum FROM ignore_list WHERE ignored=?");
  
  userInfo.setString(1, nick);

  ResultSet rs = userInfo.executeQuery();

  if (!rs.next()) {
    throw new UserNotFoundException(nick);
  }

  int userid = user.getId();

  stat1.setInt(1, userid);
  stat2.setInt(1, userid);
  stat3.setInt(1, userid);
  stat4.setInt(1, userid);
  stat5.setInt(1, userid);

  if (user.getPhoto() != null) {
    out.print("<td valign='top' align='center'>");

    try {
      out.print("<img src=\"/photos/" + user.getPhoto() + "\" alt=\"" + nick + "\" " + new ImageInfo(tmpl.getObjectConfig().getHTMLPathPrefix() + "photos/" + user.getPhoto()).getCode() + ">");
    } catch (BadImageException ex) {
      out.print("[bad image]");
    }

    if (tmpl.isModeratorSession()) {
      out.print("<p><form name='f_remove_userpic' method='post' action='usermod.jsp'>\n");
      out.print("<input type='hidden' name='id' value='" + userid + "'>\n");
      out.print("<input type='hidden' name='action' value='remove_userpic'>\n");
      out.print("<input type='submit' value='Удалить изображение'>\n");
      out.print("</form>");
    }

    out.print("</td>");
  }
%>
<td valign="top" align="left">
<h2>Регистрация</h2>
<b>ID:</b> <%= userid %><br>
<b>Nick:</b> <%= nick %><br>
<% String url=rs.getString("url");
   String town=rs.getString("town");
   Timestamp lastlogin=rs.getTimestamp("lastlogin");
   Timestamp regdate=rs.getTimestamp("regdate");
   String fullname=rs.getString("name");
   String sEmail=rs.getString("email");
   int score = user.getScore();

   if (fullname!=null) if (!"".equals(fullname)) out.println("<b>Полное имя:</b> "+fullname+"<br>");
   if (url!=null) if (!"".equals(url)) out.println("<b>URL:</b> <a href=\""+url+"\">"+url+"</a><br>");
   if (town!=null) if (!"".equals(town)) out.println("<b>Город:</b> "+town+"<br>");
   if (regdate!=null) out.println("<b>Дата регистрации:</b> "+Template.dateFormat.format(regdate)+"<br>");
   else out.println("<b>Дата регистрации:</b> неизвестно<br>");
   if (lastlogin!=null) out.println("<b>Последний логин:</b> "+Template.dateFormat.format(lastlogin)+"<br>");
   else out.println("<b>Последний логин:</b> неизвестно<br>");
%>
<b>Статус:</b> <%= user.getStatus() %><%
  if (user.canModerate()) {
	out.print(" (модератор)");
  }

  if (user.isBlocked())
    out.println(" (заблокирован)\n");

  out.print("<br>");

  if (tmpl.isSessionAuthorized(session) && (session.getValue("nick").equals(nick) ||
              ((Boolean)session.getValue("moderator")).booleanValue())) {
            if (sEmail!=null) if (!sEmail.equals(""))
                out.println("<br><b>Email:</b> " + sEmail + "<br>");
            out.println("<b>Score</b>: "+score+"<br>\n");
			rs.close(); rs=stat5.executeQuery(); rs.next();
			out.println("<b>Игнорируется</b>: "+rs.getInt("inum")+"<br>\n");
  }
  if (tmpl.isSessionAuthorized(session) && !session.getValue("nick").equals(nick) && !session.getValue("nick").equals("anonymous")) {
    out.println("<br>");
    Map<Integer, String> ignoreList = IgnoreList.getIgnoreListHash(db,(String)session.getValue("nick"));
	if (ignoreList != null && !ignoreList.isEmpty() && ignoreList.containsValue(nick)) {
      out.print("<form name='i_unblock' method='post' action='ignore-list.jsp'>\n");
      out.print("<input type='hidden' name='ignore_list' value='" + userid + "'>\n");
      out.print("Вы игнорируете этого пользователя &nbsp; \n");
	  out.print("<input type='submit' name='del' value='не игнорировать'>\n");
      out.print("</form>");
    } else {
      out.print("<form name='i_block' method='post' action='ignore-list.jsp'>\n");
      out.print("<input type='hidden' name='nick' value='" + nick + "'>\n");
       out.print("Вы не игнорируете этого пользователя &nbsp; \n");
	  out.print("<input type='submit' name='add' value='игнорировать'>\n");
      out.print("</form>");
    }
  }

  out.println("<br>");
  if (tmpl.isModeratorSession() && user.isBlockable()) {
    if (user.isBlocked()) {
      out.print("<form name='f_unblock' method='post' action='usermod.jsp'>\n");
      out.print("<input type='hidden' name='id' value='" + userid + "'>\n");
      out.print("<input type='submit' name='action' value='unblock'>\n");
      out.print("</form>");
    } else {
      out.print("<form name='f_block' method='post' action='usermod.jsp'>\n");
      out.print("<input type='hidden' name='id' value='" + userid + "'>\n");
      out.print("<input type='submit' name='action' value='block'>\n");
      out.print("<input type='submit' name='action' value='block-n-delete-comments'>\n</form>");
    }
  }

  userInfo.close();

%>
<br>
<p>
<cite>
<%
  out.print(HTMLFormatter.nl2br(tmpl.getObjectConfig().getStorage().readMessageDefault("userinfo", String.valueOf(userid), "")));

  if (tmpl.isModeratorSession()) {
    out.print("<p><form name='f_remove_userinfo' method='post' action='usermod.jsp'>\n");
    out.print("<input type='hidden' name='id' value='" + userid + "'>\n");
    out.print("<input type='hidden' name='action' value='remove_userinfo'>\n");
    out.print("<input type='submit' value='Удалить текст'>\n");
    out.print("</form>");
  }

%>
</cite>
<%
  if (tmpl.isSessionAuthorized(session) && (session.getValue("nick").equals(nick))) {
    out.print("<p><a href=\"register.jsp?mode=change\">Изменить регистрацию</a>.");
  }
%>

<h2>Статистика</h2>
<% rs.close(); rs=stat3.executeQuery(); rs.next();
  Timestamp first = rs.getTimestamp("first");
  Timestamp last = rs.getTimestamp("last");
 %>
<b>Первая созданная тема:</b> <%= first==null?"нет":Template.dateFormat.format(first) %><br>
<b>Последняя созданная тема:</b> <%= last==null?"нет":Template.dateFormat.format(last) %><br>
<% rs.close(); %>
<% rs=stat4.executeQuery(); rs.next();
  Timestamp firstComment = rs.getTimestamp("first");
  Timestamp lastComment = rs.getTimestamp("last");
%>
<b>Первый комментарий:</b> <%= firstComment==null?"нет":Template.dateFormat.format(firstComment) %><br>
<b>Последний комментарий:</b> <%= lastComment==null?"нет":Template.dateFormat.format(lastComment) %>
<% rs.close(); %>
<p>
<div class="forum">
<div class="color1">
<table width="100%" cellspacing='1' cellpadding='0' border='0'>
<thead>
<tr class='color1'><th>Раздел</th><th>Число сообщений (тем)</th></tr>
<tbody>
<% rs=stat2.executeQuery(); %>
<%
   while (rs.next()) {
   	out.print("<tr class='color2'><td>"+rs.getString("pname")+"</td><td>"+rs.getInt("c")+"</td></tr>");
   }
%>
<tfoot>
<% rs.close(); rs=stat1.executeQuery(); rs.next(); %>
<tr class='color2'><td>Комментарии</td><td valign='top'><%= rs.getInt("c") %></td></tr>
</table>
</div></div>
<% if (userid!=2) { %>

<h2>Сообщения пользователя</h2>
<ul>
  <li>
    <a href="show-topics.jsp?nick=<%= nick %>">Темы</a>
  </li>

  <li>
    <a href="show-comments.jsp?nick=<%= nick %>">Комментарии</a>
  </li>
</ul>

<% } %>


</td></tr></table>

<%
  } finally {
    if (db!=null) db.close();
  }
%>
<%= tmpl.DocumentFooter() %>
