<%@ page contentType="text/html; charset=koi8-r"%>
<%@ page import="java.sql.*,ru.org.linux.site.MissingParameterException,ru.org.linux.site.Template,ru.org.linux.site.User,ru.org.linux.site.UserNotFoundException,ru.org.linux.util.HTMLFormatter" errorPage="error.jsp" buffer="60kb" %>
<%@ page import="ru.org.linux.util.ImageInfo"%>
<%@ page import="ru.org.linux.util.StringUtil"%>
<%@ page import="ru.org.linux.util.BadImageException"%>
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

    // update user to moderator command -> block/unblock
    if (tmpl.isModeratorSession()) {
      if (request.getMethod().equals("POST")) {
        String moder_name = (String) session.getValue("nick");

        out.print("<!-- update mode -->");
        String abuser = user.getNick();
        String logmessage = "user " + abuser;
        int uid = user.getId();
        Statement st1 = db.createStatement();

        if (user.isBlockable()) {
          if (request.getParameter("block") != null) {
            st1.executeUpdate("UPDATE users SET blocked='t' WHERE id=" + uid);
            out.print("<!-- user " + abuser + " is blocked -->");
            logmessage = logmessage + " blocked by " + moder_name;
            tmpl.getLogger().notice("whois.jsp", logmessage);
          }
          if (request.getParameter("unblock") != null) {
            st1.executeUpdate("UPDATE users SET blocked='f' WHERE id=" + uid);
            out.print("<!-- user " + user.getNick() + " is unblocked -->");
            logmessage = logmessage + " unblocked by " + moder_name;
            tmpl.getLogger().notice("whois.jsp", logmessage);
          }
          st1.close();

          user = new User(db, nick);
        } else {
          out.print("<!-- moders and anonymous can't be blocked -->");
        }
        // rs.close();
      } else {
        out.print("<!-- userview mode "+request.getMethod()+" -->");
      }
    }
%>

<h1>Информация о пользователе <%= nick %></h1>
<table><tr>
<%
  PreparedStatement userInfo = db.prepareStatement("SELECT url, photo, town, lastlogin, email, name, regdate FROM users WHERE nick=?");
  PreparedStatement stat1 = db.prepareStatement("SELECT count(*) as c FROM comments WHERE userid=?");
  PreparedStatement stat2 = db.prepareStatement("SELECT sections.name as pname, count(*) as c FROM topics, groups, sections WHERE topics.userid=? AND groups.id=topics.groupid AND sections.id=groups.section GROUP BY sections.name");
  PreparedStatement stat3 = db.prepareStatement("SELECT min(postdate) as first,max(postdate) as last FROM topics WHERE topics.userid=?");
  PreparedStatement stat4 = db.prepareStatement("SELECT min(postdate) as first,max(postdate) as last FROM comments WHERE comments.userid=?");

  userInfo.setString(1, nick);

  ResultSet rs = userInfo.executeQuery();

  if (!rs.next()) {
    throw new UserNotFoundException(nick);
  }

  String photo = rs.getString("photo");
  int userid = user.getId();

  stat1.setInt(1, userid);
  stat2.setInt(1, userid);
  stat3.setInt(1, userid);
  stat4.setInt(1, userid);

  if (photo != null) {
    out.print("<td valign='top' align='left'>");

    try {
      out.print("<img src=\"/photos/" + photo + "\" alt=\"" + nick + "\" " + new ImageInfo(tmpl.getObjectConfig().getHTMLPathPrefix() + "photos/" + photo).getCode() + ">");
    } catch (BadImageException ex) {
      out.print("[bad image]");
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
  if (tmpl.isSessionAuthorized(session) && (session.getValue("nick").equals(nick) || 
              ((Boolean)session.getValue("moderator")).booleanValue())) {
            if (sEmail!=null) if (!sEmail.equals("")) 
                out.println("<br><b>Email:</b> " + sEmail + "<br>");
            out.println("<b>Score</b>: "+score+"<br>\n");
  }

  if (user.isBlocked())
    out.println(" (заблокирован)\n");

  out.println("<br>");
  if (tmpl.isModeratorSession() && user.isBlockable()) {
    if (user.isBlocked()) {
      out.print("<form name='f_unblock' method='post' action='whois.jsp'>\n");
      out.print("<input type='hidden' name='update' value='" + nick + "'>\n");
      out.print("<input type='hidden' name='nick' value='" + nick + "'>\n");
      out.print("<input type='submit' name='unblock' value='unblock'>\n");
      out.print("</form>");
    } else {
      out.print("<form name='f_block' method='post' action='whois.jsp'>\n");
      out.print("<input type='hidden' name='update' value='" + nick + "'>\n");
      out.print("<input type='hidden' name='nick' value='" + nick + "'>\n");
      out.print("<input type='submit' name='block' value='block user'>\n</form>");
    }
  }

  userInfo.close();

%>
<br>
<p>
<cite>
<%
    out.print(HTMLFormatter.nl2br(tmpl.getObjectConfig().getStorage().readMessageDefault("userinfo", String.valueOf(userid), "")));
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
