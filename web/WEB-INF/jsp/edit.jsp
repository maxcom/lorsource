<%@ page contentType="text/html; charset=utf-8"%>
<%@ page
    import="java.sql.Connection,ru.org.linux.site.LorDataSource,ru.org.linux.site.Message" %>
<%@ page import="ru.org.linux.site.Tags" %>
<%@ page import="ru.org.linux.site.Template" %>
<%@ page import="ru.org.linux.util.HTMLFormatter" %>
<%@ taglib tagdir="/WEB-INF/tags" prefix="lor" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
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
<%--@elvariable id="newMsg" type="ru.org.linux.site.Message"--%>
<%--@elvariable id="group" type="ru.org.linux.site.Group"--%>
<%--@elvariable id="info" type="java.lang.String"--%>
<%--@elvariable id="editInfo" type="ru.org.linux.site.EditInfoDTO"--%>

<jsp:include page="/WEB-INF/jsp/head.jsp"/>
<title>Редактирование сообщения</title>
<script src="/js/jquery.validate.pack.js" type="text/javascript"></script>
<script src="/js/jquery.validate.ru.js" type="text/javascript"></script>
<script type="text/javascript">
  $(document).ready(function() {
    $("#messageForm").validate({
      messages : {
        title : "Введите заголовок"
      }
    });
  });
</script>
<jsp:include page="/WEB-INF/jsp/header.jsp"/>
<%
  Connection db = null;
  try {
    db = LorDataSource.getConnection();
    Message newMsg = (Message) request.getAttribute("newMsg");

%>
<c:if test="${info!=null}">
  <h1>${info}</h1>
  <h2>Текущая версия сообщения</h2>
  <div class=messages>
    <lor:message db="<%= db %>" message="${message}" showMenu="false" user="<%= Template.getNick(session) %>"/>
  </div>
  <h2>Ваше сообщение</h2>
</c:if>
<c:if test="${info==null}">
  <h1>Редактирование</h1>
</c:if>

<div class=messages>
  <lor:message db="<%= db %>" message="${newMsg}" showMenu="false" user="<%= Template.getNick(session) %>"/>
</div>

<form action="edit.jsp" name="edit" method="post" id="messageForm">
  <input type="hidden" name="msgid" value="${message.id}">
  <c:if test="${editInfo!=null}">
    <input type="hidden" name="lastEdit" value="${editInfo.editdate.time}">
  </c:if>

  <c:if test="${not message.expired}">
  Заголовок:
  <input type=text name=title class="required" size=40 value="<%= newMsg.getTitle()==null?"":HTMLFormatter.htmlSpecialChars(newMsg.getTitle()) %>" ><br>

  <br>
  <textarea name="newmsg" cols="70" rows="20">${newMsg.message}</textarea>
  <br><br>
    <c:if test="${message.haveLink}">
      Текст ссылки:
      <input type=text name=linktext size=60
             value="<%= newMsg.getLinktext()==null?"":HTMLFormatter.htmlSpecialChars(newMsg.getLinktext()) %>"><br>
      Ссылка :
      <input type=text name=url size=70
             value="<%= newMsg.getUrl()==null?"":HTMLFormatter.htmlSpecialChars(newMsg.getUrl()) %>"><br>
    </c:if>
  </c:if>

  <c:if test="${group.moderated}">
  Теги:
  <input type="text" name="tags" id="tags" value="<%= newMsg.getTags().toString() %>"><br>
  Популярные теги: <%= Tags.getEditTags(Tags.getTopTags(db)) %> <br>
    </c:if>
  <br>

  <input type="submit" value="Отредактировать">
  &nbsp;
  <input type=submit name=preview value="Предпросмотр">
</form>
<%
  } finally {
    if (db != null) {
      db.close();
    }
  }

%>
<jsp:include page="/WEB-INF/jsp/footer.jsp"/>