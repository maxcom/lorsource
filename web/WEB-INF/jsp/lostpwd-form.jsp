<%@ page contentType="text/html; charset=utf-8"%>
<%@ page buffer="64kb"%>
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

        <title>Получить забытый пароль</title>
<jsp:include page="/WEB-INF/jsp/header.jsp"/>

<H1>Получить забытый пароль</H1>
<form method=POST action="/lostpwd.jsp">
Email:
<input type=text name=email size=40><br>
<input type=submit value="Получить">
</form>
<jsp:include page="/WEB-INF/jsp/footer.jsp"/>
