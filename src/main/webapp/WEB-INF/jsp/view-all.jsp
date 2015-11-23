<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ page contentType="text/html; charset=utf-8"%>
<%@ page import="ru.org.linux.section.Section"%>
<%@ taglib tagdir="/WEB-INF/tags" prefix="lor" %>
<%@ taglib prefix="form" uri="http://www.springframework.org/tags/form" %>
<%--
  ~ Copyright 1998-2015 Linux.org.ru
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
<%--@elvariable id="messages" type="java.util.List<ru.org.linux.topic.PersonalizedPreparedTopic>"--%>
<%--@elvariable id="template" type="ru.org.linux.site.Template"--%>
<%--@elvariable id="deletedTopics" type="java.util.List<ru.org.linux.topic.TopicListDto.DeletedTopic>"--%>
<%--@elvariable id="sections" type="java.util.List<ru.org.linux.section.Section>"--%>
<%--@elvariable id="section" type="ru.org.linux.section.Section"--%>

<jsp:include page="/WEB-INF/jsp/head.jsp"/>

<%
    Section section = (Section) request.getAttribute("section");
    int sectionid = 0;
    if (section!=null) {
      sectionid=section.getId();
    }

%>
<title>${title}</title>
<jsp:include page="/WEB-INF/jsp/header.jsp"/>

<h1>${title}</h1>

<nav>
  <c:if test="${section!=null}">
    <a class="btn btn-default" href="view-all.jsp">Все</a>
  </c:if>

  <c:if test="${section==null}">
    <a class="btn btn-selected" href="view-all.jsp">Все</a>
  </c:if>

  <c:forEach items="${sections}" var="item">
    <c:if test="${item.premoderated}">
      <c:if test="${section!=null && item.id == section.id}">
        <a href="view-all.jsp?section=${item.id}" class="btn btn-selected">${item.name}</a>
      </c:if>
      <c:if test="${item.id != section.id}">
        <a href="view-all.jsp?section=${item.id}" class="btn btn-default">${item.name}</a>
      </c:if>
    </c:if>
  </c:forEach>

  <c:if test="${not empty addlink}">
    <a class="btn btn-primary" href="${addlink}">Добавить</a>
  </c:if>
</nav>

<strong>Внимание!</strong> Cообщения отображаются точно так же, как
они будут выглядеть на главной странице. Если ваше сообщение отображается не так, как вы хотели, или
в нем не работают какие-либо ссылки, пожалуйста,
исправьте его.<p>
<strong>Внимание модераторам!</strong> Не подтверждайте сразу
много скриншотов, дайте им повисеть на главной странице.<p>
<c:forEach var="msg" items="${messages}">
  <lor:news messageMenu="${msg.topicMenu}"
          preparedMessage="${msg.preparedTopic}"
          multiPortal="<%= sectionid==0 %>"
          moderateMode="true"/>
</c:forEach>
<%
%>
<h2>Последние удаленные неподтвержденные</h2>
<div class=forum>
<table class="message-table" width="100%">
<thead>
<tr>
  <th>Заголовок</th>
  <th>Причина удаления</th>
  <c:if test="${template.moderatorSession}">
    <th>Дата</th>
  </c:if>
</tr>
<tbody>

<c:forEach items="${deletedTopics}" var="topic">

<tr>
  <td><a href="view-message.jsp?msgid=${topic.id}">${topic.title}</a> (${topic.nick})</td>
  <td>${topic.reason}</td>
  <c:if test="${template.moderatorSession}">
    <td>
      написано <lor:dateinterval date="${topic.postDate}"/><br>
      удалено <lor:dateinterval date="${topic.delDate}"/>
    </td>
  </c:if>
</tr>
</c:forEach>
</table>
</div>
<jsp:include page="/WEB-INF/jsp/footer.jsp"/>
