<%@ page contentType="text/html; charset=utf-8" %>
<%@ page
        import="java.sql.Connection,ru.org.linux.site.Comment,ru.org.linux.site.LorDataSource" %>
<%@ page import="ru.org.linux.util.HTMLFormatter" %>
<%@ taglib tagdir="/WEB-INF/tags" prefix="lor" %>
<%--
  ~ Copyright 1998-2009 Linux.org.ru
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

<%
  int topicId = (Integer) request.getAttribute("topic");
  int postscore = (Integer) request.getAttribute("postscore");
%>
<jsp:include page="/WEB-INF/jsp/head.jsp"/>

<%
  Exception error = (Exception) request.getAttribute("error");
  String mode = (String) request.getAttribute("mode");
  Comment comment = (Comment) request.getAttribute("comment");

%>

<title>Добавить сообщение</title>
<jsp:include page="/WEB-INF/jsp/header.jsp"/>

<h1>Добавить комментарий</h1>

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
    Connection db = null;

    try {
      db = LorDataSource.getConnection();

      String title = "";
      Integer replyto = null;

      Comment onComment = (Comment) request.getAttribute("onComment");

      if (onComment != null) {
        replyto = onComment.getId();

        title = onComment.getTitle();
        if (!title.startsWith("Re:")) {
          title = "Re: " + title;
        }
%>
<div class=messages>
  <lor:comment
          showMenu="false"
          comment="<%= onComment %>"
          db="<%= db %>"
          comments="${null}"
          expired="${false}"/>
</div>
  <%
      }

      if (request.getParameter("title") != null) {
        title = HTMLFormatter.htmlSpecialChars(request.getParameter("title"));
      }

      if (comment != null) {
%>
  <p><b>Ваше сообщение</b></p>
  <div class=messages>
    <lor:comment showMenu="false" comment="<%= comment %>" db="<%= db %>" comments="${null}" expired="${false}"/>
  </div>

<% if (error != null) { %>
<div class="error">
  ${error.message}
</div>
<% }
  }
%>

<lor:commentForm
        topicId="<%= topicId %>"
        title="<%= title %>"
        replyto="<%= replyto %>"
        msg="<%=request.getParameter(&quot;msg&quot;)%>"
        mode="<%= mode %>"
        postscore="<%= postscore %>"/>

<%
    } finally {
      if (db != null) {
        db.close();
      }
    }
  %>

<jsp:include page="/WEB-INF/jsp/footer.jsp"/>
