<%@ page contentType="text/html; charset=koi8-r"%>
<%@ page import="java.net.URLEncoder,java.sql.Connection,java.sql.ResultSet,java.sql.Statement, ru.org.linux.site.Template" errorPage="error.jsp"%>
<%@ page import="ru.org.linux.site.BadInputException"%>
<%@ page import="ru.org.linux.site.AccessViolationException"%>
<% Template tmpl = new Template(request, config, response); %>
<%= tmpl.head() %>
	<title>Опросы</title>
<%= tmpl.DocumentHeader() %>
<%
  boolean showDeleted =request.getParameter("deleted")!=null;
  if (showDeleted && !"POST".equals(request.getMethod())) {
    response.setHeader("Location", tmpl.getRedirectUrl() + "votes.jsp");
    response.setStatus(HttpServletResponse.SC_MOVED_TEMPORARILY);

    showDeleted = false;
  }

  if (showDeleted) {
    if (!tmpl.isSessionAuthorized(session)) {
      throw new BadInputException("Вы уже вышли из системы");
    }
  }

  if (showDeleted && !tmpl.isModeratorSession()) {
    throw new AccessViolationException("Not moderator");
  }

  Connection db = null;
  try {
%>
<div class=messages>
<div class=nav>
<div class=color1>
  <table width="100%" cellspacing=1 cellpadding=1 border=0>
    <tr class=body>
      <td align=left valign=middle>
        <strong>Опросы</strong>
      </td>

      <td align=right valign=middle>
        [<a style="text-decoration: none" href="add-poll.jsp">Добавить опрос</a>]
      </td>
    </tr>
  </table>
</div>
</div>
</div>


<H1>Опросы</H1>
<ul>
<%
db = tmpl.getConnection("votes");

Statement st=db.createStatement();

ResultSet rs;

  if (tmpl.isModeratorSession()) {
    rs=st.executeQuery("SELECT votenames.id, votenames.title, moderate, deleted, sum(votes) as vs FROM votenames LEFT JOIN votes ON votenames.id=votes.vote GROUP BY votenames.id, votenames.title, moderate, deleted ORDER BY id DESC");
  } else {
    rs=st.executeQuery("SELECT votenames.id, votenames.title, moderate, deleted, sum(votes) as vs FROM votenames, votes WHERE votenames.id=votes.vote AND moderate AND NOT deleted GROUP BY votenames.id, votenames.title, moderate, deleted ORDER BY id DESC");
  }

  while(rs.next()) {
    int id=rs.getInt("id");
    boolean deleted = rs.getBoolean("deleted");
    boolean moderate = rs.getBoolean("moderate");

    if (deleted && !showDeleted) {
      continue;
    }

%> <li>
  <a href="view-vote.jsp?vote=<%= id %>">
  <% if (deleted) {
      out.print("<s>");
    }
  %>

  <%= rs.getString("title") %></a>

  <% if (deleted) {
      out.print("</s>");
    }
  %>

  <%
    if (moderate) {
      out.print("("+rs.getInt("vs")+" голосов)");
    } else if (tmpl.isModeratorSession() && !deleted) {
      out.print(" [<a href=\"commit-vote.jsp?id="+id+"\">Редактировать/Подтвердить</a>]");
    }

    if (!deleted && tmpl.isModeratorSession()) {
      out.print(" [<a href=\"delete-vote.jsp?id="+id+"\">Удалить</a>]");
    }
  %>
  <% } %>
</ul>

<p>
<% if (tmpl.isModeratorSession() &&  !showDeleted) { %>
<hr>
<form action="votes.jsp" method=POST>
<input type=hidden name=deleted value=1>
<input type=submit value="Показать удаленные опросы">
</form>
<hr>
<% } %>

<%
    rs.close();
    st.close();
  } finally {
    if (db!=null) db.close();
  }
%>
<%= tmpl.DocumentFooter() %>
