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
<%--@elvariable id="template" type="ru.org.linux.site.Template"--%>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ tag pageEncoding="utf-8" %>
<c:if test="${template.currentUser.unreadEvents > 0}">
  <a href="show-replies.jsp?nick=${template.nick}" onclick="$('#events_form').submit(); return false;"
          >Уведомления (${template.currentUser.unreadEvents})</a>

  <form id="events_form" action="/show-replies.jsp" method="POST" style="display: none;">
    <input type="hidden" name="nick" value="${template.nick}">
  </form>
</c:if>
<c:if test="${template.currentUser.unreadEvents == 0}">
  <a href="show-replies.jsp?nick=${template.nick}">Уведомления</a>
</c:if>
