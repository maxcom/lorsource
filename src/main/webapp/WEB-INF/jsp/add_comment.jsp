<%@ page contentType="text/html; charset=utf-8" %>
<%@ page
        import="ru.org.linux.site.PreparedComment,ru.org.linux.util.HTMLFormatter" %>
<%@ taglib tagdir="/WEB-INF/tags" prefix="lor" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="form" uri="http://www.springframework.org/tags/form" %>
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
<%--@elvariable id="add" type="ru.org.linux.spring.AddCommentRequest"--%>
<%--@elvariable id="onComment" type="ru.org.linux.site.PreparedComment"--%>
<%--@elvariable id="comment" type="ru.org.linux.site.PreparedComment"--%>
<jsp:include page="/WEB-INF/jsp/head.jsp"/>

<title>Добавить сообщение</title>
<jsp:include page="/WEB-INF/jsp/header.jsp"/>

<table class=nav>
<tr>
<td align=left valign=middle id="navPath">
Добавить комментарий
</td>
  <td align=right>
    [<a href="/view-message.jsp?msgid=${add.topic}">Читать комментарии</a>]
  </td>
</tr>
  </table>
<h1 class="optional">Добавить комментарий</h1>

<%--<% if (tmpl.getProf().getBoolean("showinfo") && !Template.isSessionAuthorized(session)) { %>--%>
<%--<font size=2>Чтобы просто поместить сообщение, используйте login `anonymous',--%>
<%--без пароля. Если вы собираетесь активно участвовать в форуме,--%>
<%--помещать новости на главную страницу,--%>
<%--<a href="register.jsp">зарегистрируйтесь</a></font>.--%>
<%--<p>--%>

<%--<% } %>--%>
<font size=2><strong>Внимание!</strong> Перед написанием комментария ознакомьтесь с
  <a href="rules.jsp">правилами</a> сайта.</font>

<p>
  <%
      String title = "";
      Integer replyto = null; 
  %><c:if test="${onComment != null}">
    <%
        PreparedComment onComment = (PreparedComment) request.getAttribute("onComment");

        replyto = onComment.getComment().getId();

        if (onComment.getComment().getTitle().length() > 0) {
            title = onComment.getComment().getTitle();
            if (!title.startsWith("Re:")) {
                title = "Re: " + title;
            }
        }
    %>

    <div class=messages>
        <lor:comment
                showMenu="false"
                comment="${onComment}"
                comments="${null}"
                expired="${false}" topic="${null}"/>
    </div>
</c:if>

<%
      if (request.getParameter("title") != null) {
        title = HTMLFormatter.htmlSpecialChars(request.getParameter("title"));
      }
%>
<c:if test="${comment!=null}">
  <p><b>Ваше сообщение</b></p>
  <div class=messages>
    <lor:comment showMenu="false" comment="${comment}" comments="${null}" expired="${false}" topic="${null}"/>
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

<lor:commentForm
        topic="${topic}"
        title="<%= title %>"
        replyto="<%= replyto %>"
        msg="${add.msg}"
        mode="${add.mode}"/>

<jsp:include page="/WEB-INF/jsp/footer.jsp"/>
