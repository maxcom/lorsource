<%@ page info="last active topics" %>
<%@ page contentType="text/html; charset=utf-8" %>
<%@ page
        import="ru.org.linux.site.Template"
        buffer="200kb" %>
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
<%--@elvariable id="newUsers" type="java.util.List<ru.org.linux.site.User>"--%>
<%--@elvariable id="msgs" type="java.util.List<ru.org.linux.spring.TrackerController.Item>"--%>

<% Template tmpl = Template.getTemplate(request); %>
<jsp:include page="/WEB-INF/jsp/head.jsp"/>
<%@ taglib tagdir="/WEB-INF/tags" prefix="lor" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="fn" uri="http://java.sun.com/jsp/jstl/functions" %>

<%
    String title = "Последние сообщения";
    if ((Boolean) request.getAttribute("mine")) {
      title += " (мои темы)";
    }
%>

<title><%= title %>
</title>
<jsp:include page="/WEB-INF/jsp/header.jsp"/>

<form action="tracker.jsp">

  <table class=nav>
    <tr>
      <td align=left valign=middle id="navPath">
        <%= title %>
      </td>

      <td align=right valign=middle>
        <select name="filter">
          <c:if test="${filter=='notalks'}">
            <option value="all">все сообщения</option>
            <option value="notalks" selected="selected">без Talks</option>
            <option value="tech">тех. разделы форума</option>
            <c:if test="${template.sessionAuthorized}">
              <option value="mine">мои темы</option>
            </c:if>
          </c:if>
          <c:if test="${filter=='tech'}">
            <option value="all">все сообщения</option>
            <option value="notalks">без Talks</option>
            <option value="tech" selected="selected">тех. разделы форума</option>
            <c:if test="${template.sessionAuthorized}">
              <option value="mine">мои темы</option>
            </c:if>
          </c:if>
          <c:if test="${filter==null || filter == 'all'}">
            <option value="all" selected="selected">все сообщения</option>
            <option value="notalks">без Talks</option>
            <option value="tech">тех. разделы форума</option>
            <c:if test="${template.sessionAuthorized}">
              <option value="mine">мои темы</option>
            </c:if>
          </c:if>
          <c:if test="${filter=='mine'}">
            <option value="all">все сообщения</option>
            <option value="notalks">без Talks</option>
            <option value="tech">тех. разделы форума</option>
            <option value="mine" selected="selected">мои темы</option>
          </c:if>
        </select>
        <input type="submit" value="показать">
      </td>
    </tr>
  </table>
</form>

<h1 class="optional"><%= title %>
</h1>

<div class=forum>
  <table width="100%" class="message-table">
    <thead>
    <tr>
      <th>Группа</th>
      <th>Заголовок</th>
      <th>Последнее<br>сообщение</th>
      <th>Число ответов<br>всего/день/час</th>
    </tr>
    </thead>
    <tbody>
    <c:forEach var="msg" items="${msgs}">

      <tr>
        <td>
          <a href="${msg.groupUrl}">
              ${msg.groupTitle}
          </a>
          <c:if test="${msg.uncommited}">
            (не подтверждено)
          </c:if>
        </td>
        <td>
          <c:if test="${filter=='mine' && msg.resolved}">
            <img src="/img/solved.png" alt="решено" title="решено"/>
          </c:if>
          <% if (tmpl.getProf().getBoolean("newfirst")) { %>
          <a href="${msg.urlReverse}">
                <% } else { %>
            <a href="${msg.url}">
              <% } %>
                ${msg.title}
            </a> (<lor:user user="${msg.author}" decorate="true"/>)
        </td>
        <td class="dateinterval">
          <lor:dateinterval date="${msg.postdate}"/>
          <c:if test="${msg.lastCommentBy != null}">
            (<lor:user user="${msg.lastCommentBy}" decorate="true"/>)
          </c:if>
        </td>
        <td align='center'>
          <c:if test="${msg.stat1==0}">-</c:if><%--
          --%><c:if test="${msg.stat1>0}"><b>${msg.stat1}</b></c:if>/<c:if
                test="${msg.stat3==0}">-</c:if><%--
          --%><c:if test="${msg.stat3>0}"><b>${msg.stat3}</b></c:if>/<c:if
                test="${msg.stat4==0}">-</c:if><%--
          --%><c:if test="${msg.stat4>0}"><b>${msg.stat4}</b></c:if>
      </tr>
    </c:forEach>
    </tbody>

  </table>
</div>

<table class="nav">
  <tr>
    <td align="left">
      <c:if test="${offset>0}">
        <a href="tracker.jsp?offset=${offset-topics}${query}">← предыдущие</a>
      </c:if>
    </td>
    <td align="right">
      <c:if test="${offset+topics<300 and fn:length(msgs)==topics}">
        <a href="tracker.jsp?offset=${offset+topics}${query}">следующие →</a>
      </c:if>
    </td>
  </tr>
</table>

<c:if test="${newUsers!=null}">
  <h2>Новые пользователи</h2>
  Новые пользователи за последние 3 дня:
  <c:forEach items="${newUsers}" var="user">
    <c:if test="${user.activated}">
      <b>
    </c:if>
    <c:if test="${user.blocked}">
      <s>
    </c:if>
    <a href="/people/${user.nick}/profile">${user.nick}</a>
    <c:if test="${user.blocked}">
      </s>
    </c:if>
    <c:if test="${user.activated}">
      </b>
    </c:if>
  </c:forEach>
  (всего ${fn:length(newUsers)})
</c:if>
<jsp:include page="/WEB-INF/jsp/footer.jsp"/>
