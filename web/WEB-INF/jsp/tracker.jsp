<%@ page info="last active topics" %>
<%@ page contentType="text/html; charset=utf-8"%>
<%@ page import="java.sql.Connection,java.sql.ResultSet,java.sql.Statement,java.sql.Timestamp,ru.org.linux.site.BadInputException,ru.org.linux.site.LorDataSource,ru.org.linux.site.Template,ru.org.linux.util.ServletParameterParser"   buffer="200kb"%>
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
<%@ taglib tagdir="/WEB-INF/tags" prefix="lor" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="fn" uri="http://java.sun.com/jsp/jstl/functions" %>

<%
  Connection db = null;
  try {
    // defaults
    int hours = (Integer) request.getAttribute("hour");
    db = LorDataSource.getConnection();

    String sSuf = "ов";
    
    /* some magic */
    if (hours % 10 < 5 && hours % 10 > 0 && hours % 100 / 10 != 1) {
	    if (hours % 10 == 1) {
		    sSuf = "";
	    } else {
		     sSuf="а";
	    }
    }

    String title = "Последние сообщения за "+hours+" час"+sSuf;
%>

<title><%= title %></title>
<jsp:include page="/WEB-INF/jsp/header.jsp"/>

<form action="tracker.jsp">

  <table class=nav><tr>
    <td align=left valign=middle id="navPath">
      <%= title %>
    </td>

    <td align=right valign=middle>
      за последние
        <input name="h" onChange="submit();" value="<%= hours %>">
      часа

      <input type="submit" value="показать">
    </td>
  </tr>
 </table>
</form>

<h1 class="optional"><%= title %></h1>

<div class=forum>
<table width="100%" class="message-table">
<thead>
<tr>
  <th>Форум</th>
  <th>Заголовок</th>
  <th>Последнее<br>сообщение</th>
  <th>Число ответов<br>всего/день/час</th>
</tr>
</thead>
<tbody>
<c:forEach var="msg" items="${msgs}">

<tr>
  <td>
    <a href="group.jsp?group=${msg.groupId}">
        ${msg.groupTitle}
    </a>
  </td>
  <td>
    <c:if test="${msg.pages>1}">
      <% if (tmpl.getProf().getBoolean("newfirst")) { %>
         <a href="/view-message.jsp?msgid=${msg.msgid}&amp;lastmod=${msg.lastmod.time}">
      <% } else { %>
         <a href="/view-message.jsp?msgid=${msg.msgid}&amp;page=${msg.pages-1}&amp;lastmod=${msg.lastmod.time}">
      <% } %>
    </c:if>
    <c:if test="${msg.pages<=1}">
      <a href="/view-message.jsp?msgid=${msg.msgid}&amp;lastmod=${msg.lastmod.time}">
    </c:if>
      ${msg.title}
    </a>     (<lor:user id="${msg.author}" db="<%= db %>" decorate="true"/>)
  </td>
  <td class="dateinterval">
      <lor:dateinterval date="${msg.lastmod}"/>
      <c:if test="${msg.lastCommentBy != 0}">
        (<lor:user id="${msg.lastCommentBy}" db="<%= db %>" decorate="true"/>)
      </c:if>
  </td>
  <td align='center'>
    <c:if test="${msg.stat1==0}">-</c:if><c:if test="${msg.stat1>0}"><b>${msg.stat1}</b></c:if>/<c:if test="${msg.stat3==0}">-</c:if><c:if test="${msg.stat3>0}"><b>${msg.stat3}</b></c:if>/<c:if test="${msg.stat4==0}">-</c:if><c:if test="${msg.stat4>0}"><b>${msg.stat4}</b></c:if>
</tr>
</c:forEach>
</tbody>

<tfoot>
  <tr><td colspan='4' align='right'>всего: ${fn:length(msgs)} &nbsp</td></tr>
</tfoot>

</table>
</div>

        <%

      if (tmpl.isModeratorSession()) {
%>
<h2>Новые пользователи</h2>
<%
        // new users
      String sSql = "SELECT nick, blocked, activated FROM users where regdate IS NOT null " +
          "AND regdate > CURRENT_TIMESTAMP - interval '3 days' ORDER BY regdate";
      out.println("<P>New users for the last 3 days:");
      Statement st = db.createStatement();
      st.executeQuery(sSql);
      ResultSet rs = st.getResultSet();
      if (rs == null) {
        out.println("no new users.");
      } else {
        int cnt = 0;
        while (rs.next()) {
          String nick = rs.getString("nick");
          boolean blocked = rs.getBoolean("blocked");
          boolean activated = rs.getBoolean("activated");

          if (activated) {
            out.print("<b>");
          }

          if (blocked) {
            out.print("<s>");
          }

          out.print("<a href='/whois.jsp?nick=" + nick + "'>" + nick + "</a>");
          cnt++;

          if (blocked) {
            out.print("</s>");
          }
          
          if (activated) {
            out.print("</b>");
          }

          out.print(", ");
        }
        out.println("(total " + cnt + ')');
      }

      rs.close();
      st.close();
    }
  } finally {
    if (db!=null) {
      db.close();
    }
  }
%>
<jsp:include page="/WEB-INF/jsp/footer.jsp"/>
