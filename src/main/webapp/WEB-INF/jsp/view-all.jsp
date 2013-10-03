<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ page contentType="text/html; charset=utf-8"%>
<%@ page import="ru.org.linux.section.Section,java.util.Date"   buffer="60kb"%>
<%@ taglib tagdir="/WEB-INF/tags" prefix="lor" %>
<%@ taglib prefix="form" uri="http://www.springframework.org/tags/form" %>
<%--
  ~ Copyright 1998-2013 Linux.org.ru
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
    response.setDateHeader("Expires", new Date(new Date().getTime() - 20 * 3600 * 1000).getTime());
    response.setDateHeader("Last-Modified", new Date(new Date().getTime() - 120 * 1000).getTime());

    Section section = (Section) request.getAttribute("section");
    int sectionid = 0;
    if (section!=null) {
      sectionid=section.getId();
    }

%>
<title>${title}</title>
<jsp:include page="/WEB-INF/jsp/header.jsp"/>

  <c:url var="urlFilterHandler" value="view-all.jsp" />
  <form action="${urlFilterHandler}">

  <div class=nav>
    <div id="navPath">
      ${title}
    </div>

    <div class="nav-buttons">
      <ul>
      <c:if test="${not empty addlink}">
          <li>
              <a href="${addlink}">Добавить</a>
          </li>
      </c:if>
      </ul>
      <select name="section" onChange="submit();" title="Быстрый переход">
        <option value="0">Все</option>
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
    </div>
  </div>
  </form>

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
<tr><th class="hideon-tablet">Группа</th><th>Заголовок</th><th>Причина удаления</th></tr>
<tbody>

<c:forEach items="${deletedTopics}" var="topic">

<tr>
  <td class="hideon-tablet"><a href="group.jsp?group=${topic.groupId}">${topic.gtitle}</a></td>
  <td><a href="view-message.jsp?msgid=${topic.id}">${topic.title}</a> (${topic.nick}<span class="hideon-desktop"> в </span><%--
                  --%><span class="hideon-desktop"><a href="group.jsp?group=${topic.groupId}">${topic.gtitle}</a></span>)</td>
  <td>${topic.reason}</td>
</tr>
</c:forEach>
</table>
</div>
<jsp:include page="/WEB-INF/jsp/footer.jsp"/>
