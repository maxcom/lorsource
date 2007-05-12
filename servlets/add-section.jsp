<%@ page contentType="text/html; charset=koi8-r"%>
<%@ page import="java.net.URLEncoder,java.sql.Connection,java.sql.ResultSet,java.sql.Statement,ru.org.linux.site.BadSectionException" errorPage="error.jsp"%>
<%@ page import="ru.org.linux.site.MissingParameterException"%>
<%@ page import="ru.org.linux.site.Template"%>
<% Template tmpl = new Template(request, config, response); %>
<%= tmpl.head() %>
<%
  Connection db = null;
  try {

   if (request.getParameter("section")==null)
   	throw new MissingParameterException("section");

   int section = Integer.parseInt(request.getParameter("section"));

   db = tmpl.getConnection("add-section");
   Statement st=db.createStatement();

   ResultSet rs=st.executeQuery("SELECT name, browsable FROM sections WHERE id=" + section);

   if (!rs.next()) throw new BadSectionException();
   if (!rs.getBoolean("browsable")) { throw new BadSectionException(); }
   String title=rs.getString("name")+": добавление";
   rs.close();
%>
<title><%= title %></title>
<%= tmpl.DocumentHeader() %>

<% String info=tmpl.getObjectConfig().getStorage().readMessageNull("addportal", String.valueOf(section));
   if (info!=null) {
   	out.print(info);
	out.print("<h2>Выберите группу</h2>");
   } else
   	out.print("<h1>"+title+"</h1>");
%>

Доступные группы:
<ul>
<%

	rs=st.executeQuery("SELECT groups.id as guid, title FROM groups WHERE section="+section+" ORDER BY guid");

	while(rs.next()) {
		int guid=rs.getInt("guid");
		String returnUrl="&return="+URLEncoder.encode("group.jsp?group="+guid);
		if (tmpl.isSearchMode())
			out.print("<li><a href=\"add.jsp?group=" + guid + "&noinfo=1\">" + rs.getString("title") + "</a> (<a href=\"group.jsp?group=" + guid + "\">просмотр...</a>)");
		else
			out.print("<li><a href=\"add.jsp?group=" + guid + "&noinfo=1"+returnUrl+"\">" + rs.getString("title") + "</a> (<a href=\"group.jsp?group=" + guid + "\">просмотр...</a>)");

		String des=tmpl.getObjectConfig().getStorage().readMessageNull("grinfo", String.valueOf(guid));
		if (des!=null) {
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
