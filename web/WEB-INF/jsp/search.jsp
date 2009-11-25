<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="fn" uri="http://java.sun.com/jsp/jstl/functions" %>
<%@ page contentType="text/html; charset=utf-8"%>
<%@ page import="ru.org.linux.site.SearchViewer"  %>

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
<jsp:include page="/WEB-INF/jsp/head.jsp"/>

<title>Поиск по сайту
  <c:if test="${not initial}">
    - <c:out value="${q}" escapeXml="true"/>
  </c:if>
</title>
<jsp:include page="/WEB-INF/jsp/header.jsp"/>

<H1>Поиск по сайту</h1>
<h2>Поисковая система сайта</h2>

<%
  int include = (Integer) request.getAttribute("include");
  int date = (Integer) request.getAttribute("date");
  int section = (Integer) request.getAttribute("section");
  int sort = (Integer) request.getAttribute("sort");  
%>

<FORM METHOD=GET ACTION="search.jsp">
<INPUT TYPE="text" NAME="q" SIZE=50 VALUE="${fn:escapeXml(q)}">
  <input TYPE="submit" VALUE="Поиск"><BR>
  
  <p>
  <select name="include">
    <option value="topics" <%= (include==SearchViewer.SEARCH_TOPICS)?"selected":"" %>>только темы</option>
    <option value="all" <%= (include==SearchViewer.SEARCH_ALL)?"selected":"" %>>темы и комментарии</option>
  </select>

  За:
  <select name="date">
    <option value="3month" <%= (date==SearchViewer.SEARCH_3MONTH)?"selected":"" %>>три месяца</option>
    <option value="year" <%= (date==SearchViewer.SEARCH_YEAR)?"selected":"" %>>год</option>
    <option value="all" <%= (date==SearchViewer.SEARCH_ALL)?"selected":"" %>>весь период</option>
  </select>
<br>
  Раздел:
  <select name="section">
    <option value="1" <%= (section == 1) ? "selected" : "" %>>новости</option>
    <option value="2" <%= (section == 2) ? "selected" : "" %>>форум</option>
    <option value="3" <%= (section == 3) ? "selected" : "" %>>галерея</option>
    <option value="0" <%= (section == 0) ? "selected" : "" %>>все</option>
  </select>

  Пользователь:
  <INPUT TYPE="text" NAME="username" SIZE=20 VALUE="${fn:escapeXml(username)}">
  <br>

  Сортировать
  <select name="sort">
  <option value="<%= SearchViewer.SORT_DATE %>" <%= (sort==SearchViewer.SORT_DATE)?"selected":"" %>>по дате</option>

  <option value="<%= SearchViewer.SORT_R %>" <%= (sort==SearchViewer.SORT_R)?"selected":"" %>>по релевантности</option>
  </select>

  <br>
</form>

<c:if test="${not initial}">
  <c:out value="${result}" escapeXml="false"/>
  <p>
    <i>
      <c:if test="${cached}">
        Результаты извлечены из кеша, время поиска: ${time} ms
      </c:if>
      <c:if test="${not cached}">
        Результаты извлечены из БД, время поиска: ${time} ms 
      </c:if>
    </i>
  </p>
</c:if>

<c:if test="${initial}">
  <h2>Поиск через Google</h2>
  <jsp:include page="/WEB-INF/jsp/${template.style}/google-search.jsp"/>
</c:if>

<jsp:include page="/WEB-INF/jsp/footer.jsp"/>
