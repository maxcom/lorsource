<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ page contentType="text/html; charset=utf-8"%>
<%@ page import="java.net.URLEncoder,java.sql.Connection,java.sql.ResultSet,java.sql.Statement,java.util.Date"   buffer="60kb"%>
<%@ page import="ru.org.linux.site.LorDataSource" %>
<%@ page import="ru.org.linux.site.Section" %>
<%@ page import="ru.org.linux.site.User" %>
<%@ taglib tagdir="/WEB-INF/tags" prefix="lor" %>
<%--
  ~ Copyright 1998-2010 Linux.org.ru
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
<jsp:include page="/WEB-INF/jsp/head.jsp"/>

<%
  Connection db = null;
  try {
    response.setDateHeader("Expires", new Date(new Date().getTime() - 20 * 3600 * 1000).getTime());
    response.setDateHeader("Last-Modified", new Date(new Date().getTime() - 120 * 1000).getTime());

    db = LorDataSource.getConnection();

    Section section = (Section) request.getAttribute("section");
    int sectionid = 0;
    if (section!=null) {
      sectionid=section.getId();
    }

%>
<title>Просмотр неподтвержденных сообщений - <%= section==null?"Все":section.getName() %></title>
<jsp:include page="/WEB-INF/jsp/header.jsp"/>

  <form action="view-all.jsp">

  <table class=nav><tr>
    <td align=left valign=middle id="navPath">
      Просмотр неподтвержденных сообщений - <%= section==null?"Все":section.getName() %>
    </td>

    <td align=right valign=middle>
      [<a style="text-decoration: none" href="rules.jsp">Правила форума</a>]
      [<a style="text-decoration: none" href="tags.jsp">Метки</a>]

      <select name=section onChange="submit();" title="Быстрый переход">
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

<h1 class="optional"><%= section==null?"П":(section.getName()+": п") %>росмотр неподтвержденных</h1>
<strong>Внимание!</strong> Cообщения отображаются точно так же, как
они будут выглядеть на главной странице. Если ваше сообщение отображается не так, как вы хотели, или
в нем не работают какие-либо ссылки, пожалуйста,
исправьте его.<p>
<strong>Внимание модераторам!</strong> Не подтверждайте сразу
много скриншотов, дайте им повисеть на главной странице.<p>
<%
  User currentUser = User.getCurrentUser(db, session);
%>
<c:forEach var="msg" items="${messages}">
  <lor:news
          db="<%= db %>"
          message="${msg}"
          multiPortal="<%= sectionid==0 %>"
          moderateMode="true"
          currentUser="<%= currentUser %>"/>
</c:forEach>
<%
  Statement st = db.createStatement();
  ResultSet rs;

  if (sectionid == 0) {
    rs = st.executeQuery("SELECT topics.title as subj, nick, groups.section, groups.title as gtitle, topics.id as msgid, groups.id as guid, sections.name as ptitle, reason FROM topics,groups,users,sections,del_info WHERE sections.id=groups.section AND topics.userid=users.id AND topics.groupid=groups.id AND sections.moderate AND deleted AND del_info.msgid=topics.id AND topics.userid!=del_info.delby AND delDate is not null ORDER BY del_info.delDate DESC LIMIT 20");
  } else {
    rs = st.executeQuery("SELECT topics.title as subj, nick, groups.section, groups.title as gtitle, topics.id as msgid, groups.id as guid, sections.name as ptitle, reason FROM topics,groups,users,sections,del_info WHERE sections.id=groups.section AND topics.userid=users.id AND topics.groupid=groups.id AND sections.moderate AND deleted AND del_info.msgid=topics.id AND topics.userid!=del_info.delby AND delDate is not null AND section=" + sectionid + " ORDER BY del_info.delDate DESC LIMIT 20");
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
%>
<tr>
  <td align="center">
    <c:if test="${template.moderatorSession}">
      <a href="/undelete.jsp?msgid=<%= msgid %>" title="Восстановить">#</a>
    </c:if>
  </td>
  <td><a href="/people/<%= URLEncoder.encode(nick) %>/profile"><%=nick %></a></td>
  <td><a href="group.jsp?group=<%= guid %>">
      <%
	out.print(rs.getString("ptitle")+" - " + rs.getString("gtitle")+"");
%>
    </a></td>
<%
	out.print("<td><a href=\"view-message.jsp?msgid="+msgid+"\">"+rs.getString("subj")+"</a></td>");
	out.print("<td>"+rs.getString("reason")+"</td>");
%>
</tr>
  <%
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
<jsp:include page="/WEB-INF/jsp/footer.jsp"/>
