<%@ tag import="ru.org.linux.site.Template" %>
<%@ tag import="ru.org.linux.util.StringUtil" %>
<%--
  ~ Copyright 1998-2010 Linux.org.ru
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
<%@ tag pageEncoding="UTF-8"%>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib tagdir="/WEB-INF/tags" prefix="lor" %>

<%@ attribute name="topic" required="true" type="ru.org.linux.topic.Topic" %>
<%@ attribute name="title" required="true" type="java.lang.String" %>
<%@ attribute name="replyto" required="false" type="java.lang.Integer" %>
<%@ attribute name="msg" required="false" type="java.lang.String" %>
<%@ attribute name="mode" required="true" type="java.lang.String" %>
<%@ attribute name="cancel" required="false" type="java.lang.Boolean" %>
<%@ attribute name="ipBlockInfo" required="false" type="ru.org.linux.auth.IPBlockInfo" %>
<form method="POST" action="add_comment.jsp" id="commentForm">
  <input type="hidden" name="session"
         value="<%= StringUtil.escapeHtml(session.getId()) %>">
  <% if (!Template.isSessionAuthorized(session)) { %>
  <label for="nick">Имя:</label>
  <input id="nick" type='text' name='nick' value="anonymous" size=40><br>
  <label for="password">Пароль:</label>
  <input id="password" type=password name=password size=40><br>
  ${topic.postScoreInfo}
  <br>
  <% } %>

  <input type=hidden name=topic value="${topic.id}">
  <c:if test="${replyto != null}">
    <input type=hidden name=replyto value="<%= replyto %>">
  </c:if>

  <label for="title">Заглавие:</label>
  <input type=text id="title" name=title size=40 value="<%= title %>"><br>

  <label for="msg">Сообщение:</label><br>
  <font size=2>(В режиме <i>Tex paragraphs</i> игнорируются переносы строк.<br> Пустая строка (два
    раза Enter) начинает новый абзац.<br> Знак '&gt;' в начале абзаца выделяет абзац курсивом
    цитирования)</font><br>
  <font size="2"><b>Внимание:</b> Новый режим - <a href="/wiki/en/Lorcode" target="_blank">LORCODE</a></font><br>

  <textarea id="msg" class="required" name="msg" cols="70"
            rows="20"><%= msg == null ? "" : StringUtil.escapeHtml(msg)
  %></textarea><br>

  <select name=mode>
  <option value=quot <%= "quot".equals(mode)?"selected":""%> >TeX paragraphs w/quoting
  <option value=ntobr <%= "ntobr".equals(mode)?"selected":""%> >User line breaks w/quoting
  <option value=lorcode <%= "lorcode".equals(mode)?"selected":""%> >LORCODE
  </select>  <br>

  ${topic.postScoreInfo}

  <br>
  <lor:captcha ipBlockInfo="${ipBlockInfo}" />

  <input type=submit value="Поместить">
  <input type=submit name=preview value="Предпросмотр">
  <c:if test="${cancel!=null && cancel}">
    <input type=reset name=cancel value="Отменить" id="cancelButton">
  </c:if>
</form>
