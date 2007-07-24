<%@ page contentType="text/html; charset=koi8-r"%>
<%@ page import="java.net.URLEncoder,java.sql.Connection,java.sql.ResultSet,java.sql.Statement,ru.org.linux.site.BadSectionException" errorPage="/error.jsp"%>
<%@ page import="ru.org.linux.site.MissingParameterException"%>
<%@ page import="ru.org.linux.site.Section"%>
<%@ page import="ru.org.linux.site.Template" %>
<% Template tmpl = new Template(request, config, response); %>
<%= tmpl.head() %>
<%
  Connection db = null;
  try {

    if (request.getParameter("section") == null)
      throw new MissingParameterException("section");

    int sectionid = Integer.parseInt(request.getParameter("section"));

    db = tmpl.getConnection("add-section");
    Statement st = db.createStatement();

    Section section = new Section(db, sectionid);

    if (!section.isBrowsable()) {
      throw new BadSectionException(sectionid);
    }

    String title = section.getName() + ": добавление";
%>
<title><%= title %></title>
<%= tmpl.DocumentHeader() %>

<%
  String info = tmpl.getObjectConfig().getStorage().readMessageNull("addportal", String.valueOf(sectionid));
  if (info != null) {
    out.print(info);
    out.print("<h2>Выберите группу</h2>");
  } else
    out.print("<h1>" + title + "</h1>");
%>

Доступные группы:
<ul>
<%

  ResultSet rs = st.executeQuery("SELECT groups.id as guid, title FROM groups WHERE section=" + sectionid + " ORDER BY guid");

  while (rs.next()) {
    int guid = rs.getInt("guid");
    String returnUrl = "&return=" + URLEncoder.encode("group.jsp?group=" + guid);
    out.print("<li><a href=\"add.jsp?group=" + guid + "&noinfo=1" + returnUrl + "\">" + rs.getString("title") + "</a> (<a href=\"group.jsp?group=" + guid + "\">просмотр...</a>)");

    String des = tmpl.getObjectConfig().getStorage().readMessageNull("grinfo", String.valueOf(guid));
    if (des != null) {
      out.print(" - <em>");
      out.print(des);
      out.print("</em>");
    }


  }

%>
</ul>
Если вы считаете, что необходимо добавить какую-либо группу, <a href="mailto:webmaster@linux.org.ru">сообщите</a> нам.

<%
   rs.close();
   st.close();
  } finally {
    if (db!=null) db.close();
  }
%>
<%=	tmpl.DocumentFooter() %>
