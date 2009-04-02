<%@ page pageEncoding="koi8-r" contentType="text/html; charset=utf-8"%>
<%@ page import="java.sql.Connection,java.sql.PreparedStatement,java.util.List,java.util.Random,java.util.logging.Logger"  %>
<%@ page import="javax.servlet.http.HttpServletResponse"%>
<%@ page import="ru.org.linux.site.*"%>
<%@ page import="ru.org.linux.util.HTMLFormatter"%>
<%@ page import="ru.org.linux.util.ServletParameterParser" %>
<%--
  ~ Copyright 1998-2009 Linux.org.ru
  ~    Licensed under the Apache License, Version 2.0 (the "License");
  ~    you may not use this file except in compliance with the License.
  ~    You may obtain a copy of the License at
  ~
  ~        http://www.apache.org/licenses/LICENSE-2.0
  ~
  ~    Unless required by applicable law or agreed to in writing, software
  ~    distributed under the License is distributed on an "AS IS" BASIS,
  ~    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
  ~    See the License for the specific language governing permissions and
  ~    limitations under the License.
  --%>

<% Template tmpl = Template.getTemplate(request);
  Logger logger = Logger.getLogger("ru.org.linux");
%>
<jsp:include page="WEB-INF/jsp/head.jsp"/>

        <title>Редактирование опроса</title>
<jsp:include page="WEB-INF/jsp/header.jsp"/>

<%
  if (!tmpl.isModeratorSession()) {
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

      int id = Poll.getPollIdByTopic(db, msgid);

      Poll poll = new Poll(db, id);

%>
<h1>Редактирование опроса</h1>
<p>
Данная форма предназначена для администраторов сайта и пользователей,
имеющих права подтверждения сообщений.
<p>

<form method=POST action="edit-vote.jsp">
<input type=hidden name=id value="<%= id %>">
<input type=hidden name=msgid value="<%= msgid %>">
Вопрос:
<input type=text name=title size=40 value="<%= poll.getTitle() %>">
<br>
<%
  List<PollVariant> variants = poll.getPollVariants(db, Poll.ORDER_ID);

  for (PollVariant var : variants) {
%>
  Вариант #<%= var.getId() %>: <input type="text" name="var<%= var.getId() %>" size="40"
                                      value="<%= var.getLabel() %>"><br>
  <%
    }
  %>
  Еще вариант: <input type="text" name="new1" size="40"><br>
  Еще вариант: <input type="text" name="new2" size="40"><br>
  Еще вариант: <input type="text" name="new3" size="40"><br>

  <%

    } finally {
      if (db != null) {
        db.close();
      }
    }
  %>
<input type=submit name="change" value="Изменить">
<!-- input type=submit name="submit" value="Подтвердить" -->
</form>
<%
  } else {
    if (request.getParameter("msgid") == null) {
      throw new MissingParameterException("msgid");
    }

    int msgid = new ServletParameterParser(request).getInt("msgid");
    int id = new ServletParameterParser(request).getInt("id");
    String title = new ServletParameterParser(request).getString("title");

    boolean submit = request.getParameter("submit") != null;

    Connection db = null;

    try {
      db = LorDataSource.getConnection();
      db.setAutoCommit(false);

      User user = User.getUser(db, (String) session.getAttribute("nick"));
      user.checkCommit();

      Poll poll = new Poll(db, id);

      PreparedStatement pstTitle = db.prepareStatement("UPDATE votenames SET title=? WHERE id=?");
      pstTitle.setInt(2, id);
      pstTitle.setString(1, HTMLFormatter.htmlSpecialChars(title));

      pstTitle.executeUpdate();

      PreparedStatement pstTopic = db.prepareStatement("UPDATE topics SET title=? WHERE id=?");
      pstTopic.setInt(2, msgid);
      pstTopic.setString(1, HTMLFormatter.htmlSpecialChars(title));

      pstTopic.executeUpdate();

      List<PollVariant> variants = poll.getPollVariants(db, Poll.ORDER_ID);
      for (PollVariant var : variants) {
        String label = new ServletParameterParser(request).getString("var" + var.getId());

        if (label == null || label.trim().length() == 0) {
          var.remove(db);
        } else {
          var.updateLabel(db, label);
        }
      }

      for (int i = 1; i <= 3; i++) {
        String label = new ServletParameterParser(request).getString("new" + i);

        if (label != null && label.trim().length() > 0) {
          poll.addNewVariant(db, label);
        }
      }

      if (submit) {
        //poll.commit(db, user);

        out.print("Сообщение отредактировано");

        logger.info("Отредактирован опрос" + id + " пользователем " + user.getNick());
      }

      db.commit();

      Random random = new Random();

      response.setHeader("Location", tmpl.getMainUrl() + "view-message.jsp?msgid=" + msgid + "&nocache=" + random.nextInt());
      response.setStatus(HttpServletResponse.SC_MOVED_TEMPORARILY);
    } finally {
      if (db != null) {
        db.close();
      }
    }
  }
%>
<jsp:include page="WEB-INF/jsp/footer.jsp"/>
