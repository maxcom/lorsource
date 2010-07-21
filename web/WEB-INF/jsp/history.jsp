<%@ page import="java.sql.Connection" %>
<%@ page import="ru.org.linux.site.LorDataSource" %>
<%@ page import="ru.org.linux.site.EditInfoDTO" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ page contentType="text/html; charset=utf-8"%>
<%@ taglib tagdir="/WEB-INF/tags" prefix="lor" %>
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
<%--@elvariable id="message" type="ru.org.linux.site.Message"--%>
<%--@elvariable id="messageText" type="java.lang.String"--%>
<%--@elvariable id="editInfos" type="java.util.List<ru.org.linux.site.PreparedEditInfo>"--%>

<jsp:include page="/WEB-INF/jsp/head.jsp"/>
<title>История изменений</title>
<jsp:include page="/WEB-INF/jsp/header.jsp"/>
<h1>История изменений</h1>

<div class="messages">
  <div class="msg">
    <div class="msg_body">
      ${messageText}
    </div>
  </div>

  <c:forEach items="${editInfos}" var="editInfo">
    <p>
      Исправление <lor:user user="${editInfo.editor}"/>,
      <lor:dateinterval date="${editInfo.editInfo.editdate}"/>
    </p>
    <div class="msg">
      <div class="msg_body">
        ${editInfo.oldMessage}
      </div>
    </div>
</c:forEach>
</div>

<jsp:include page="/WEB-INF/jsp/footer.jsp"/>


