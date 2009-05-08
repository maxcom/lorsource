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

<%
  Connection db = null;
  try {
    // defaults
    int hours = 3;

    if (request.getParameter("h") != null) {
      hours = new ServletParameterParser(request).getInt("h");

      if (hours < 1 || hours > 23) {
        throw new BadInputException("неправильный ввод. hours = " + hours);
      }
    }

    // active topics for last xx hours
    db = LorDataSource.getConnection();

    String sSql = "SELECT nick, t.id, lastmod, t.stat1 AS stat1, t.stat3 AS stat3, t.stat4 AS stat4, g.id AS gid, g.title AS gname, t.title AS title FROM users AS u, topics AS t, groups AS g, (SELECT distinct topic FROM comments WHERE postdate > CURRENT_TIMESTAMP - interval '" + hours + " hours' UNION SELECT id FROM topics WHERE postdate > CURRENT_TIMESTAMP - interval '" + hours + " hours') AS foo WHERE t.userid=u.id AND not deleted AND t.id=foo.topic AND t.groupid=g.id ORDER BY lastmod DESC";

    Statement st = db.createStatement();
    ResultSet rs = st.executeQuery(sSql);

    out.println("<!-- hours = " + hours + " -->");

/*

    (hours % 100 / 10) == 1  => 2  (( 11, 12, 13, 14))
    (hours % 10) == 1        => 0
    (hours % 10) == 2 .. 4   => 1
    else                     => 2

*/

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
<jsp:include page="WEB-INF/jsp/header.jsp"/>

<form action="tracker.jsp">

  <table class=nav><tr>
    <td align=left valign=middle>
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

<h1><%= title %></h1>

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
<%
  int cnt = 0;

  while (rs.next()) {
    Timestamp lastmod = rs.getTimestamp("lastmod");

    if (lastmod == null) {
      lastmod = new Timestamp(0);
    }

    int itotal = Integer.parseInt(rs.getString("stat1"));
%>
<tr>
  <td>
    <a href="group.jsp?group=<%= rs.getString("gid") %> + ">
        <%= rs.getString("gname") %></a>
  </td>
  <td>
<%
    String sTemp = "";

    int messages = tmpl.getProf().getInt("messages");

    if (itotal > messages) {
      // itotal = round(ceil(itotal/50));
      if (!tmpl.getProf().getBoolean("newfirst")) {
        itotal = itotal / messages;
        sTemp = "&page=" + itotal;
      }
    }
%>
    <a href="view-message.jsp?msgid=<%= rs.getString("id") %>&amp;lastmod=<%= lastmod.getTime() + sTemp %>">
      <%= rs.getString("title")%>
    </a> (<%= rs.getString("nick") %>)
  </td>
  <td>
    <lor:dateinterval date="<%= lastmod %>"/>
  </td>
  <td align='center'><%=
        rs.getString("stat1") + '/' +
        rs.getString("stat3") + '/' +
        rs.getString("stat4") %>
  </td>
</tr>
<%
    cnt++;
  }
%>

</tbody>

<tfoot>
  <tr><td colspan='4' align='right'>всего: <%= cnt %> &nbsp</td></tr>
</tfoot>

</table>
</div>

        <%

/*        // users for last 10 minutes
        sSql = "SELECT id,nick FROM users WHERE lastlogin > CURRENT_TIMESTAMP - interval '10 minutes' ORDER BY nick";
        st = db.createStatement();
        st.executeQuery(sSql); rs = st.getResultSet();
        out.print("<br>Users seen on site for last 10 minutes: ");
        cnt=0;

        while (rs.next()) {
            nick = rs.getString("nick");
            out.println("<a href='/whois.jsp?nick="+nick+"'>"+nick+"</a> , ");
            cnt++;
        }
        out.println("(total "+cnt+")");

        rs.close();
*/
      if (tmpl.isModeratorSession()) {
%>
<h2>Новые пользователи</h2>
<%
        // new users
      sSql = "SELECT nick, blocked, activated FROM users where regdate IS NOT null " +
          "AND regdate > CURRENT_TIMESTAMP - interval '3 days' ORDER BY regdate";
      out.println("<P>New users for the last 3 days:");
      cnt = 0;
      st.executeQuery(sSql);
      rs = st.getResultSet();
      if (rs == null) {
        out.println("no new users.");
      } else {
        while (rs.next()) {
          String nick = rs.getString("nick");
          boolean blocked = rs.getBoolean("blocked");
          boolean activated = rs.getBoolean("activated");

          if (blocked) {
            out.print("<s>");
          }

          if (activated) {
            out.print("<b>");
          }

          out.print("<a href='/whois.jsp?nick=" + nick + "'>" + nick + "</a> , ");
          cnt++;

          if (activated) {
            out.print("</b>");
          }

          if (blocked) {
            out.print("</s>");
          }
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
<jsp:include page="WEB-INF/jsp/footer.jsp"/>
