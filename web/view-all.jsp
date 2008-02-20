<%@ page pageEncoding="koi8-r" contentType="text/html; charset=utf-8"%>
<%@ page import="java.net.URLEncoder,java.sql.Connection,java.sql.ResultSet,java.sql.Statement,java.util.Date,ru.org.linux.site.NewsViewer" errorPage="/error.jsp" buffer="60kb"%>
<%@ page import="ru.org.linux.site.Section"%>
<%@ page import="ru.org.linux.site.Template" %>
<%@ page import="ru.org.linux.util.ServletParameterParser" %>
<% Template tmpl = new Template(request, config, response); %>
<%= tmpl.head() %>
<%
  Connection db = null;
  try {

    response.setDateHeader("Expires", new Date(new Date().getTime() - 20 * 3600 * 1000).getTime());
    response.setDateHeader("Last-Modified", new Date(new Date().getTime() - 120 * 1000).getTime());

    db = tmpl.getConnection();

    int sectionid = 0;
    Section section = null;

    if (request.getParameter("section") != null) {
      sectionid = new ServletParameterParser(request).getInt("section");
      if (sectionid != 0) {
        section = new Section(db, sectionid);
      }
    }

%>
<title>Просмотр неподтвержденных сообщений - <%= section==null?"Все":section.getName() %></title>
<%= tmpl.DocumentHeader() %>

  <form action="view-all.jsp">

  <table class=nav><tr>
    <td align=left valign=middle>
      Просмотр неподтвержденных сообщений - <%= section==null?"Все":section.getName() %>
    </td>

    <td align=right valign=middle>
      [<a style="text-decoration: none" href="rules.jsp">Правила форума</a>]

      <select name=section onChange="submit()" title="Быстрый переход">
        <option value=0>Все</option>
        <%
                Statement sectionListSt = db.createStatement();
                ResultSet sectionList = sectionListSt.executeQuery("SELECT id, name FROM sections WHERE moderate order by id");

                while (sectionList.next()) {
                        int id = sectionList.getInt("id");
        %>
                <option value=<%= id %> <%= id==sectionid?"selected":"" %> ><%= sectionList.getString("name") %></option>
        <%
                }

                sectionList.close();
                sectionListSt.close();
        %>

      </select>
    </td>

  </tr>
 </table>
</form>

<h1><%= section==null?"П":(section.getName()+": п") %>росмотр неподтвержденных</h1>
<strong>Внимание!</strong> сообщения отображаются точно также, как
они будут выглядеть на главной странице (за исключением раздела <em>ссылок</em>). Если ваше сообщение отображается не так, как вы хотели, или
в нем не работают какие-либо ссылки, пожалуйста,
исправьте его и пошлите заново (и, в идеале, самостоятельно удалите
старую версию).<p>
<strong>Внимание модераторам!</strong> Не подтверждайте сразу
много скриншотов, дайте им повисеть на главной странице.<p>
<%

  Statement st = db.createStatement();

//  ResultSet rs;

//  if (sectionid != 0) {
//    rs = st.executeQuery("SELECT topics.title as subj, lastmod, postdate, nick, image, groups.title as gtitle, topics.id as msgid, sections.comment, groups.id as guid, topics.url, topics.linktext, imagepost, vote, sections.name as pname, linkup, postdate<(CURRENT_TIMESTAMP-expire) as expired, message FROM topics,groups,users,sections, msgbase WHERE sections.id=groups.section AND topics.userid=users.id AND topics.groupid=groups.id AND topics.id=msgbase.id AND (NOT topics.moderate) AND sections.moderate AND NOT deleted AND section=" + sectionid + " AND postdate>(CURRENT_TIMESTAMP-'1 month'::interval) ORDER BY msgid DESC");
//  } else {
//    rs = st.executeQuery("SELECT topics.title as subj, lastmod, postdate, nick, image, groups.title as gtitle, topics.id as msgid, sections.comment, groups.id as guid, topics.url, topics.linktext, imagepost, vote, sections.name as pname, linkup, postdate<(CURRENT_TIMESTAMP-expire) as expired, message FROM topics,groups,users,sections, msgbase WHERE sections.id=groups.section AND topics.userid=users.id AND topics.groupid=groups.id AND topics.id=msgbase.id AND (NOT topics.moderate) AND sections.moderate AND NOT deleted AND postdate>(CURRENT_TIMESTAMP-'1 month'::interval) ORDER BY msgid DESC");
//  }

  NewsViewer nw = new NewsViewer(tmpl.getConfig(), tmpl.getProf());
  nw.setViewAll(true);
  nw.setDatelimit("postdate>(CURRENT_TIMESTAMP-'1 month'::interval)");
  if (sectionid != 0) {
    nw.setSection(sectionid);
  }

  out.print(nw.show(db));

  ResultSet rs;

  if (sectionid == 0) {
    rs = st.executeQuery("SELECT topics.title as subj, nick, groups.title as gtitle, topics.id as msgid, groups.id as guid, sections.name as ptitle, reason FROM topics,groups,users,sections,del_info WHERE sections.id=groups.section AND topics.userid=users.id AND topics.groupid=groups.id AND sections.moderate AND deleted AND del_info.msgid=topics.id AND topics.userid!=del_info.delby ORDER BY msgid DESC LIMIT 20;");
  } else {
    rs = st.executeQuery("SELECT topics.title as subj, nick, groups.title as gtitle, topics.id as msgid, groups.id as guid, sections.name as ptitle, reason FROM topics,groups,users,sections,del_info WHERE sections.id=groups.section AND topics.userid=users.id AND topics.groupid=groups.id AND sections.moderate AND deleted AND del_info.msgid=topics.id AND topics.userid!=del_info.delby AND section=" + sectionid + " ORDER BY msgid DESC LIMIT 20;");
  }
%>
<h2>Последние удаленные неподтвержденные</h2>
<div class=forum>
<div class=color1>
<table width="100%" cellspacing=1 cellpadding=0 border=0>
<thead>
<tr class=color1><th>Автор</th><th>Группа</th><th>Заголовок</th><th>Причина удаления</th></tr>
<tbody>

<%
  while (rs.next()) {
  	String nick=rs.getString("nick");
	int msgid=rs.getInt("msgid");
	int guid=rs.getInt("guid");
	out.print("<tr class=color2>");
	out.print("<td><a href=\"whois.jsp?nick="+URLEncoder.encode(nick)+"\">"+nick+"</a></td>");
	out.print("<td><a href=\"group.jsp?group="+guid+"\">"+rs.getString("ptitle")+" - " + rs.getString("gtitle")+"</a></td>");
	out.print("<td><a href=\"view-message.jsp?msgid="+msgid+"\">"+rs.getString("subj")+"</a></td>");
	out.print("<td>"+rs.getString("reason")+"</td>");

	out.print("</tr>");

  }
%>
</table>
</div>
</div>

<%
  rs.close();
  st.close();
  } finally {
    if (db!=null) {
      db.close();
    }
  }
%>
<%=	tmpl.DocumentFooter() %>
