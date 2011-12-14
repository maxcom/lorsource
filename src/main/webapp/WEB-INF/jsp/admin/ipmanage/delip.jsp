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

<jsp:include page="/WEB-INF/jsp/head.jsp"/>
<%

  response.setDateHeader("Expires", new Date(new Date().getTime() - 20 * 3600 * 1000).getTime());
  response.setDateHeader("Last-Modified", new Date(new Date().getTime() - 2 * 1000).getTime());

%>
<title>delip</title>
<link rel="parent" title="Linux.org.ru" href="/">
<jsp:include page="/WEB-INF/jsp/header.jsp"/>

<h1>Удаление тем и сообщений</h1>

${message}

<br/>

Удалено тем: ${topics}

<ul>

  <c:forEach var="del" items="${deleted}">

    <li>
    
Удаляется #${del.key}: ${del.value}

    </li>
                      
  </c:forEach>
                        
</ul>

<jsp:include page="/WEB-INF/jsp/footer.jsp"/>

