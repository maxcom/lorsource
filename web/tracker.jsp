<%@ page info="last active topics" %>
<%@ page contentType="text/html; charset=koi8-r"%>
<%@ page import="java.sql.Connection,java.sql.ResultSet,java.sql.Statement,java.sql.Timestamp,java.text.DateFormat,java.text.SimpleDateFormat,ru.org.linux.site.BadInputException,ru.org.linux.site.Template" errorPage="/error.jsp" buffer="200kb"%>
<% Template tmpl = new Template(request, config, response); %>
<%= tmpl.head() %>
<%
    Connection db = null;
    try {
        // defaults
      int hours=3;

      if (request.getParameter("h")!=null) {
         hours = tmpl.getParameters().getInt("h");

         if (hours<1 || hours>23) {
          throw new BadInputException("неправильный ввод. hours = "+hours);
         }
      }

      // active topics for last xx hours
      db = tmpl.getConnection("tracker");

      String sSql="SELECT nick, t.id, lastmod, CURRENT_TIMESTAMP-lastmod AS backtime, t.stat1 AS stat1, t.stat3 AS stat3, t.stat4 AS stat4, g.id AS gid, g.title AS gname, t.title AS title FROM users AS u, topics AS t, groups AS g, (SELECT distinct topic FROM comments WHERE postdate > CURRENT_TIMESTAMP - interval '"+hours+" hours' UNION SELECT id FROM topics WHERE postdate > CURRENT_TIMESTAMP - interval '"+hours+" hours') AS foo WHERE t.userid=u.id AND not deleted AND t.id=foo.topic AND t.groupid=g.id ORDER BY lastmod DESC";

      Statement st=db.createStatement();
      ResultSet rs=st.executeQuery(sSql);

      out.println("<!-- hours = "+hours+" -->");

      String title = "Последние сообщения";

%>
<title><%= title %></title>
<%= tmpl.DocumentHeader() %>

<form action="tracker.jsp">

  <table class=nav><tr>
    <td align=left valign=middle>
      <%= title %>
    </td>

    <td align=right valign=middle>
      за последние
        <input name="h" onChange="submit()" value="<%= hours %>">
      часа

      <input type="submit" value="показать">
    </td>
  </tr>
 </table>
</form>

<h1><%= title %> за <%= hours %> часа</h1>

<div class=forum>
<table width="100%" class="message-table">
<thead>
<tr>
  <th>Форум</th>
  <th>Заголовок</th>
  <th>Последнее сообщение</th>
  <th>Число ответов<br>всего/день/час</th>
</tr>
</thead>
<tbody>
<%
  DateFormat rfc822 = new SimpleDateFormat("HH:mm:ss 'назад'");

  int cnt = 0;

  while (rs.next()) {
    Timestamp lastmod = rs.getTimestamp("lastmod");

    if (lastmod == null) {
      lastmod = new Timestamp(0);
    }

    int itotal = Integer.parseInt(rs.getString("stat1"));

    out.print("<tr><td>" +
        "<a href='group.jsp?group=" + rs.getString("gid") + "'>" +
        rs.getString("gname") + "</a>" +
        "</td><td>" +
        "<a href='view-message.jsp?msgid=" + rs.getString("id"));

    String sTemp = "";

    int messages = tmpl.getProf().getInt("messages");

    if (itotal > messages) {
      // itotal = round(ceil(itotal/50));
      if (!tmpl.getProf().getBoolean("newfirst")) {
        itotal = itotal / messages;
        sTemp = "&page=" + itotal;
      }
    }

    out.println("&lastmod=" + lastmod.getTime() + sTemp +
        "'>" + rs.getString("title") +
        "</a> (" + rs.getString("nick") + ')' +
        "</td><td align='center'>" +
        rfc822.format(rs.getTimestamp("backtime")) +
        "</td><td align='center'>" +
        rs.getString("stat1") + '/' +
        rs.getString("stat3") + '/' +
        rs.getString("stat4") +
        "</td></tr>");
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
<%= tmpl.DocumentFooter() %>
