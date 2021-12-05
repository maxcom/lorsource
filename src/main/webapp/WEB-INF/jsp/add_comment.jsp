<%@ page contentType="text/html; charset=utf-8" %>
<%@ page import="ru.org.linux.comment.PreparedComment" %>
<%@ taglib tagdir="/WEB-INF/tags" prefix="lor" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="form" uri="http://www.springframework.org/tags/form" %>
<%@ taglib prefix="l" uri="http://www.linux.org.ru" %>
<%--
  ~ Copyright 1998-2021 Linux.org.ru
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
<%--@elvariable id="onComment" type="ru.org.linux.comment.PreparedComment"--%>
<%--@elvariable id="comment" type="ru.org.linux.comment.PreparedComment"--%>
<jsp:include page="/WEB-INF/jsp/head.jsp"/>

<title>Добавить комментарий</title>
<jsp:include page="/WEB-INF/jsp/header.jsp"/>

<div class=nav>
<div id="navPath">
Добавить комментарий
</div>
  <div class="nav-buttons">
    <ul>
        <li><a href="/view-message.jsp?msgid=${add.topic.id}">Читать комментарии</a></li>
    </ul>
  </div>
</div>

<%--<% if (tmpl.getProf().getBoolean("showinfo") && !Template.isSessionAuthorized(session)) { %>--%>
<%--<font size=2>Чтобы просто поместить сообщение, используйте login `anonymous',--%>
<%--без пароля. Если вы собираетесь активно участвовать в форуме,--%>
<%--помещать новости на главную страницу,--%>
<%--<a href="register.jsp">зарегистрируйтесь</a></font>.--%>
<%--<p>--%>

<%--<% } %>--%>
<font size=2><strong>Внимание!</strong> Перед написанием комментария ознакомьтесь с
  <a href="/help/rules.md">правилами</a> сайта.</font>

<p>
  <%
      Integer replyto = null;
  %><c:if test="${onComment != null}">
    <%
        PreparedComment onComment = (PreparedComment) request.getAttribute("onComment");

        replyto = onComment.getId();
    %>

    <div class=messages>
        <l:comment
                commentsAllowed="false"
                showMenu="false"
                enableSchema="false"
                comment="${onComment}"
                topic="${add.topic}"/>
    </div>
</c:if>

<c:if test="${comment!=null}">
  <p><b>Ваше сообщение</b></p>
  <div class=messages>
    <l:comment commentsAllowed="false" showMenu="false" comment="${comment}" topic="${add.topic}" enableSchema="false"/>
  </div>
</c:if>

<form:form modelAttribute="add">
    <form:errors path="*" cssClass="error" element="div" />
</form:form>

<c:if test="${error!=null}">
  <div class="error">
      ${error.message}
  </div>
</c:if>

<c:url var="form_action_url" value="/add_comment.jsp" />

<lor:commentForm
        topic="${add.topic}"
        replyto="<%= replyto %>"
        msg="${add.msg}"
        mode="${add.mode}"
        form_action_url="${form_action_url}"
        postscoreInfo="${postscoreInfo}"
        modes="${modes}"/>

<jsp:include page="/WEB-INF/jsp/footer.jsp"/>
