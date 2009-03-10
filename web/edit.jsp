<%@ page pageEncoding="koi8-r" contentType="text/html; charset=utf-8"%>
<%@ page
    import="java.sql.Connection,java.sql.PreparedStatement,java.util.List"
      buffer="200kb" %>
<%@ page import="java.util.logging.Logger" %>
<%@ page import="ru.org.linux.site.*" %>
<%@ page import="ru.org.linux.util.HTMLFormatter" %>
<%@ page import="ru.org.linux.util.ServletParameterParser" %>
<%@ taglib tagdir="/WEB-INF/tags" prefix="lor" %>

<%
  Template tmpl = Template.getTemplate(request);
  Logger logger = Logger.getLogger("ru.org.linux");

  if (!tmpl.isSessionAuthorized()) {
    throw new AccessViolationException("Not authorized");
  }

  int msgid = new ServletParameterParser(request).getInt("msgid");
%>
<jsp:include page="/WEB-INF/jsp/header.jsp"/>
<%
  Connection db = null;
  try {
    db = LorDataSource.getConnection();
    db.setAutoCommit(false);
    Message message = new Message(db, msgid);
    Message newMsg = message;

    User user = User.getCurrentUser(db, session);

    if (!message.isEditable(db, user)) {
      throw new AccessViolationException("это сообщение нельзя править");
    }

    boolean showForm = true;

    if ("POST".equals(request.getMethod())) {
      newMsg = new Message(db, message, request);
      boolean preview = request.getParameter("preview") != null;

      String sSql = "UPDATE topics SET title=?, linktext=?, url=? WHERE id=?";
      PreparedStatement pst = db.prepareStatement(sSql);

      boolean modified = false;

      if (!message.getTitle().equals(newMsg.getTitle())) {
        modified = true;
      }

      pst.setString(1, newMsg.getTitle());

      boolean messageModified = false;
      if (!message.getMessage().equals(newMsg.getMessage())) {
        messageModified = true;
      }

      pst.setString(2, newMsg.getLinktext());

      if (!message.getLinktext().equals(newMsg.getLinktext())) {
        modified = true;
      }

      pst.setString(3, newMsg.getUrl());

      if (message.isHaveLink() && !message.getUrl().equals(newMsg.getUrl())) {
        modified = true;
      }

      pst.setInt(4, msgid);

      if (!preview) {
        if (modified) {
          pst.executeUpdate();
        }

        if (messageModified) {
          newMsg.updateMessageText(db);
        }

        List<String> oldTags = Tags.getMessageTags(db, msgid);
        List<String> newTags = Tags.parseTags(newMsg.getTags().toString());

        boolean modifiedTags = Tags.updateTags(db, msgid, newTags);
        if (modifiedTags && message.isCommited()) {
          Tags.updateCounters(db, oldTags, newTags);
        }

        if (modifiedTags) {
          out.print("tags updated\n");
        }

        if (modified || messageModified || modifiedTags) {
          out.print("<br><a href='view-message.jsp?msgid=" + msgid + "'>Сообщение исправлено</a>.<br>\n");
          logger.info("сообщение " + msgid + " исправлено " + session.getValue("nick"));
          showForm = false;
        } else {
          out.print("nothing changed.\n");
        }
      }
    }

    if (showForm) {
%>
<h1>Редактирование</h1>
<div class=messages>
  <lor:message db="<%= db %>" message="<%= newMsg %>" showMenu="false" user="<%= Template.getNick(session) %>"/>
</div>

<form action="edit.jsp" name="edit" method="post">
  <input type="hidden" name="msgid" value="<%= msgid %>">
  Заголовок новости :
  <input type=text name=title size=40 value="<%= newMsg.getTitle()==null?"":HTMLFormatter.htmlSpecialChars(newMsg.getTitle()) %>" ><br>

  <br>
  <textarea name="newmsg" cols="70" rows="20"><%= newMsg.getMessage() %></textarea>
  <br><br>
  <% if (message.isHaveLink()) {
  %>
  Текст ссылки:
  <input type=text name=linktext size=60 value="<%= newMsg.getLinktext()==null?"":HTMLFormatter.htmlSpecialChars(newMsg.getLinktext()) %>"><br>
  <%
    }
  %>
  <% if (message.isHaveLink()) {
  %>
  Ссылка :
  <input type=text name=url size=70 value="<%= newMsg.getUrl()==null?"":HTMLFormatter.htmlSpecialChars(newMsg.getUrl()) %>"><br>
  <% } %>

  <% if (message.getSectionId()==1) {
    String result = newMsg.getTags().toString();
  %>
  Теги:
  <input type="text" name="tags" id="tags" value="<%= result %>"><br>
  Популярные теги: <%= Tags.getEditTags(Tags.getTopTags(db)) %> <br>
  <% } %>
  <br>

  <input type="submit" value="отредактировать">
  &nbsp;
  <input type=submit name=preview value="Предпросмотр">
</form>
<%
      }

    db.commit();
    // out.print("<-- or msgid is null -->\n");
  } finally {
    if (db != null) {
      db.close();
    }
  }

%>
<jsp:include page="/WEB-INF/jsp/footer.jsp"/>
