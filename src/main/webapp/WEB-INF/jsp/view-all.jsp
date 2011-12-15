<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ page contentType="text/html; charset=utf-8"%>
<%@ page import="java.util.Date,ru.org.linux.section.Section"   buffer="60kb"%>
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
<%--@elvariable id="messages" type="java.util.List<ru.org.linux.site.PreparedTopic>"--%>
<%--@elvariable id="template" type="ru.org.linux.site.Template"--%>
<%--@elvariable id="deletedTopics" type="java.util.List<ru.org.linux.spring.NewsViewerController.DeletedTopic>"--%>
<%--@elvariable id="sections" type="java.util.List<ru.org.linux.section.Section>"--%>
<%--@elvariable id="section" type="ru.org.linux.section.Section"--%>

<jsp:include page="/WEB-INF/jsp/head.jsp"/>

<%
    response.setDateHeader("Expires", new Date(new Date().getTime() - 20 * 3600 * 1000).getTime());
    response.setDateHeader("Last-Modified", new Date(new Date().getTime() - 120 * 1000).getTime());

    Section section = (Section) request.getAttribute("section");
    int sectionid = 0;
    if (section!=null) {
      sectionid=section.getId();
    }

%>
<title>Просмотр неподтвержденных сообщений - <%= section==null?"Все":section.getName() %></title>
<jsp:include page="/WEB-INF/jsp/header.jsp"/>

  <form action="view-all.jsp">

  <table class=nav><tr>
    <td align=left valign=middle id="navPath">
      Просмотр неподтвержденных сообщений - <%= section==null?"Все":section.getName() %>
    </td>

    <td align=right valign=middle>
      [<a style="text-decoration: none" href="rules.jsp">Правила форума</a>]
      [<a style="text-decoration: none" href="tags.jsp">Метки</a>]

      <select name=section onChange="submit();" title="Быстрый переход">
        <option value=0>Все</option>
        <c:forEach items="${sections}" var="item">
          <c:if test="${item.premoderated}">
            <c:if test="${section!=null && item.id == section.id}">
              <option value="${item.id}" selected>${item.name}</option>
            </c:if>
            <c:if test="${item.id != section.id}">
              <option value="${item.id}">${item.name}</option>
            </c:if>
          </c:if>
        </c:forEach>
      </select>
    </td>

  </tr>
 </table>
</form>

<h1 class="optional"><%= section==null?"П":(section.getName()+": п") %>росмотр неподтвержденных</h1>
<strong>Внимание!</strong> Cообщения отображаются точно так же, как
они будут выглядеть на главной странице. Если ваше сообщение отображается не так, как вы хотели, или
в нем не работают какие-либо ссылки, пожалуйста,
исправьте его.<p>
<strong>Внимание модераторам!</strong> Не подтверждайте сразу
много скриншотов, дайте им повисеть на главной странице.<p>
<c:forEach var="msg" items="${messages}">
  <lor:news
          message="${msg.message}" preparedMessage="${msg}"
          multiPortal="<%= sectionid==0 %>"
          moderateMode="true"/>
</c:forEach>
<%
%>
<h2>Последние удаленные неподтвержденные</h2>
<div class=forum>
<table class="message-table" width="100%">
<thead>
<tr><th>&nbsp;<a name="undelete" title="Восстановить">#</a>&nbsp;</th><th>Автор</th><th>Группа</th><th>Заголовок</th><th>Причина удаления</th></tr>
<tbody>

<c:forEach items="${deletedTopics}" var="topic">

<tr>
  <td align="center">
    <c:if test="${template.moderatorSession}">
      <a href="/undelete.jsp?msgid=${topic.id}" title="Восстановить">#</a>
    </c:if>
  </td>
  <td><a href="/people/${topic.nick}/profile">${topic.nick}</a></td>
  <td><a href="group.jsp?group=${topic.groupId}">${topic.ptitle} - ${topic.gtitle}</a></td>
  <td><a href="view-message.jsp?msgid=${topic.id}">${topic.title}</a></td>
  <td>${topic.reason}</td>
</tr>
</c:forEach>
</table>
</div>
<jsp:include page="/WEB-INF/jsp/footer.jsp"/>
