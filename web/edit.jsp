<%@ page pageEncoding="koi8-r" contentType="text/html; charset=utf-8"%>
<%@ page
    import="java.sql.Connection,java.sql.PreparedStatement,java.util.List"
      buffer="200kb" %>
<%@ page import="java.util.logging.Logger" %>
<%@ page import="ru.org.linux.site.*" %>
<%@ page import="ru.org.linux.util.HTMLFormatter" %>
<%@ page import="ru.org.linux.util.ServletParameterParser" %>
<%
  Template tmpl = Template.getTemplate(request);
  Logger logger = Logger.getLogger("ru.org.linux");

  if (!tmpl.isModeratorSession() && !tmpl.isCorrectorSession()) {
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

    String sMsgTitle = message.getTitle();
    String sURL = message.getUrl();
    String sURLtitle = message.getLinktext();

    User moderator = User.getCurrentUser(db, session);

    if (!message.isEditable(db, moderator)) {
      throw new AccessViolationException("это сообщение нельзя править");
    }

    String cText = message.getMessage();

    boolean debugme = false;

    if (debugme) {
      out.print("<!-- old message = " + cText + "\n msglen: " + cText.length() + " -->\n");
    }

    String cnText = request.getParameter("newmsg");
    if (request.getMethod().equals("POST") && (cnText != null)) {
      if (debugme) {
        out.print("<!-- new message = " + cnText + "\n msglen: " + cnText.length() + " -->\n");
        out.print("<!-- method is POST -->\n");
      }
      // do changes to message
      // update db
      String snMsgTitle = request.getParameter("title");
      String snURLtitle = request.getParameter("url_text");
      String snURL = request.getParameter("url");

      String tags = request.getParameter("tags");

      String sSql = "UPDATE topics SET title=?, linktext=?, url=? WHERE id=?";
      PreparedStatement pst = db.prepareStatement(sSql);

      pst.setString(1, snMsgTitle);

      boolean modified = false;

      if (!snMsgTitle.equals(sMsgTitle)) {
        modified = true;
      }

      if (!cText.equals(cnText)) {
        modified = true;
        message.updateMessageText(db, cnText);
      }

      pst.setString(2, snURLtitle);

      if (snURLtitle != null && !snURLtitle.equals(sURLtitle)) {
        modified = true;
      }

      pst.setString(3, snURL);

      if (snURL != null && !snURL.equals(sURL)) {
        modified = true;
      }

      pst.setInt(4, msgid);

      if (modified) {
        pst.executeUpdate();

        out.print("<a href='view-message.jsp?msgid=" + msgid + "'>Сообщение исправлено</a>.<br>\n");
        // out.print("Редирект в течении 5 секунд.\n");
        logger.info("сообщение " + msgid + " исправлено " + session.getValue("nick"));
      } else {
        out.print("nothing changed.\n");
      }

      if (tags!=null) {
        List<String> oldTags = Tags.getMessageTags(db, msgid);
        List<String> newTags = Tags.parseTags(tags);

        Tags.updateTags(db, msgid, newTags);
        if (message.isCommited()) {
          Tags.updateCounters(db, oldTags, newTags);
        }
        
        out.print("tags updates\n");
      }
    } else {
%>
<form action="edit.jsp" name="edit" method="post">
  <input type="hidden" name="msgid" value="<%= msgid %>">
  Заголовок новости :
  <% if ((sMsgTitle != null) && (sMsgTitle.length() != 0)) {
    out.print("<input type=\"text\" name=\"title\" size=\"70\" value=\"" + HTMLFormatter.htmlSpecialChars(sMsgTitle) + "\">\n");
  } else {
    out.print("<input type=\"text\" name=\"title\" size=\"70\" value='' disabled>\n");
  }
  %>
  <br>
  <textarea name="newmsg" cols="70" rows="20"><%= cText %></textarea>
  <br><br>
  Текст ссылки :
  <% if (message.isHaveLink()) {
    out.print("<input type=\"text\" name=\"url_text\" size=\"78\" value=\"" + sURLtitle + "\">\n");
  } else {
    out.print("<input type=\"text\" name=\"url_text\" size=\"78\" value='" + sURLtitle + "' readonly style=\"background:#979797;color:#79787e;\">\n");
  }
  %>
  <br>
  Ссылка :
  <% if (message.isHaveLink()) {
    out.print("<input type=\"text\" name=\"url\" size=\"84\" value=\"" + sURL + "\">\n");
  } else {
    out.print("<input type=\"text\" name=\"url\" size=\"84\" value='" + sURL + "' readonly style=\"background:#979797;color:#79787e;\">\n");
  }
  %>
  <br>

  Теги:
  <% if (message.getSectionId()==1) { %>
  <input type="text" name="tags" id="tags" value="<%= Tags.getPlainTags(db, msgid) %>"><br>
  Популярные теги: <%= Tags.getEditTags(Tags.getTopTags(db)) %> <br>
  <% } %>
  <br><br>
  <input type="submit" value="отредактировать">
  &nbsp;
  <input type="reset" value="сбросить">
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
