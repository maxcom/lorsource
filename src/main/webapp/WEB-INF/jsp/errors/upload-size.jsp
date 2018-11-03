<%@ page contentType="text/html; charset=utf-8"%>
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

<%
   response.setStatus(404);
%>
<jsp:include page="/WEB-INF/jsp/head.jsp"/>

<title>Ошибка: слишком крупное изображение</title>
<jsp:include page="/WEB-INF/jsp/header.jsp"/>

<div id="warning-body">
    <div id="warning-logo"><img src="/img/good-penguin.png" alt="good-penguin" /></div>
    <div id="warning-text">
        <h1>Слишком крупное изображение</h1>
        <p>Размер изображения превышает лимит в ${exception.maxUploadSize} Байт</p>
        <p>Вернитесь <a href="javascript:history.back()">назад</a></p>
    </div>
</div>
<div id="warning-footer"></div>


<jsp:include page="/WEB-INF/jsp/footer.jsp"/>
