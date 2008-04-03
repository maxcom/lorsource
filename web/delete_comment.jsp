<%@ page pageEncoding="koi8-r" contentType="text/html; charset=utf-8"%>
<%@ page import="java.sql.Connection,java.sql.PreparedStatement,java.sql.ResultSet"  %>
<%@ page import="java.sql.Statement"%>
<%@ page import="ru.org.linux.site.*"%>
<%@ page import="ru.org.linux.util.ServletParameterParser" %>
<% Template tmpl = Template.getTemplate(request); %>
<jsp:include page="WEB-INF/jsp/head.jsp"/>

        <title>Удаление сообщения</title>
<jsp:include page="WEB-INF/jsp/header.jsp"/>
<%
   Connection db = null;

  int msgid= new ServletParameterParser(request).getInt("msgid");

   try {

   if (request.getParameter("reason")==null) {
   	if (request.getParameter("msgid")==null) {
             throw new MissingParameterException("msgid");
           }
%>
<script language="Javascript" type="text/javascript">
<!--
function change(dest,source)
{
	dest.value = source.options[source.selectedIndex].value;
}
   // -->
</script>
<h1>Удаление сообщения</h1>
Вы можете удалить свое сообщение в течении часа с момента
его помещения.
<form method=POST action="delete_comment.jsp">
<table>
<% if (session == null || session.getAttribute("login") == null || !(Boolean) session.getAttribute("login")) { %>
<tr>
<td>Имя:</td>
<td><input type=text name=nick size=40>
</td>
</tr>
<tr>
<td>Пароль:</td>
<td><input type=password name=password size=40></td>
</tr>
<% } %>
<tr>
<td>Причина удаления<br>Выберите из меню или напишите сами</td>
<td>
<select name=reason_select onChange="change(reason,reason_select)">
<option value="">
<option value="3.1 Дубль">3.1 Дубль
<option value="3.2 Неверная кодировка">3.2 Неверная кодировка
<option value="3.3 Некорректное форматирование">3.3 Некорректное форматирование
<option value="3.4 Пустое сообщение">3.4 Пустое сообщение
<option value="4.1 Offtopic">4.1 Offtopic
<option value="4.2 Вызывающе неверная информация">4.2 Вызывающе неверная информация
<option value="4.3 Провокация flame">4.3 Провокация flame
<option value="4.4 Обсуждение действий модераторов">4.4 Обсуждение действий модераторов
<option value="4.5 Тестовые сообщения">4.5 Тестовые сообщения
<option value="4.6 Спам">4.6 Спам
<option value="4.7 Флуд">4.7 Флуд
<option value="5.1 Нецензурные выражения">5.1 Нецензурные выражения
<option value="5.2 Оскорбление участников дискуссии">5.2 Оскорбление участников дискуссии
<option value="5.3 Национальные/политические/религиозные споры">5.3 Национальные/политические/религиозные споры
<option value="5.4 Личная переписка">5.4 Личная переписка
<option value="5.5 Преднамеренное нарушение правил русского языка">5.5 Преднамеренное нарушение правил русского языка
<option value="6 Нарушение copyright">6 Нарушение copyright
<option value="6.2 Warez">6.2 Warez
<option value="7.1 Ответ на некорректное сообщение">7.1 Ответ на некорректное сообщение
</select>
</td><tr><td></td>
<td><input type=text name=reason size=40></td>
</tr>

<% if (tmpl.isModeratorSession()) { %>
<tr><td>Штраф score (от 0 до 20)</td>
<td><input type=text name=bonus size=40 value="7"></td>
</tr>
<% } %>
</table>
<% if (!tmpl.isModeratorSession()) { %>
  <input type=hidden name=bonus size=40 value="0">
<% } %>
<input type=hidden name=msgid value="<%= request.getParameter("msgid") %>">
<input type=submit value="Delete/Удалить">
</form>
<div class="messages">
<div class="comment">
<%
  db = LorDataSource.getConnection();

  Statement st = db.createStatement();
  ResultSet rs = st.executeQuery("SELECT topic FROM comments WHERE id=" + msgid);
  rs.next();

  int topicId = rs.getInt("topic");
  rs.close();

  try {
    Message topic = new Message(db, topicId);

    if (topic.isDeleted()) {
      throw new AccessViolationException("тема удалена");
    }

    /* TODO надо сделать проверку на то, что коммент уже удален. И еще логику с select for update */

    boolean showDeleted = tmpl.isModeratorSession();

    CommentList comments = CommentList.getCommentList(db, topic, showDeleted);

    CommentViewer cv = new CommentViewer(tmpl, db, comments, Template.getNick(session), topic.isExpired());
    out.print(cv.showSubtree(msgid));
  } catch (MessageNotFoundException ex) {
    // it's ok for votes
  }
%>
</div>
</div>
<%
  } else {
    String nick = request.getParameter("nick");
    String reason = request.getParameter("reason");
    int bonus = new ServletParameterParser(request).getInt("bonus");

    if (bonus < 0 || bonus > 20) {
      throw new BadParameterException("incorrect bonus value");
    }

    db = LorDataSource.getConnection();
    db.setAutoCommit(false);

    CommentDeleter deleter = new CommentDeleter(db);

    User user;

    if (session == null || session.getAttribute("login") == null || !(Boolean) session.getAttribute("login")) {
      if (request.getParameter("nick") == null) {
        throw new BadInputException("Вы уже вышли из системы");
      }
      user = User.getUser(db, nick);
      user.checkPassword(request.getParameter("password"));
    } else {
      user = User.getUser(db, (String) session.getAttribute("nick"));
      nick = (String) session.getAttribute("nick");
    }

    user.checkAnonymous();

    PreparedStatement pr = db.prepareStatement("SELECT postdate>CURRENT_TIMESTAMP-'1 hour'::interval as perm FROM users, comments WHERE comments.id=? AND comments.userid=users.id AND users.nick=?");
    pr.setInt(1, msgid);
    pr.setString(2, nick);
    ResultSet rs = pr.executeQuery();
    boolean perm = false;

    if (rs.next()) {
      perm = rs.getBoolean("perm");
    }
    boolean selfDel = false;
    if (perm) {
      selfDel = true;
    }

    rs.close();

    if (!perm && user.canModerate()) {
      perm = true;
    }

    if (!perm) {
      PreparedStatement mod = db.prepareStatement("SELECT moderator FROM groups,topics,comments WHERE topics.groupid=groups.id AND comments.id=? AND comments.topic=topics.id");
      mod.setInt(1, msgid);

      rs = mod.executeQuery();
      if (!rs.next()) {
        throw new MessageNotFoundException(msgid);
      }

      if (rs.getInt("moderator") == user.getId()) {
        perm = true; // NULL is ok
      }

      rs.close();
      mod.close();
    }

    if (!perm) {
      user.checkDelete();
    }

    if (!selfDel) {
      out.print(deleter.deleteReplys(msgid, user, bonus != 0));
      out.print(deleter.deleteComment(msgid, reason, user, -bonus));
    } else {
      out.print(deleter.deleteComment(msgid, reason, user, 0));
    }

    deleter.close();

    db.commit();
  }
%>
<%
  } finally {
    if (db != null) {
      db.close();
    }
  }
%>
<jsp:include page="WEB-INF/jsp/footer.jsp"/>
