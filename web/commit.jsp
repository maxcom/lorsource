<%@ page pageEncoding="koi8-r" contentType="text/html; charset=utf-8"%>
<%@ page import="java.sql.*"  %>
<%@ page import="java.util.List"%>
<%@ page import="java.util.Random"%>
<%@ page import="java.util.logging.Logger"%>
<%@ page import="javax.servlet.http.HttpServletResponse" %>
<%@ page import="ru.org.linux.site.*" %>
<%@ page import="ru.org.linux.util.HTMLFormatter" %>
<%@ page import="ru.org.linux.util.ServletParameterParser" %>
<%@ taglib tagdir="/WEB-INF/tags" prefix="lor" %>

<% Template tmpl = Template.getTemplate(request);
  Logger logger = Logger.getLogger("ru.org.linux");
%>
<jsp:include page="/WEB-INF/jsp/head.jsp"/>

        <title>Подтверждение сообщения</title>
<jsp:include page="WEB-INF/jsp/header.jsp"/>

<%
  if (!Template.isSessionAuthorized(session)) {
    throw new AccessViolationException("Not authorized");
  }

  if ("GET".equals(request.getMethod())) {
    if (request.getParameter("msgid") == null) {
      throw new MissingParameterException("msgid");
    }

    Connection db = null;

    try {

      int msgid = new ServletParameterParser(request).getInt("msgid");

      db = LorDataSource.getConnection();

      Message message = new Message(db, msgid);

      int groupid = message.getGroupId();
      Group group = new Group(db, message.getGroupId());

      if (message.isCommited()) {
        throw new AccessViolationException("Сообщение уже подтверждено");
      }

      if (!group.isModerated()) {
        throw new AccessViolationException("группа не является модерируемой");
      }

      Section section = new Section(db, group.getSectionId());

%>
<h1>Подтверждение сообщения</h1>
<p>
Данная форма предназначена для администраторов сайта и пользователей,
имеющих права подтверждения сообщений.
<p>
<div class=messages>
  <lor:message db="<%= db %>" message="<%= message %>" showMenu="false" user="<%= Template.getNick(session) %>"/>
</div>

<form method=POST action="commit.jsp">
<input type=hidden name=msgid value="<%= msgid %>">
<input type=hidden name=groupid value="<%= groupid %>">
Заголовок:
<input type=text name=title size=40 value="<%= message.getTitle() %>">
<br>
<%
    Statement st = db.createStatement();
    ResultSet rq = null;
    if (message.getSectionId() == 1) { // news
      out.println("Метки (теги): ");
      out.println("<input type=\"text\" id=\"tags\" name=\"tags\" size=40 value=\"" + message.getTags().toString() + "\"><br>");
%>
  Популярные теги: <%= Tags.getEditTags(Tags.getTopTags(db)) %> <br>
  <%
      out.println("Переместить в группу: ");
      rq = st.executeQuery("SELECT id, title FROM groups WHERE section=1 ORDER BY id");
      out.println("<select name=\"chgrp\">");
      out.println("<option value=" + groupid + '>' + message.getGroupTitle() + " (не менять)</option>");
      while (rq.next()) {
        int id = rq.getInt("id");
        if (id != groupid) {
          out.println("<option value=" + id + '>' + rq.getString("title") + "</option>");
        }
      }
      out.println("</select><br>");
    }
    if (rq != null) {
      rq.close();
    }
    st.close();

    Timestamp lastCommit = section.getLastCommitdate(db);
    if (lastCommit!=null) {
      out.println("Последнее подтверждение в разделе: "+tmpl.dateFormat.format(lastCommit)+"<br>");
    }

  } finally {
      if (db != null) {
        db.close();
      }
  }
%>
<input type=submit value="Submit/Подтвердить">
</form>
<%
  } else {
    int msgid = new ServletParameterParser(request).getInt("msgid");
    String title = new ServletParameterParser(request).getString("title");
    Connection db = null;

    try {
      db = LorDataSource.getConnection();
      db.setAutoCommit(false);
      PreparedStatement pst = db.prepareStatement("UPDATE topics SET moderate='t', commitby=?, commitdate='now', title=? WHERE id=?");
      pst.setInt(3, msgid);
      pst.setString(2, HTMLFormatter.htmlSpecialChars(title));

      PreparedStatement pst2 = db.prepareStatement("UPDATE users SET score=score+3 WHERE id IN (SELECT userid FROM topics WHERE id=?) AND score<300");
      pst2.setInt(1, msgid);

      User user = User.getUser(db, (String) session.getAttribute("nick"));
      pst.setInt(1, user.getId());

      user.checkCommit();

      if (request.getParameter("chgrp") != null) {
        int chgrp = new ServletParameterParser(request).getInt("chgrp");

        Statement st = db.createStatement();
        ResultSet rs = st.executeQuery("select groupid, section, groups.title FROM topics, groups WHERE topics.id=" + msgid + " and groups.id=topics.groupid");
        if (!rs.next()) {
          throw new MessageNotFoundException(msgid);
        }

        int oldgrp = rs.getInt("groupid");
        if (oldgrp != chgrp) {
          String oldtitle = rs.getString("title");
          int section = rs.getInt("section");
          if (section != 1) {
            throw new AccessViolationException("Can't move topics in non-news section");
          }

          rs.close();
          rs = st.executeQuery("SELECT section, title FROM groups WHERE groups.id=" + chgrp);
          rs.next();
          if (rs.getInt("section") != section) {
            throw new AccessViolationException("Can't move topics between sections");
          }
          String newtitle = rs.getString("title");
          st.executeUpdate("UPDATE topics SET groupid=" + chgrp + " WHERE id=" + msgid);
          /* to recalc counters */
          st.executeUpdate("UPDATE groups SET stat4=stat4+1 WHERE id=" + oldgrp + " or id=" + chgrp);
          out.println("<br>Сменена группа с '" + oldtitle + "' на '" + newtitle + "'<br>");
        }
        rs.close();
        st.close();
      }

      pst.executeUpdate();
      pst2.executeUpdate();
      
      if (request.getParameter("tags")!=null) {
        List<String> tags = Tags.parseTags(request.getParameter("tags"));
        Tags.updateTags(db, msgid, tags);
        Tags.updateCounters(db, null, Tags.getMessageTags(db, msgid));
      }

      out.print("Сообщение подтверждено");

      logger.info("Подтверждено сообщение " + msgid + " пользователем " + user.getNick());

      pst.close();
      db.commit();

      Random random = new Random();

      response.setHeader("Location", tmpl.getMainUrl() + "view-all.jsp?nocache=" + random.nextInt());
      response.setStatus(HttpServletResponse.SC_MOVED_TEMPORARILY);
    } finally {
      if (db != null) {
        db.close();
      }
    }
  }
%>
<jsp:include page="WEB-INF/jsp/footer.jsp"/>
