<%@ page pageEncoding="koi8-r" contentType="text/html; charset=utf-8"%>
<%@ page import="java.sql.Connection,java.sql.PreparedStatement,java.sql.ResultSet,java.sql.Statement,java.util.logging.Logger"   buffer="60kb" %>
<%@ page import="ru.org.linux.site.LorDataSource" %>
<%@ page import="ru.org.linux.site.Template" %>
<%@ page import="ru.org.linux.site.User" %>
<%@ page import="ru.org.linux.util.ServletParameterParser" %>
<%
  Logger logger = Logger.getLogger("ru.org.linux");

  Template tmpl = Template.getTemplate(request);
%>
<jsp:include page="WEB-INF/jsp/head.jsp"/>
<%
  if (!tmpl.isModeratorSession()) {
    throw new IllegalAccessException("Not authorized");
  }

  out.println("<title>Перенос темы...</title>");
  %>
<jsp:include page="WEB-INF/jsp/header.jsp"/>
<%
  Connection db = null;

  try {
    int msgid = new ServletParameterParser(request).getInt("msgid");
    db = LorDataSource.getConnection();
    Statement st1 = db.createStatement();
    if (request.getMethod().equals("POST")) {
      String newgr = request.getParameter("moveto");
      String sSql = "UPDATE topics SET groupid=" + newgr + " WHERE id=" + msgid;

      PreparedStatement pst = db.prepareStatement("SELECT topics.groupid,topics.userid,groups.title FROM topics,groups WHERE topics.id=? AND groups.id=topics.groupid");
      pst.setInt(1,msgid);

      ResultSet rs = pst.executeQuery();
      String oldgr = "n/a";
      String title = "n/a";

      if (rs.next()) {
        oldgr = rs.getString("groupid");
	title = rs.getString("title");
        int userid = rs.getInt("userid");

        User user = User.getUserCached(db, userid);

        if (user.isAnonymousScore() && "8404".equals(newgr)) {
	  throw new IllegalAccessException("Была ж договоренность на тему того, что сценарий \"анонимный пост в других разделах с просьбой перетащить в толксы\" не должен работать");
	}
      }

      st1.executeUpdate(sSql);

      PreparedStatement pst1 = db.prepareStatement("UPDATE msgbase SET message=message||? WHERE id=?");
      pst1.setString(1,"\n<br>\n<br><i>Перемещено " + session.getValue("nick") + " из "+title+"</i>\n");
      pst1.setInt(2,msgid);
      pst1.executeUpdate();
      logger.info("topic " + msgid + " moved" +
          " by " + session.getValue("nick") + " from news/forum " + oldgr + " to forum " + newgr);
    } else {
      out.println("перенос сообщения <strong>" + msgid + "</strong> в форум:");
      ResultSet rs = st1.executeQuery("SELECT id,title FROM groups WHERE section=2 ORDER BY id");
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
        if (db!=null) {
          db.close();
        }
    }
%>


  <jsp:include page="WEB-INF/jsp/footer.jsp"/>
