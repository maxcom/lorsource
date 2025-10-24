<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ page contentType="text/html; charset=utf-8" %>
<%@ taglib tagdir="/WEB-INF/tags" prefix="lor" %>
<%--
  ~ Copyright 1998-2024 Linux.org.ru
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
<%@ taglib prefix="l" uri="http://www.linux.org.ru" %>
<%@ taglib prefix="fn" uri="http://java.sun.com/jsp/jstl/functions" %>

<jsp:include page="/WEB-INF/jsp/head.jsp"/>

<title>Удаление комментария</title>
<jsp:include page="/WEB-INF/jsp/header.jsp"/>
<script language="Javascript" type="text/javascript">
  <!--
  function change(dest, source)
  {
    dest.value = source.options[source.selectedIndex].value;
  }
  // -->
</script>
<h1>Удаление комментария</h1>
Вы можете удалить свой комментарий в течении трех часов с момента
его помещения, если на него еще нет ответов.
<form method=POST action="delete_comment.jsp" class="form-horizontal">
  <lor:csrf/>
  <div class="control-group">
    <label class="control-label" for="reason-input">
      Причина удаления
    </label>

    <div class="controls">
      <c:if test="${template.moderatorSession}">
          <select name=reason_select onChange="change(reason,reason_select);">
            <option value="">

            <c:forEach var="reason" items="${deleteReasons}">
              <option value="${fn:escapeXml(reason)}">${fn:escapeXml(reason)}
            </c:forEach>
          </select><br>
      </c:if>

      <input id="reason-input" type=text name=reason>
    </div>
  </div>

  <c:if test="${template.moderatorSession and not topic.expired}">
  <div class="control-group">
    <label class="control-label" for="bonus-input">
      Штраф<br>
      score автора: ${commentsPrepared[0].author.score}
    </label>
    <div class="controls">
      <input id="bonus-input" type=number name=bonus value="7" min="0" max="20">
      <span class="help-inline">(от 0 до 20)</span>
    </div>
  </div>
  </c:if>

  <c:if test="${template.moderatorSession}">
  <div class="control-group">
    <label class="control-label" for="delete_replys">Удалять ответы</label>
    <div class="controls">
      <input id="delete_replys" type="checkbox" name="delete_replys">
      <span class="help-block">Внимание! Значение по умолчанию изменено!</span>
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

<div class="messages">
  <div class="comment">
    <c:forEach var="comment" items="${commentsPrepared}">
      <lor:comment commentsAllowed="false" showMenu="true" comment="${comment}" topic="${topic}"/>
    </c:forEach>

  </div>
</div>
<jsp:include page="/WEB-INF/jsp/footer.jsp"/>
