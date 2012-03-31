<%@ page contentType="text/html; charset=utf-8"%>
<%@ page buffer="200kb"%>
<%@ taglib tagdir="/WEB-INF/tags" prefix="lor" %>
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
<%--@elvariable id="preparedMessage" type="ru.org.linux.topic.PreparedTopic"--%>
<%--@elvariable id="template" type="ru.org.linux.site.Template"--%>
<%--@elvariable id="add" type="ru.org.linux.comment.AddCommentRequest"--%>

<jsp:include page="/WEB-INF/jsp/head.jsp"/>

<title>${add.topic.title} - ${preparedMessage.group.title} - ${preparedMessage.section.title}</title>
<jsp:include page="/WEB-INF/jsp/header.jsp"/>
<div class=messages>
  <lor:message messageMenu="<%= null %>" preparedMessage="${preparedMessage}" message="${add.topic}" showMenu="false"/>
</div>

<h2><a name=rep>Добавить сообщение:</a></h2>
<%--<% if (tmpl.getProf().getBoolean("showinfo") && !Template.isSessionAuthorized(session)) { %>--%>
<%--<font size=2>Чтобы просто поместить сообщение, используйте login `anonymous',--%>
<%--без пароля. Если вы собираетесь активно участвовать в форуме,--%>
<%--помещать новости на главную страницу,--%>
<%--<a href="register.jsp">зарегистрируйтесь</a></font>.--%>
<%--<p>--%>

<%--<% } %>--%>
<font size=2><strong>Внимание!</strong> Перед написанием комментария ознакомьтесь с
<a href="rules.jsp">правилами</a> сайта.</font><p>

<lor:commentForm
        topic="${add.topic}"
        title=""
        mode="${add.mode}"
        ipBlockInfo="${ipBlockInfo}"
        postscoreInfo="${preparedMessage.postscoreInfo}" />

<jsp:include page="/WEB-INF/jsp/footer.jsp"/>
