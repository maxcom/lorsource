<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ page contentType="text/html; charset=utf-8"%>
<%--
  ~ Copyright 1998-2022 Linux.org.ru
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
<%@ taglib tagdir="/WEB-INF/tags" prefix="lor" %>
<%--@elvariable id="template" type="ru.org.linux.site.Template"--%>
<%--@elvariable id="bonus" type="java.lang.Boolean"--%>
<%--@elvariable id="msgid" type="java.lang.Integer"--%>
<%--@elvariable id="author" type="ru.org.linux.user.User"--%>
<%--@elvariable id="draft" type="java.lang.Boolean"--%>
<%--@elvariable id="uncommited" type="java.lang.Boolean"--%>

<jsp:include page="/WEB-INF/jsp/head.jsp"/>

        <title>Удаление сообщения</title>
<jsp:include page="/WEB-INF/jsp/header.jsp"/>

<script language="Javascript">
<!--
function change(dest,source)
{
	dest.value = source.options[source.selectedIndex].value;
}
   // -->
</script>
<h1>Удаление сообщения</h1>
<c:if test="${not draft}">
Вы можете удалить своё сообщение в течении 6 часов с момента
его помещения.
</c:if>
<form method=POST action="delete.jsp" class="form-horizontal">
<lor:csrf/>
  <div class="control-group">
    <label class="control-label" for="reason-input">
      Причина удаления
    </label>

    <div class="controls">
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
          <option value="4.8 Дискуссия не на русском языке">4.8 Дискуссия не на русском языке
          <option value="5.1 Нецензурные выражения">5.1 Нецензурные выражения
          <option value="5.2 Оскорбление участников дискуссии">5.2 Оскорбление участников дискуссии
          <option value="5.3 Национальные/политические/религиозные споры">5.3 Национальные/политические/религиозные
            споры
          <option value="5.4 Личная переписка">5.4 Личная переписка
          <option value="5.5 Преднамеренное нарушение правил русского языка">5.5 Преднамеренное нарушение правил
            русского
            языка
          <option value="6 Нарушение copyright">6 Нарушение copyright
          <option value="6.2 Warez">6.2 Warez
          <option value="7.1 Ответ на некорректное сообщение">7.1 Ответ на некорректное сообщение
        </select><br>
      </c:if>

      <input id="reason-input" type=text name=reason><br>
      <c:if test="${uncommited and template.moderatorSession}">
        Сообщения, удалённые с пустой причиной, не будут показаны в списке удалённых внизу страницы неподтверждённых.
      </c:if>
    </div>
  </div>

  <c:if test="${template.moderatorSession and bonus}">
  <div class="control-group">
    <label class="control-label" for="bonus-input">
      Штраф<br>
      score автора: ${author.score}
    </label>
    <div class="controls">
      <input id="bonus-input" type=number name=bonus value="0" min="0" max="20">
      <span class="help-inline">(от 0 до 20)</span>
    </div>
  </div>
  </c:if>

  <input type=hidden name=msgid value="${msgid}">

  <div class="control-group">
    <div class="controls">
      <button type=submit class="btn btn-danger">Удалить</button>
    </div>
  </div>
</form>
<jsp:include page="/WEB-INF/jsp/footer.jsp"/>
