<%@ page contentType="text/html; charset=utf-8"%>
<%@ page import="java.util.Date"   %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
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
<%--@elvariable id="section" type="ru.org.linux.dto.SectionDto"--%>
<%--@elvariable id="groups" type="java.util.List<ru.org.linux.dto.GroupDto>"--%>
<%--@elvariable id="template" type="ru.org.linux.site.Template"--%>
<jsp:include page="/WEB-INF/jsp/head.jsp"/>
<%

  response.setDateHeader("Expires", new Date(new Date().getTime() - 20 * 3600 * 1000).getTime());
  response.setDateHeader("Last-Modified", new Date(new Date().getTime() - 2 * 1000).getTime());

%>
<title>${section.name}</title>
<link rel="parent" title="Linux.org.ru" href="/">
<LINK REL="alternate" HREF="/section-rss.jsp?section=${section.id}" TYPE="application/rss+xml">
<jsp:include page="/WEB-INF/jsp/header.jsp"/>

  <table class=nav>
    <tr>
      <td align=left valign=middle id="navPath">
        <strong>${section.name}</strong>
      </td>

      <td align=right valign=middle>
        [<a href="add-section.jsp?section=${section.id}">${section.addText}</a>]
        
        <c:if test="${section.id!=2}">
            [<a href="section-rss.jsp?section=${section.id}">RSS</a>]
        </c:if>

        <c:if test="${section.id==2}">
            [<a href="/forum/lenta/">Лента</a>]
            [<a href="section-rss.jsp?section=${section.id}">RSS</a>
            <span id="rss-select">
                <a href="section-rss.jsp?section=${section.id}&filter=notalks">без talks</a>
                <a href="section-rss.jsp?section=${section.id}&filter=tech">тех. разделы форума</a></span>]
        </c:if>
      </td>
    </tr>
  </table>

<h1 class="optional">${section.name}</h1>

Группы:
<ul>

  <c:forEach var="group" items="${groups}">
    <li>
      <a class="navLink" href="${group.url}">${group.title}</a>

      (${group.stat1}/${group.stat2}/${group.stat3})

      <c:if test="${group.info != null}">
        - <em><c:out value="${group.info}" escapeXml="false"/></em>
      </c:if>

    </li>

  </c:forEach>

</ul>

<c:if test="${section.forum}">
<h1>Настройки</h1>
<c:if test="${not template.sessionAuthorized}">
Если вы еще не зарегистрировались - вам <a href="register.jsp">сюда</a>.
</c:if>
<ul>
<li><a href="addphoto.jsp">Добавить фотографию</a>
<li><a href="register.jsp">Изменение регистрации</a>
<li><a href="lostpwd.jsp">Получить забытый пароль</a>
<li><a href="edit-profile.jsp">Персональные настройки сайта</a>
</ul>
</c:if>

<jsp:include page="/WEB-INF/jsp/footer.jsp"/>
