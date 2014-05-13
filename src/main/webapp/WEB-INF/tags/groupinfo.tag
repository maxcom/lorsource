<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="sec" uri="http://www.springframework.org/security/tags" %>
<%--
  ~ Copyright 1998-2014 Linux.org.ru
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
<%@ tag pageEncoding="utf-8" %>
<%@ attribute name="group" required="true" type="ru.org.linux.group.PreparedGroupInfo" %>
<c:if test="${not empty group.info}">
  <p style="margin-top: 0"><em>${group.info}</em></p>
</c:if>

<c:if test="${not empty group.longInfo}">
  <div class="infoblock infoblock-small">
  ${group.longInfo}
  <sec:authorize access="hasRole('ROLE_MODERATOR')">
    <p>[<a href="groupmod.jsp?group=${group.id}">править</a>]</p>
  </sec:authorize>
  </div>
</c:if>

