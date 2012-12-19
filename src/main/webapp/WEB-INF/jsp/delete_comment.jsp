<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ page contentType="text/html; charset=utf-8" %>
<%@ taglib tagdir="/WEB-INF/tags" prefix="lor" %>
<%--
  ~ Copyright 1998-2012 Linux.org.ru
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
<%--@elvariable id="topic" type="ru.org.linux.topic.Topic"--%>
<%--@elvariable id="msgid" type="java.lang.Integer"--%>
<%--@elvariable id="template" type="ru.org.linux.site.Template"--%>
<%--@elvariable id="commentsPrepared" type="java.util.List<ru.org.linux.comment.PreparedComment>"--%>
<%--@elvariable id="comments" type="ru.org.linux.comment.CommentList"--%>

<jsp:include page="/WEB-INF/jsp/head.jsp"/>

<title>Удаление сообщения</title>
<jsp:include page="/WEB-INF/jsp/header.jsp"/>
<script language="Javascript" type="text/javascript">
  <!--
  function change(dest, source)
  {
    dest.value = source.options[source.selectedIndex].value;
  }
  // -->
</script>
<h1>Удаление сообщения</h1>
Вы можете удалить свое сообщение в течении часа с момента
его помещения.
<form method=POST action="delete_comment.jsp">
  <lor:csrf/>
  <table>
    <tr>
      <td>Причина удаления
      <td>
        <c:if test="${template.moderatorSession}">
        <select name=reason_select onChange="change(reason,reason_select);">
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
          <option value="5.3 Национальные/политические/религиозные споры">5.3
            Национальные/политические/религиозные споры
          <option value="5.4 Личная переписка">5.4 Личная переписка
          <option value="5.5 Преднамеренное нарушение правил русского языка">5.5 Преднамеренное
            нарушение правил русского языка
          <option value="6 Нарушение copyright">6 Нарушение copyright
          <option value="6.2 Warez">6.2 Warez
          <option value="7.1 Ответ на некорректное сообщение">7.1 Ответ на некорректное сообщение
        </select>
        </c:if>
      </td>
    </tr>
    <tr>
      <td></td>
      <td><input type=text name=reason size=40></td>
    </tr>

    <c:if test="${template.moderatorSession}">
    <tr>
      <td>Штраф score (от 0 до 20)</td>
      <td><input type=text name=bonus size=40 value="7"></td>
    </tr>
    </c:if>
  </table>
  <input type=hidden name=msgid value="${msgid}">
  <input type=submit value="Delete/Удалить">
</form>
<div class="messages">
  <div class="comment">
    <c:forEach var="comment" items="${commentsPrepared}">
      <lor:comment commentsAllowed="false" showMenu="true" comment="${comment}"
                   expired="${topic.expired}" topic="${topic}"/>
    </c:forEach>

  </div>
</div>
<jsp:include page="/WEB-INF/jsp/footer.jsp"/>
