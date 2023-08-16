<%--
  ~ Copyright 1998-2023 Linux.org.ru
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
<%@ tag import="ru.org.linux.util.StringUtil" %>
<%--@elvariable id="template" type="ru.org.linux.site.Template"--%>
<%@ tag pageEncoding="UTF-8"%>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib tagdir="/WEB-INF/tags" prefix="lor" %>
<%@ taglib prefix="l" uri="http://www.linux.org.ru" %>
<%@ taglib prefix="form" uri="http://www.springframework.org/tags/form" %>

<%@ attribute name="topic" required="true" type="ru.org.linux.topic.Topic" %>
<%@ attribute name="replyto" required="false" type="java.lang.Integer" %>
<%@ attribute name="original" required="false" type="java.lang.Integer" %>
<%@ attribute name="msg" required="false" type="java.lang.String" %>
<%@ attribute name="mode" required="true" type="java.lang.String" %>
<%@ attribute name="form_action_url" required="true" type="java.lang.String" %>
<%@ attribute name="cancel" required="false" type="java.lang.Boolean" %>
<%@ attribute name="ipBlockInfo" required="false" type="ru.org.linux.auth.IPBlockInfo" %>
<%@ attribute name="postscoreInfo" required="true" type="java.lang.String" %>
<%@ attribute name="modes" required="true" type="java.util.Map" %>

<form:form modelAttribute="add" method="POST" action="${form_action_url}" id="commentForm">
  <lor:csrf/>
  <c:if test="${!template.sessionAuthorized}">
    <div class="control-group">
      <label for="nick">Имя</label>
      <input id="nick" type='text' name='nick' value="anonymous">
    </div>
    <div class="control-group">
      <label for="password">Пароль:</label>
      <input id="password" type=password name=password>
    </div>
    <div class="help-block">
      ${postscoreInfo}
    </div>
  </c:if>

  <input type=hidden name=topic value="${topic.id}">
  <c:if test="${replyto != null}">
    <input type=hidden name=replyto value="<%= replyto %>">
  </c:if>
  <c:if test="${original != null}">
    <input type="hidden" name="original" value="${original}">
  </c:if>
  <c:if test="${not empty modes}">
    <label>Разметка:*<br>
      <form:select path="mode" items="${modes}"/></label><br>
  </c:if>

  <div class="warning-block" id="author-readonly-note">
  </div>

  <div class="control-group">
    <label for="msg">Сообщение</label>
    <textarea id="msg" required name="msg"><%= msg == null ? "" : StringUtil.escapeHtml(msg) %></textarea><br>
    <div class="help-block">Пустая строка (два раза Enter) начинает новый абзац.
                 Знак '&gt;' в начале абзаца выделяет абзац курсивом цитирования.<br>
      <b>Внимание:</b> прочитайте описание разметки <a target="_blank" href="/help/markdown.md">Markdown</a> или <a href="/help/lorcode.md" target="_blank" title="[br] - перевод строки

[b]жирный текст[/b]

[i]курсив[/i]

[u]подчёркнутый текст[/u]

[s]зачёркнутый текст[/s]

[em]emphasis[/em]

[strong]stronger emphasis[/strong]

[pre]preformatted text[/pre]

[user]maxcom[/user] - ссылка на профиль пользователя. 
При использовании этого тега упомянутому пользователю приходит уведомление

[code]код[/code]

[inline]Строчное оформление кода[/inline]

Цитата:
[quote]цитата[/quote] или
[quote='название цитаты']цитата[/quote] или
>>цитата

Ссылка:
[url]http://www.linux.org.ru/[/url] 
можно с параметром, например: 
[url=http://www.example.com/]Сюда![/url]">LORCODE</a>.</div>

  </div>

  <div class="help-block">
    <lor:captcha ipBlockInfo="${ipBlockInfo}" />
  </div>

  <div class="form-actions">
  <button type=submit class="btn btn-primary">Поместить</button>
  <button type=submit name=preview class="btn btn-default">Предпросмотр</button>
  <c:if test="${cancel!=null && cancel}">
    <c:choose>
      <%-- Для режима редактирования --%>
      <c:when test="${original != null}">
        <a class="btn btn-default" href="${topic.link}?cid=${original}">Отменить</a>
      </c:when>
      <%-- Для всех остальных режимов --%>
      <c:otherwise>
        <button type=reset name=cancel id="cancelButton" class="btn btn-default">Отменить</button>
      </c:otherwise>
    </c:choose>
  </c:if>
    <div class="help-block">Используйте Ctrl-Enter для размещения комментария</div>
  </div>
</form:form>
