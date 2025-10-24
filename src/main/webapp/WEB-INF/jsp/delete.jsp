<%@ page contentType="text/html; charset=utf-8"%>
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
<%@ taglib tagdir="/WEB-INF/tags" prefix="lor" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="fn" uri="http://java.sun.com/jsp/jstl/functions" %>
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

          <c:forEach var="reason" items="${deleteReasons}">
            <option value="${fn:escapeXml(reason)}">${fn:escapeXml(reason)}
          </c:forEach>
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
      <input id="bonus-input" type=number name=bonus value="7" min="0" max="20">
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
