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
<%@ page contentType="text/html; charset=utf-8"%>
<%@ taglib tagdir="/WEB-INF/tags" prefix="lor" %>
<%--@elvariable id="preparedTopic" type="ru.org.linux.topic.PreparedTopic"--%>
<%--@elvariable id="image" type="ru.org.linux.gallery.Image"--%>

<jsp:include page="/WEB-INF/jsp/head.jsp"/>

<title>Удаление изображения</title>
<jsp:include page="/WEB-INF/jsp/header.jsp"/>
<h1>Удаление изображения</h1>

<lor:image preparedMessage="${preparedTopic}" showImage="true" showInfo="false"/>

<form method="POST" action="/delete_image">
  <lor:csrf/>
  <input type="hidden" name="id" value="${image.id}">

  <input type="submit" value="Удалить">
</form>

<jsp:include page="/WEB-INF/jsp/footer.jsp"/>

