<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="l" uri="http://www.linux.org.ru" %>
<%@ page contentType="text/html; charset=utf-8"%>
<%@ taglib tagdir="/WEB-INF/tags" prefix="lor" %>
<%--
  ~ Copyright 1998-2014 Linux.org.ru
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
<%--@elvariable id="message" type="ru.org.linux.topic.Topic"--%>
<%--@elvariable id="editHistories" type="java.util.List<ru.org.linux.edithistory.PreparedEditHistory>"--%>
<meta name="robots" content="noindex">
<jsp:include page="/WEB-INF/jsp/head.jsp"/>
<title>История изменений</title>
<script type="text/javascript">
  $script('/js/diff_match_patch.js', function() {
     $script('/js/lor_view_diff_history.js');
  });
</script>
<jsp:include page="/WEB-INF/jsp/header.jsp"/>
<h1>История изменений</h1>
<div id="historyButtonBar"></div>
<div class="messages">
  <c:forEach items="${editHistories}" var="editHistory">
    <p>
      <c:if test="${editHistory.original}">
        Исходная версия
      </c:if>

      <c:if test="${not editHistory.original}">
        Исправление
      </c:if>

      <lor:user link="true" user="${editHistory.editor}"/>,
      <lor:dateinterval date="${editHistory.editDate}"/>

      <c:if test="${editHistory.current}">
        (текущая версия)
      </c:if>

      :
    </p>
    <div class="msg">
      <div class="msg_header">
        <h2>${editHistory.title}</h2>
      </div>
      <div class="msg_body">
        ${editHistory.message}
      </div>
      <div class="msg_footer">
        <c:if test="${(editHistory.linktext != null) || (editHistory.url != null)}">
          <p>&gt;&gt;&gt; <a href="${editHistory.url==null ? "#" : editHistory.url}">${editHistory.linktext==null ? "(текст ссылки не изменен)" : editHistory.linktext}</a>
        </c:if>
        <c:if test="${editHistory.tags != null}">
            <l:tags list="${editHistory.tags}"/>
        </c:if>
        <c:if test="${editHistory.minor != null}">
            <em>Мини-новость: ${editHistory.minor}</em>
        </c:if>
      </div>
    </div>
</c:forEach>
</div>

<jsp:include page="/WEB-INF/jsp/footer.jsp"/>
