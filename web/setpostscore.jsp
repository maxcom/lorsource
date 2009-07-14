<%@ page contentType="text/html; charset=utf-8"%>
<%@ page import="java.sql.Connection,java.sql.PreparedStatement,java.util.logging.Logger"  %>
<%@ page import="ru.org.linux.site.*" %>
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

        <title>Смена параметров сообщения</title>
<jsp:include page="WEB-INF/jsp/header.jsp"/>
<%
  if (!tmpl.isModeratorSession()) {
    throw new AccessViolationException("Not moderator");
  }
%>

<%
  if ("GET".equals(request.getMethod())) {
    Connection db = null;

    try {
      int msgid = new ServletParameterParser(request).getInt("msgid");

      db = LorDataSource.getConnection();

      Message msg = new Message(db, msgid);

      int postscore = msg.getPostScore();
      boolean sticky = msg.isSticky();
      boolean notop = msg.isNotop();

%>
<h1>Смена режима параметров сообщения</h1>
Данная форма предназначена для администраторов сайта и пользователей,
имеющих права подтверждения сообщений.
<form method=POST action="setpostscore.jsp">
<input type=hidden name=msgid value="<%= msgid %>">
<br>
Текущий уровень записи: <%= (postscore<0?"только для модераторов":Integer.toString(postscore)) %>
<select name="postscore">
  <option value="0">0 - без ограничений</option>
  <option value="50">50 - для зарегистрированных</option>
  <option value="100">100 - одна "звезда"</option>
  <option value="200">200 - две "звезды"</option>
  <option value="300">300 - три "звезды"</option>
  <option value="400">400 - четыре "звезды"</option>
  <option value="500">500 - пять "звезд"</option>
  <option value="-1">только для модераторов</option>
</select><br>
Прикрепить сообщение <input type=checkbox name="sticky" <%= sticky?"checked":"" %>><br>
Удалить из top10 <input type=checkbox name="notop" <%= notop?"checked":"" %>><br>
<%
  } finally {
    if (db != null) {
      db.close();
    }
  }
%>
<input type=submit value="Изменить">
</form>
<%
  } else {
    int msgid = new ServletParameterParser(request).getInt("msgid");
    int postscore = new ServletParameterParser(request).getInt("postscore");
    boolean sticky = request.getParameter("sticky") != null;
    boolean notop = request.getParameter("notop") != null;

    if (postscore < -1) {
      postscore = 0;
    }
    if (postscore > 500) {
      postscore = 500;
    }

    Connection db = null;
    try {
      db = LorDataSource.getConnection();
      db.setAutoCommit(false);

      Message msg = new Message(db, msgid);

      PreparedStatement pst = db.prepareStatement("UPDATE topics SET postscore=?, sticky=?, notop=? WHERE id=?");
      pst.setInt(1, postscore);
      pst.setBoolean(2, sticky);
      pst.setBoolean(3, notop);
      pst.setInt(4, msgid);

      User user = User.getUser(db, Template.getNick(session));
      user.checkCommit();

      pst.executeUpdate();

      if (msg.getPostScore() != postscore) {
        out.print("Установлен новый уровень записи " + (postscore < 0 ? "только для модераторов" : Integer.toString(postscore)) + "<br>");
        logger.info("Установлен новый уровень записи " + postscore + " для " + msgid + " пользователем " + user.getNick());
      }

      if (msg.isSticky() != sticky) {
        out.print("Новое значение sticky: " + sticky + "<br>");
        logger.info("Новое значение sticky: " + sticky);
      }

      if (msg.isNotop() != notop) {
        out.print("Новое значение notop: " + notop + "<br>");
        logger.info("Новое значение notop: " + notop);
      }

      pst.close();
      db.commit();

    } finally {
      if (db != null) {
        db.close();
      }
    }
  }
%>
<jsp:include page="WEB-INF/jsp/footer.jsp"/>
