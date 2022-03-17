<%--
  ~ Copyright 1998-2022 Linux.org.ru
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
<%@ page contentType="text/html; charset=utf-8"%>
<%@ taglib tagdir="/WEB-INF/tags" prefix="lor" %>

<jsp:include page="/WEB-INF/jsp/head.jsp"/>

<title>Удаленные темы ${user.nick}</title>
<jsp:include page="/WEB-INF/jsp/header.jsp"/>

<h1>Удаленные темы ${user.nick}</h1>

<lor:deleted-topics topics="${topics}" showDates="true"/>

<jsp:include page="/WEB-INF/jsp/footer.jsp"/>
