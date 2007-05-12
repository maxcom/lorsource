<%@ page contentType="text/html; charset=koi8-r"%>
<%@ page import="java.sql.Connection,java.sql.ResultSet,java.sql.Statement,java.sql.Timestamp" errorPage="error.jsp" buffer="60kb" %>
<%@ page import="ru.org.linux.site.*"%>
<%@ page import="ru.org.linux.util.HTMLFormatter"%>
<%@ page import="ru.org.linux.util.StringUtil"%>
<% Template tmpl = new Template(request, config, response); %>
<%= tmpl.head() %>
<%
   if (!tmpl.isSessionAuthorized(session) || !(((Boolean) session.getValue("moderator")).booleanValue())) {
     throw new IllegalAccessException("Not authorized");
   }

%>
<title>Поиск писем с IP-адреса</title>
<%= tmpl.DocumentHeader() %>
<% Connection db = null;
  try {
%>

<%
	db = tmpl.getConnection("sameip");

	final String ip;

	if (request.getParameter("msgid")!=null) {
          Statement ipst = db.createStatement();
   	  int msgid = tmpl.getParameters().getInt("msgid");

          ResultSet rs = ipst.executeQuery("SELECT postip FROM topics WHERE id=" + msgid);

          if (!rs.next()) {
            rs.close();
            rs = ipst.executeQuery("SELECT postip FROM comments WHERE id="+msgid);
            if (!rs.next())
              throw new MessageNotFoundException(msgid);
          }

          ip = rs.getString("postip");

          if (ip==null) {
            throw new ScriptErrorException("No IP data for #"+msgid);
          }

          rs.close();
	} else {
	  ip = tmpl.getParameters().getIP("ip");
        }

%>
<div class=messages>
<div class=nav>
<table width="100%"><tr class=color1><td><table width="100%" cellspacing=1 cellpadding=1 border=0><tr class=body>
			<td align=left valign=middle>
			<strong>Интерфейс модератора - Сообщения с <%= ip %></strong>
			</td>

			<td align=right valign=middle>

[<a href="http://www.radio-msu.net/serv/wwwnslookup/nph-wwwtr.cgi?server=<%= ip%>">NSLOOKUP</a>] [WHOIS
<% 
      // URLs ripped off from ACID snort project with corrections
      out.print("<a href='http://www.ripe.net/perl/whois?query="+ip+"'>RIPE</a> / "); 
      out.print("<a href='http://ws.arin.net/whois/?queryinput="+ip+"'>ARIN</a> / ");
      out.print("<a href='http://www.apnic.net/apnic-bin/whois.pl?search="+ip+"'>APNIC</a> / ");
      out.print("<a href='http://lacnic.net/cgi-bin/lacnic/whois?lg=EN&query="+ip+"'>LACNIC</a>\n");
%>
]
			</td>

			</table></td>

</tr></table></td></tr></table>
</div>
</div>

<h1 align="center">Сообщения с <%= ip %> (за 24 часа)</h1>

<strong>Текущий статус: </strong>

<%
  IPBlockInfo blockInfo = IPBlockInfo.getBlockInfo(db, ip);

  if (blockInfo==null) {
    out.print("адрес не заблокирован");
  } else {
    Timestamp banDate = blockInfo.getBanDate();
    User moderator = new User(db, blockInfo.getModeratorId());

    if (banDate==null) {
      out.print("адрес заблокирован постоянно");
    } else {
      out.print("адрес заблокирован до "+Template.dateFormat.format(banDate));
      if (!blockInfo.isBlocked()) {
        out.print(" (блокировка истекла)");
      }
    }

    out.print("<br><strong>Причина блокировки: </strong>"+HTMLFormatter.htmlSpecialChars(blockInfo.getReason()));
    out.print("<br><strong>Дата блокировки: </strong>"+Template.dateFormat.format(blockInfo.getOriginalDate()));
    out.print("<br><strong>Адрес блокирован: </strong>"+HTMLFormatter.htmlSpecialChars(moderator.getNick()));
  }
%>

<p>

<form method="post" action="banip.jsp">
<input type="hidden" name="ip" value="<%= ip %>">
забанить/разбанить IP по причине: <br>
<input type="text" name="reason" maxlength="254" size="40" value=""><br>
<select name="time">
<option value="hour">1 час</option>
<option value="day">1 день</option>
<option value="month">1 месяц</option>
<option value="3month">3 месяца</option>
<option value="6month">6 месяцев</option>
<option value="unlim">постоянно</option>
<option value="remove">не блокировать</option>
</select>
<p>
<input type="submit" name="ban" value="ban ip">
</form>

<h2>Темы</h2>

<div class=forum width="100%">
<table>
<tr class=color1><td>
<table width="100%" cellspacing=1 cellpadding=0 border=0>
<thead>
<tr class=color1><th>Раздел</th><th>Группа</th><th>Заглавие</th><th>Дата</th></tr>
<tbody>
<%

  Statement st=db.createStatement();
  ResultSet rs=st.executeQuery("SELECT sections.name as ptitle, groups.title as gtitle, topics.title as title, topics.id as msgid, postdate FROM topics, groups, sections, users WHERE topics.groupid=groups.id AND sections.id=groups.section AND users.id=topics.userid AND topics.postip='"+ip+"' AND postdate>CURRENT_TIMESTAMP-'24 hour'::interval ORDER BY msgid DESC");
  while (rs.next())
	out.print("<tr class=color2><td>"+rs.getString("ptitle")+"</td><td>"+rs.getString("gtitle")+"</td><td><a href=\"view-message.jsp?msgid="+rs.getInt("msgid")+"\" rev=contents>"+StringUtil.makeTitle(rs.getString("title"))+"</a></td><td>"+Template.dateFormat.format(rs.getTimestamp("postdate"))+"</td></tr>");

  rs.close();
  st.close();

%>
</table>
</td></tr></table>
</div>
<h2>Комментарии</h2>

<div class=forum width="100%">
<table>
<tr class=color1><td>
<table width="100%" cellspacing=1 cellpadding=0 border=0>
<thead>
<tr class=color1><th>Раздел</th><th>Группа</th><th>Заглавие темы</th><th>Дата</th></tr>
<tbody>
<%

  st=db.createStatement();
  rs=st.executeQuery("SELECT sections.name as ptitle, groups.title as gtitle, topics.title, topics.id as topicid, comments.id as msgid, comments.postdate FROM sections, groups, topics, comments WHERE sections.id=groups.section AND groups.id=topics.groupid AND comments.topic=topics.id AND comments.postip='"+ip+"' AND comments.postdate>CURRENT_TIMESTAMP-'24 hour'::interval ORDER BY postdate DESC;");
  while (rs.next())
	out.print("<tr class=color2><td>"+rs.getString("ptitle")+"</td><td>"+rs.getString("gtitle")+"</td><td><a href=\"jump-message.jsp?msgid="+rs.getInt("topicid")+'#'+rs.getInt("msgid")+"\" rev=contents>"+StringUtil.makeTitle(rs.getString("title"))+"</a></td><td>"+Template.dateFormat.format(rs.getTimestamp("postdate"))+"</td></tr>");

  rs.close();
  st.close();

%>

</table>
</td></tr></table>
</div>
<%
  } finally {
    if (db!=null) db.close();
  }
%>
<%= tmpl.DocumentFooter() %>
