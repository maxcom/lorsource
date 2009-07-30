<%@ page contentType="text/html; charset=utf-8"%>
<%@ page import="java.net.URLEncoder,java.sql.Connection,java.sql.ResultSet,java.sql.Statement,java.util.Date,ru.org.linux.site.LorDataSource"   buffer="60kb"%>
<%@ page import="ru.org.linux.site.NewsViewer"%>
<%@ page import="ru.org.linux.site.Section" %>
<%@ page import="ru.org.linux.site.Template" %>
<%@ page import="ru.org.linux.util.ServletParameterParser" %>
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
<jsp:include page="/WEB-INF/jsp/head.jsp"/>

<%
  Connection db = null;
  try {

    response.setDateHeader("Expires", new Date(new Date().getTime() - 20 * 3600 * 1000).getTime());
    response.setDateHeader("Last-Modified", new Date(new Date().getTime() - 120 * 1000).getTime());

    db = LorDataSource.getConnection();

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
<jsp:include page="WEB-INF/jsp/header.jsp"/>

  <form action="view-all.jsp">

  <table class=nav><tr>
    <td align=left valign=middle>
      Просмотр неподтвержденных сообщений - <%= section==null?"Все":section.getName() %>
    </td>

    <td align=right valign=middle>
      [<a style="text-decoration: none" href="rules.jsp">Правила форума</a>]
      [<a style="text-decoration: none" href="tags.jsp">Метки</a>]

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
<strong>Внимание!</strong> Cообщения отображаются точно так же, как
они будут выглядеть на главной странице. Если ваше сообщение отображается не так, как вы хотели, или
в нем не работают какие-либо ссылки, пожалуйста,
исправьте его и пошлите заново (и, в идеале, самостоятельно удалите
старую версию).<p>
<strong>Внимание модераторам!</strong> Не подтверждайте сразу
много скриншотов, дайте им повисеть на главной странице.<p>
<%

  Statement st = db.createStatement();

  NewsViewer nw = new NewsViewer(tmpl.getConfig(), tmpl.getProf());
  nw.setViewAll(true);
  nw.setDatelimit("postdate>(CURRENT_TIMESTAMP-'1 month'::interval)");
  if (sectionid != 0) {
    nw.setSection(sectionid);
  }

  out.print(nw.show(db));

  ResultSet rs;

  if (sectionid == 0) {
    rs = st.executeQuery("SELECT topics.title as subj, nick, groups.section, groups.title as gtitle, topics.id as msgid, groups.id as guid, sections.name as ptitle, reason FROM topics,groups,users,sections,del_info WHERE sections.id=groups.section AND topics.userid=users.id AND topics.groupid=groups.id AND sections.moderate AND deleted AND del_info.msgid=topics.id AND topics.userid!=del_info.delby ORDER BY msgid DESC LIMIT 20;");
  } else {
    rs = st.executeQuery("SELECT topics.title as subj, nick, groups.section, groups.title as gtitle, topics.id as msgid, groups.id as guid, sections.name as ptitle, reason FROM topics,groups,users,sections,del_info WHERE sections.id=groups.section AND topics.userid=users.id AND topics.groupid=groups.id AND sections.moderate AND deleted AND del_info.msgid=topics.id AND topics.userid!=del_info.delby AND section=" + sectionid + " ORDER BY msgid DESC LIMIT 20;");
  }
%>
<h2>Последние удаленные неподтвержденные</h2>
<div class=forum>
<table class="message-table" width="100%">
<thead>
<tr><th>&nbsp;<a name="undelete" title="Восстановить">#</a>&nbsp;</th><th>Автор</th><th>Группа</th><th>Заголовок</th><th>Причина удаления</th></tr>
<tbody>

<%
  while (rs.next()) {
  	String nick=rs.getString("nick");
	int msgid=rs.getInt("msgid");
	int guid=rs.getInt("guid");
	out.print("<tr>");
	out.print("<td align=\"center\"><a href=\"undelete.jsp?msgid="+msgid+"\" title=\"Восстановить\">#</a></td>");
	out.print("<td><a href=\"whois.jsp?nick="+URLEncoder.encode(nick)+"\">"+nick+"</a></td>");
	out.print("<td><a href=\"group.jsp?group="+guid+"\">"+rs.getString("ptitle")+" - " + rs.getString("gtitle")+"</a></td>");
	out.print("<td><a href=\"view-message.jsp?msgid="+msgid+"\">"+rs.getString("subj")+"</a></td>");
	out.print("<td>"+rs.getString("reason")+"</td>");

	out.print("</tr>");

  }
%>
</table>
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
<jsp:include page="WEB-INF/jsp/footer.jsp"/>
