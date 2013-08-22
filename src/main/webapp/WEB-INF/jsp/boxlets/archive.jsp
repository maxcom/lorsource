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

<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt" %>
<%@ taglib prefix="lor" uri="http://www.linux.org.ru" %>
<%@ taglib prefix="fn" uri="http://java.sun.com/jsp/jstl/functions"%>
<fmt:setLocale value="ru" scope="session"/>
<c:url var="head_url" value="/news/"/>
<h2><a href="${head_url}">Архив Новостей</a></h2>

<div class="boxlet_content">
  <c:forEach var="item" items="${items}">
    <c:url value="/news/archive/${item.year}/${item.month}" var="item_url"/>
    <fmt:parseDate var="item_date" value="${item.year} ${item.month}" pattern="yyyy M"/>
    <a href="${fn:escapeXml(item_url)}"><fmt:formatDate value="${item_date}" pattern="yyyy MMMM"/>
      (${item.count})</a> <br>
  </c:forEach>
  <br>&gt;&gt;&gt; <a href="${head_url}archive/"> Предыдущие месяцы</a> (с октября 1998)
</div>