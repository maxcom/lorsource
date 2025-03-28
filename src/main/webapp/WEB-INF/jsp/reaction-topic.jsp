<%@ page pageEncoding="utf-8" contentType="text/html; charset=utf-8"%>
<%@ taglib tagdir="/WEB-INF/tags" prefix="lor" %>
<%--
  ~ Copyright 1998-2025 Linux.org.ru
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
<%--@elvariable id="message" type="ru.org.linux.topic.Topic"--%>
<%--@elvariable id="preparedMessage" type="ru.org.linux.topic.PreparedTopic"--%>

<jsp:include page="/WEB-INF/jsp/head.jsp"/>

<title>Реакция на сообщение</title>
<jsp:include page="/WEB-INF/jsp/header.jsp"/>

<div class=messages>
  <lor:topic messageMenu="<%= null %>" preparedMessage="${preparedTopic}" message="${topic}" showMenu="false"
             reactionList="${reactionList}" imageSlider="true"/>

  <a class="btn btn-primary" href="${preparedTopic.message.link}">Вернуться</a>
</div>

<jsp:include page="/WEB-INF/jsp/footer.jsp"/>
