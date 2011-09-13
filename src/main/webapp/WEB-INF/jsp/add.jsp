<%@ page contentType="text/html; charset=utf-8" import="org.apache.commons.lang.StringUtils,ru.org.linux.site.Group"  %>
<%@ page import="ru.org.linux.site.ScreenshotProcessor"%>
<%@ page import="ru.org.linux.site.Template"%>
<%@ page import="ru.org.linux.spring.AddMessageForm" %>
<%@ page import="ru.org.linux.spring.dao.TagDao" %>
<%@ page import="ru.org.linux.util.HTMLFormatter" %>
<%@ page import="java.util.SortedSet" %>
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
<%--@elvariable id="message" type="ru.org.linux.site.PreparedMessage"--%>
<%--@elvariable id="group" type="ru.org.linux.site.Group"--%>
<%--@elvariable id="template" type="ru.org.linux.site.Template"--%>
<% Template tmpl = Template.getTemplate(request);%>
<jsp:include page="/WEB-INF/jsp/head.jsp"/>
<%@ taglib tagdir="/WEB-INF/tags" prefix="lor" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="form" uri="http://www.springframework.org/tags/form" %>

<%
    AddMessageForm oldForm = (AddMessageForm) request.getAttribute("oldForm");
    Group group = (Group) request.getAttribute("group");
    SortedSet<String> topTags = (SortedSet<String>) request.getAttribute("topTags");

    String mode = oldForm.getMode();
%>

<title>Добавить сообщение</title>
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
  if (!oldForm.getNoinfo()) {
    out.print(request.getAttribute("addportal"));
  }
%>
<c:if test="${message != null}">
<h1>Предпросмотр</h1>
<div class=messages>
  <lor:message messageMenu="<%= null %>" preparedMessage="${message}" message="${message.message}" showMenu="false" user="${template.nick}"/>
</div>
</c:if>
<h1>Добавить</h1>
<%--<% if (tmpl.getProf().getBoolean("showinfo") && !Template.isSessionAuthorized(session)) { %>--%>
<%--<font size=2>Чтобы просто поместить сообщение, используйте login `anonymous',--%>
<%--без пароля. Если вы собираетесь активно участвовать в форуме,--%>
<%--помещать новости на главную страницу,--%>
<%--<a href="register.jsp">зарегистрируйтесь</a></font>.--%>
<%--<p>--%>
<%--<% } %>--%>

<% if (group!=null && group.isImagePostAllowed()) { %>
<p>
  Технические требования к изображению:
  <ul>
    <li>Ширина x Высота:
      от <%= ScreenshotProcessor.MIN_SCREENSHOT_SIZE %>x<%= ScreenshotProcessor.MIN_SCREENSHOT_SIZE %>
      до <%= ScreenshotProcessor.MAX_SCREENSHOT_SIZE %>x<%= ScreenshotProcessor.MAX_SCREENSHOT_SIZE %> пикселей</li>
    <li>Тип: jpeg, gif, png</li>
    <li>Размер не более <%= (ScreenshotProcessor.MAX_SCREENSHOT_FILESIZE / 1024) - 50 %> Kb</li>
  </ul>
</p>
<%   } %>

<form:form modelAttribute="form" id="messageForm" method="POST" action="add.jsp" enctype="${group.imagePostAllowed?'multipart/form-data':'application/x-www-form-urlencoded'}" >
  <form:errors path="*" element="div" cssClass="error"/>

  <input type="hidden" name="session" value="<%= HTMLFormatter.htmlSpecialChars(session.getId()) %>">
<%  if (oldForm.getNoinfo()) {
  %>
  <input type="hidden" name="noinfo" value="1">
 <% }
%>
<% if (!tmpl.isSessionAuthorized()) { %>
Имя:
<input type=text name=nick value="<%= oldForm.getNick()==null?"anonymous":HTMLFormatter.htmlSpecialChars(oldForm.getNick()) %>" size=40><br>
Пароль:
<input type=password name=password size=40><br>
<% } %>
<form:hidden path="group"/>

<% if (oldForm.getReturnUrl()!=null) { %>
<input type=hidden name=return value="<%= HTMLFormatter.htmlSpecialChars(oldForm.getReturnUrl()) %>">
<% } %>

<label>Заглавие:
<form:input path="title" cssClass="required" size="40"/><br>
</label>

  <% if (group!=null && group.isImagePostAllowed()) { %>
  Изображение:
  <input type="file" name="image"><br>
  <% } %>

<label for="form_msg">Сообщение:</label><br>
<font size=2>(В режиме <i>Tex paragraphs</i> игнорируются переносы строк.<br> Пустая строка (два раза Enter) начинает новый абзац)</font><br>
<font size="2"><b>Внимание:</b> Новый режим - <a href="/wiki/en/Lorcode">LORCODE</a></font><br>
<form:textarea path="msg" cols="70" rows="20" id="form_msg"/><br>

<% if (group!=null && group.isLinksAllowed()) { %>
<label>
Текст ссылки:
<input type=text name=linktext size=60 value="<%= oldForm.getLinktext()==null?group.getDefaultLinkText():HTMLFormatter.htmlSpecialChars(oldForm.getLinktext()) %>">
</label><br>
<label>
Ссылка (не забудьте <b>http://</b>)
<form:input path="url" size="70"/><br>
</label>
<% } %>
  <c:if test="${group.moderated}">
Метки (разделенные запятой)
<input type=text name=tags id="tags" size=70 value="<%= oldForm.getTags()==null?"":HTMLFormatter.htmlSpecialChars(StringUtils.strip(oldForm.getTags())) %>"><br>
  Популярные теги: <%= TagDao.getEditTags(topTags) %> <br>
  </c:if>
<select name=mode>
<option value=lorcode <%= ("lorcode".equals(mode))?"selected":""%> >LORCODE
<option value=ntobr <%= ("ntobr".equals(mode))?"selected":""%> >User line break
</select>

  <lor:captcha/>
<br>
<input type=submit value="Поместить">
<input type=submit name=preview value="Предпросмотр">
</form:form>
<jsp:include page="/WEB-INF/jsp/footer.jsp"/>
