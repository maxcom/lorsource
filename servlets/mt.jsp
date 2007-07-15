<%@ page contentType="text/html; charset=koi8-r"%>
<%@ page import="java.sql.Connection,java.sql.ResultSet,java.sql.Statement,ru.org.linux.site.Template" errorPage="/error.jsp" buffer="60kb" %>
<%
    Template tmpl = new Template(request, config, response);
    out.print(tmpl.head());
    if (!tmpl.isSessionAuthorized(session) || !(((Boolean) session.getValue("moderator")).booleanValue())) {
        throw new IllegalAccessException("Not authorized");
    }
    out.println("<title>Перенос темы...</title>");
    out.print(tmpl.DocumentHeader());
    Connection db = null;
    try {
        int msgid = tmpl.getParameters().getInt("msgid");
        db = tmpl.getConnection("view-message"); 
        ResultSet rs; Statement st1; String sSql;
        st1 = db.createStatement(); 
        if (request.getMethod().equals("POST")) {
            String newgr=request.getParameter("moveto");
            sSql = "UPDATE topics SET groupid="+newgr+" WHERE id="+msgid;
            // out.println(sSql);
            st1.executeUpdate(sSql);
            tmpl.getLogger().notice("move-topic.jsp", "topic "+msgid+" moved"+
                    " by "+session.getValue("nick")+" to forum "+newgr);
        } else {
            out.println("перенос сообщения <strong>"+msgid+"</strong> в форум:");
            rs = st1.executeQuery("SELECT id,title FROM groups WHERE section=2 ORDER BY id");
%>
<form method="post" action="mt.jsp">
<input type=hidden name="msgid" value='<%= msgid %>'>
<select name="moveto">
<%
            while (rs.next()) {
                out.println("<option value='"+rs.getInt("id")+"'>"+rs.getString("title")+"</option>");
            }
            rs.close();
            st1.close();
            out.println("</select>\n<input type='submit' name='move' value='move'>\n</form>");
        }
    } finally {
        if (db!=null) db.close();
    }
%>


<%= tmpl.DocumentFooter() %>
