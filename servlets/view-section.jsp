<%@ page contentType="text/html; charset=koi8-r"%>
<%@ page import="java.sql.Connection,java.sql.ResultSet,java.sql.Statement,java.util.Date,ru.org.linux.site.BadSectionException" errorPage="error.jsp" %>
<%@ page import="ru.org.linux.site.Template"%>
<% Template tmpl = new Template(request, config, response);%>
<%= tmpl.head() %>
<%

  response.setDateHeader("Expires", new Date(new Date().getTime()-20*3600*1000).getTime());
  response.setDateHeader("Last-Modified", new Date(new Date().getTime()-2*1000).getTime());

  int section = tmpl.getParameters().getInt("section");

  Connection db = null;
  try {

        db = tmpl.getConnection("view-section");
        Statement st=db.createStatement();

        ResultSet rs=st.executeQuery("SELECT name, browsable,linkup FROM sections WHERE id="+section);
        if (!rs.next()) throw new BadSectionException();
        if (!rs.getBoolean("browsable")) throw new BadSectionException();

        String name=rs.getString("name");
        boolean linkup=rs.getBoolean("linkup");
        rs.close();
%>
<title><%= name %></title>
<link rel="parent" title="Linux.org.ru" href="index.jsp">
<%= tmpl.DocumentHeader() %>
<div class=messages>
<div class=nav>
<div class=color1>
  <table width="100%" cellspacing=1 cellpadding=1 border=0>
    <tr class=body>
      <td align=left valign=middle>
        <strong><%= name %></strong>
      </td>

      <td align=right valign=middle>
        <% if (section==4) { %>
          [<a style="text-decoration: none" href="add-section.jsp?section=<%= section %>">Добавить ссылку</a>]
        <% } else { %>
          [<a style="text-decoration: none" href="add-section.jsp?section=<%= section %>">Добавить сообщение</a>]
        <% } %>

        [<a style="text-decoration: none" href="tracker.jsp">Последние сообщения</a>]

        <% if (section==2) { %>
          [<a style="text-decoration: none" href="rules.jsp">Правила форума</a>]
        <% } %>
      </td>
    </tr>
  </table>
</div>
</div>
</div>

<h1><%= name %></h1>

Группы:
<ul>
<%
        rs=st.executeQuery("SELECT id, title, stat1, stat2, stat3 FROM groups WHERE section="+section+" order by id");
        while (rs.next()) {
                int group=rs.getInt("id");

                if (!linkup)
                        out.print("<li><a href=\"group.jsp?group="+group+"\">"+rs.getString("title")+"</a>");
                else
                        out.print("<li><a href=\"view-links.jsp?group="+group+"\">"+rs.getString("title")+"</a>");


                out.print(" ("+(rs.getInt("stat1")));
                out.print("/"+(rs.getInt("stat2")));
                out.print("/"+(rs.getInt("stat3"))+ ')');

                String des=tmpl.getObjectConfig().getStorage().readMessageNull("grinfo", String.valueOf(group));
                if (des!=null) {
                        out.print(" - <em>");
                        out.print(des);
                        out.print("</em>");
                }
        }
        rs.close();
%>
</ul>
<%
        st.close();
  } finally {
    if (db!=null) db.close();
  }
%>

<% if (section==2) { %>

<h1>Настройки</h1>
Если вы еще не зарегистрировались - вам <a href="register.jsp">сюда</a>.
<ul>
<li><a href="http://images.linux.org.ru/addphoto.php">Добавить фотографию</a>
<li><a href="register.jsp?mode=change">Изменение регистрации</a>
<li><a href="lostpwd.jsp">Получить забытый пароль</a>
<li><a href="edit-profile.jsp">Персональные настройки сайта</a>
</ul>

<% } %>

<%= tmpl.DocumentFooter() %>
