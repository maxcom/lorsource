<%--
  ~ Copyright 1998-2026 Linux.org.ru
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
<%@ attribute name="form_action_url" required="true" type="java.lang.String" %>
<%@ attribute name="cancel" required="false" type="java.lang.Boolean" %>
<%@ attribute name="ipBlockInfo" required="false" type="ru.org.linux.auth.IPBlockInfo" %>
<%@ attribute name="postscoreInfo" required="true" type="java.lang.String" %>
<%@ attribute name="lazyCaptcha" required="false" type="java.lang.Boolean" %>
<%@ attribute name="autoFocus" required="false" type="java.lang.Boolean" %>
<%@ attribute name="formatModeFormId" required="false" type="java.lang.String" %>
<%@ attribute name="formatModeTitle" required="false" type="java.lang.String" %>

<c:if test="${empty formatModeFormId}">
  <c:set var="formatModeFormId" value="${template.formatMode}" />
</c:if>
<c:if test="${empty formatModeTitle}">
  <c:set var="formatModeTitle" value="${template.formatModeTitle}" />
</c:if>

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

  <div class="warning-block" id="author-readonly-note">
  </div>

  <div class="control-group" data-format-mode="${formatModeFormId}">
    <div class="markup-tabs">
      <ul class="markup-tabs__nav">
        <li class="markup-tabs__tab active" data-tab="editor">${formatModeTitle}</li>
      </ul>
      <div class="markup-tabs__content">
        <div class="markup-tabs__panel active" data-panel="editor">
          <c:choose>
            <c:when test="${autoFocus}">
              <form:textarea id="msg" required="true" name="msg" path="msg" autofocus="autofocus"/>
            </c:when>
            <c:otherwise>
              <form:textarea id="msg" required="true" name="msg" path="msg"/>
            </c:otherwise>
          </c:choose>
        </div>
      </div>
    </div>
    <div class="help-block">Пустая строка (два раза Enter) начинает новый абзац.
                 Знак '&gt;' в начале абзаца выделяет абзац курсивом цитирования.<br>
      <lor:markup-help mode="${formatModeFormId}"/>
    </div>
  </div>

  <div class="help-block">
    <lor:captcha ipBlockInfo="${ipBlockInfo}" lazy="${lazyCaptcha}" />
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
