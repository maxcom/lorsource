<%@ tag import="ru.org.linux.util.StringUtil" %>
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
<%--@elvariable id="template" type="ru.org.linux.site.Template"--%>
<%@ tag pageEncoding="UTF-8"%>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib tagdir="/WEB-INF/tags" prefix="lor" %>
<%@ taglib prefix="l" uri="http://www.linux.org.ru" %>

<%@ attribute name="topic" required="true" type="ru.org.linux.topic.Topic" %>
<%@ attribute name="title" required="true" type="java.lang.String" %>
<%@ attribute name="replyto" required="false" type="java.lang.Integer" %>
<%@ attribute name="original" required="false" type="java.lang.Integer" %>
<%@ attribute name="msg" required="false" type="java.lang.String" %>
<%@ attribute name="mode" required="true" type="java.lang.String" %>
<%@ attribute name="form_action_url" required="true" type="java.lang.String" %>
<%@ attribute name="cancel" required="false" type="java.lang.Boolean" %>
<%@ attribute name="ipBlockInfo" required="false" type="ru.org.linux.auth.IPBlockInfo" %>
<%@ attribute name="postscoreInfo" required="true" type="java.lang.String" %>
<form method="POST" action="${form_action_url}" id="commentForm">
  <input type="hidden" name="session"
         value="<%= StringUtil.escapeHtml(session.getId()) %>">
  <c:if test="${!template.sessionAuthorized}">
    <label for="nick">Имя:</label>
    <input id="nick" type='text' name='nick' value="anonymous"><br>
    <label for="password">Пароль:</label>
    <input id="password" type=password name=password><br>
    ${postscoreInfo}
    <br>
  </c:if>

  <input type=hidden name=topic value="${topic.id}">
  <c:if test="${replyto != null}">
    <input type=hidden name=replyto value="<%= replyto %>">
  </c:if>
  <c:if test="${original != null}">
    <input type="hidden" name="original" value="${original}">
  </c:if>
  <c:if test="${template.prof.formatMode == 'ntobr'}">
  <label for="mode">Разметка:*</label><br>
  <select id="mode" name="mode">
  <option value=quot <%= "quot".equals(mode)?"selected":""%> >TeX paragraphs w/quoting
  <option value=ntobr <%= "ntobr".equals(mode)?"selected":""%> >User line breaks w/quoting
  </select>  <br>
  </c:if>

  <label for="title">Заглавие:</label><br>
  <input type=text id="title" name=title value="<l:title><%= title %>"></l:title><br>

  <label for="msg">Сообщение:</label><br>

  <textarea id="msg" required name="msg"><%= msg == null ? "" : StringUtil.escapeHtml(msg) %></textarea><br>
  <font size="2">Пустая строка (два раза Enter) начинает новый абзац.<br>
                 Знак '&gt;' в начале абзаца выделяет абзац курсивом цитирования</font><br>
  <font size="2"><b>Внимание:</b> <a href="/wiki/en/Lorcode" target="_blank">прочитайте описание разметки LORCODE</a></font><br>

  <br>
  <lor:captcha ipBlockInfo="${ipBlockInfo}" />

  <input type=submit value="Поместить">
  <input type=submit name=preview value="Предпросмотр">
  <c:if test="${cancel!=null && cancel}">
    <input type=reset name=cancel value="Отменить" id="cancelButton">
  </c:if>
</form>
