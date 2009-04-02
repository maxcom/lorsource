<%@ page pageEncoding="koi8-r" contentType="text/html; charset=utf-8"%>
<%@ page import="java.sql.Connection,java.sql.PreparedStatement,java.sql.ResultSet"  %>
<%@ page import="java.util.logging.Logger"%>
<%@ page import="ru.org.linux.site.*" %>
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

<%
  Logger logger = Logger.getLogger("ru.org.linux");
%>
<jsp:include page="WEB-INF/jsp/head.jsp"/>

        <title>Удаление сообщения</title>
<jsp:include page="WEB-INF/jsp/header.jsp"/>

<%
  if (request.getParameter("reason")==null) {
    if (request.getParameter("msgid")==null) {
      throw new MissingParameterException("msgid");
    }
%>
<script language="Javascript">
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
<form method=POST action="delete.jsp">
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
</td>
<tr><td></td>
<td><input type=text name=reason size=40></td>
</tr>
</table>
<input type=hidden name=msgid value="<%= request.getParameter("msgid") %>">
<input type=submit value="Delete/Удалить">
</form>
<%
  } else {
    Connection db = null;
    try {
      int msgid = Integer.parseInt(request.getParameter("msgid"));
      String nick = request.getParameter("nick");
      String reason = request.getParameter("reason");

      db = LorDataSource.getConnection();
      db.setAutoCommit(false);

      PreparedStatement lock = db.prepareStatement("SELECT deleted FROM topics WHERE id=? FOR UPDATE");
      PreparedStatement st1 = db.prepareStatement("UPDATE topics SET deleted='t',sticky='f' WHERE id=?");
      PreparedStatement st2 = db.prepareStatement("INSERT INTO del_info (msgid, delby, reason) values(?,?,?)");
      lock.setInt(1, msgid);
      st1.setInt(1, msgid);
      st2.setInt(1, msgid);
      st2.setString(3, reason);

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
      st2.setInt(2, user.getId());

      ResultSet lockResult = lock.executeQuery(); // lock another delete.jsp on this row

      if (lockResult.next() && lockResult.getBoolean("deleted")) {
        throw new UserErrorException("Сообщение уже удалено");
      }

      PreparedStatement pr = db.prepareStatement("SELECT postdate>CURRENT_TIMESTAMP-'1 hour'::interval as perm FROM users, topics WHERE topics.id=? AND topics.userid=users.id AND users.nick=?");
      pr.setInt(1, msgid);
      pr.setString(2, nick);
      ResultSet rs = pr.executeQuery();
      boolean perm = false;

      if (rs.next()) {
        perm = rs.getBoolean("perm");
      }

      rs.close();

      if (!perm) {
        PreparedStatement mod = db.prepareStatement("SELECT moderator FROM groups,topics WHERE topics.groupid=groups.id AND topics.id=?");
        mod.setInt(1, msgid);

        rs = mod.executeQuery();

        if (!rs.next()) {
          throw new MessageNotFoundException(msgid);
        }

        if (rs.getInt("moderator") == user.getId()) {
          perm = true; // NULL is ok
        }

        mod.close();
        rs.close();
      }

      if (!perm) {
        PreparedStatement mod = db.prepareStatement("SELECT topics.moderate as mod, sections.moderate as needmod FROM groups,topics,sections WHERE topics.groupid=groups.id AND topics.id=? AND groups.section=sections.id");
        mod.setInt(1, msgid);

        rs = mod.executeQuery();
        if (!rs.next()) {
          throw new MessageNotFoundException(msgid);
        }

        if (rs.getBoolean("needmod") && !rs.getBoolean("mod") && user.canModerate()) {
          perm = true;
        }

        rs.close();
      }

      if (!perm && user.canModerate()) {
        PreparedStatement mod = db.prepareStatement("SELECT postdate>CURRENT_TIMESTAMP-'1 month'::interval as perm, section FROM topics,groups WHERE topics.groupid=groups.id AND topics.id=?");
        mod.setInt(1, msgid);

        rs = mod.executeQuery();
        if (!rs.next()) {
          throw new MessageNotFoundException(msgid);
        }

        if (rs.getBoolean("perm") || rs.getInt("section") == Section.SECTION_LINKS) {
          perm = true;
        }

        rs.close();
      }

      if (!perm) {
        user.checkDelete();
      }

      st1.executeUpdate();
      st2.executeUpdate();

      out.print("Сообщение удалено");
      logger.info("Удалено сообщение " + msgid + " пользователем " + nick + " по причине `" + reason + '\'');

      st1.close();
      st2.close();
      db.commit();
    } finally {
      if (db != null) {
        db.close();
      }
    }
  }
%>
<jsp:include page="WEB-INF/jsp/footer.jsp"/>
