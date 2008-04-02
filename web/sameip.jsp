<%@ page pageEncoding="koi8-r" contentType="text/html; charset=utf-8"%>
<%@ page import="java.sql.Connection,java.sql.ResultSet,java.sql.Statement,java.sql.Timestamp"   buffer="60kb" %>
<%@ page import="ru.org.linux.site.*"%>
<%@ page import="ru.org.linux.util.HTMLFormatter"%>
<%@ page import="ru.org.linux.util.ServletParameterParser"%>
<%@ page import="ru.org.linux.util.StringUtil" %>
<% Template tmpl = new Template(request, config.getServletContext(), response); %>
<%= tmpl.getHead() %>
<%
  if (!Template.isSessionAuthorized(session) || !((Boolean) session.getValue("moderator"))) {
    throw new IllegalAccessException("Not authorized");
  }

%>
<title>Поиск писем с IP-адреса</title>
<jsp:include page="WEB-INF/jsp/header.jsp"/>
<% Connection db = null;
  try {
%>

<%
  db = LorDataSource.getConnection();

  String ip;

  if (request.getParameter("msgid") != null) {
    Statement ipst = db.createStatement();
    int msgid = new ServletParameterParser(request).getInt("msgid");

    ResultSet rs = ipst.executeQuery("SELECT postip FROM topics WHERE id=" + msgid);

    if (!rs.next()) {
      rs.close();
      rs = ipst.executeQuery("SELECT postip FROM comments WHERE id=" + msgid);
      if (!rs.next()) {
        throw new MessageNotFoundException(msgid);
      }
    }

    ip = rs.getString("postip");

    if (ip == null) {
      throw new ScriptErrorException("No IP data for #" + msgid);
    }

    rs.close();
  } else {
    ip = new ServletParameterParser(request).getIP("ip");
  }

%>
<table class=nav><tr>
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
			</td>   </tr>

			</table>

<h1 align="center">Сообщения с <%= ip %> (за 24 часа)</h1>

<strong>Текущий статус: </strong>

<%
  if (IPBlockInfo.getTor(ip)) {
    out.print("адрес заблокирован: tor.ahbl.org; база: ");
  }

  IPBlockInfo blockInfo = IPBlockInfo.getBlockInfo(db, ip);

  if (blockInfo == null) {
    out.print("адрес не заблокирован");
  } else {
    Timestamp banDate = blockInfo.getBanDate();
    User moderator = User.getUser(db, blockInfo.getModeratorId());

    if (banDate == null) {
      out.print("адрес заблокирован постоянно");
    } else {
      out.print("адрес заблокирован до " + Template.dateFormat.format(banDate));
      if (!blockInfo.isBlocked()) {
        out.print(" (блокировка истекла)");
      }
    }

    out.print("<br><strong>Причина блокировки: </strong>" + HTMLFormatter.htmlSpecialChars(blockInfo.getReason()));
    out.print("<br><strong>Дата блокировки: </strong>" + Template.dateFormat.format(blockInfo.getOriginalDate()));
    out.print("<br><strong>Адрес блокирован: </strong>" + HTMLFormatter.htmlSpecialChars(moderator.getNick()));
  }
%>

<p>

<form method="post" action="banip.jsp">
<input type="hidden" name="ip" value="<%= ip %>">
забанить/разбанить IP по причине: <br>
<input type="text" name="reason" maxlength="254" size="40" value=""><br>
<select name="time" onchange="checkCustomBan(this.selectedIndex)">
<option value="hour">1 час</option>
<option value="day">1 день</option>
<option value="month">1 месяц</option>
<option value="3month">3 месяца</option>
<option value="6month">6 месяцев</option>
<option value="unlim">постоянно</option>
<option value="remove">не блокировать</option>
<option value="custom">указать (дней)</option>
</select>
<div id="custom_ban" style="display:none;">
<br><input type="text" name="ban_days" value="">
</div>
<p>
<input type="submit" name="ban" value="ban ip">
<script type="text/javascript">
<!--
function checkCustomBan(idx) {
  var custom_ban_div = document.getElementById('custom_ban');
  if (custom_ban_div==null || typeof(custom_ban_div)!="object") {
    return;
  }
  if (idx!=7) {
    custom_ban_div.style.display='none';
  } else {
    custom_ban_div.style.display='block';
  }
}
// -->
</script>
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
  while (rs.next()) {
    out.print("<tr class=color2><td>" + rs.getString("ptitle") + "</td><td>" + rs.getString("gtitle") + "</td><td><a href=\"view-message.jsp?msgid=" + rs.getInt("msgid") + "\" rev=contents>" + StringUtil.makeTitle(rs.getString("title")) + "</a></td><td>" + Template.dateFormat.format(rs.getTimestamp("postdate")) + "</td></tr>");
  }

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
  while (rs.next()) {
    out.print("<tr class=color2><td>" + rs.getString("ptitle") + "</td><td>" + rs.getString("gtitle") + "</td><td><a href=\"jump-message.jsp?msgid=" + rs.getInt("topicid") + "&amp;cid=" + rs.getInt("msgid") + "\" rev=contents>" + StringUtil.makeTitle(rs.getString("title")) + "</a></td><td>" + Template.dateFormat.format(rs.getTimestamp("postdate")) + "</td></tr>");
  }

  rs.close();
  st.close();

%>

</table>
</td></tr></table>
</div>
<%
  } finally {
    if (db!=null) {
      db.close();
    }
  }
%>
<jsp:include page="WEB-INF/jsp/footer.jsp"/>
