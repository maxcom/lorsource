<%@ page contentType="text/html; charset=koi8-r"%>
<%@ page import="java.sql.Connection,java.sql.PreparedStatement,java.sql.ResultSet,java.sql.Statement" errorPage="/error.jsp" %>
<%@ page import="ru.org.linux.site.Template"%>
<% Template tmpl = new Template(request, config, response); %>
<%= tmpl.head() %>
<title>we make new db information for mess_priority</title>
<%= tmpl.DocumentHeader() %>
<%  Connection db=null;
    try { %>
<%
	db = tmpl.getConnection("_topstat");
	db.setAutoCommit(false);
	Statement stmtFrom=db.createStatement();
	Statement stmtDel=db.createStatement();
	PreparedStatement stmtTo=db.prepareStatement("INSERT INTO top10 (msgid, mess_order) VALUES (?,?)");
	stmtDel.executeUpdate("DELETE FROM top10");
	ResultSet rs=stmtFrom.executeQuery("SELECT topics.id as msgid FROM topics WHERE age('now', topics.postdate)<'1 month 1 day' AND not deleted AND notop is null ORDER BY stat1 desc, msgid limit 10");

	int c=0;
	while (rs.next()) {
	    c++;
	    int msgid=rs.getInt("msgid");
	    stmtTo.setInt(1,msgid);
	    stmtTo.setInt(2,c);
	    stmtTo.executeUpdate();
	}
	rs.close();
	stmtFrom.close();
	stmtDel.close();
	stmtTo.close();
	db.commit();
%>

<% } finally {
  if (db!=null) db.close();
} %>
<%= tmpl.DocumentFooter() %>
