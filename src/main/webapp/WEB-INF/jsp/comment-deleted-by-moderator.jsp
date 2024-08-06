<%--
  ~ Copyright 1998-2024 Linux.org.ru
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
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>

<jsp:include page="/WEB-INF/jsp/head.jsp"/>

<title>${message}</title>
<jsp:include page="/WEB-INF/jsp/header.jsp"/>

<p><c:out value="${message}" escapeXml="true"/></p>

<p>${bigMessage}</p>

<p>
  <a class="btn btn-primary" href="${link}">Назад в топик</a>
  <a class="btn btn-default" href="/people/${author.nick}/profile">Профиль ${author.nick}</a>
  <a class="btn btn-default" href="/sameip.jsp?ip=${ip}">Поиск по IP</a>
  <a class="btn btn-default" href="/sameip.jsp?ip=${ip}&ua=${ua}&mask=0">Поиск по User-Agent</a>
</p>

<jsp:include page="/WEB-INF/jsp/footer.jsp"/>

