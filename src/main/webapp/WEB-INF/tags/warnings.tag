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
<%@ tag pageEncoding="UTF-8" trimDirectiveWhitespaces="true"%>
<%@ attribute name="warnings" required="true" type="java.util.List<ru.org.linux.warning.PreparedWarning>" %>
<%@ taglib tagdir="/WEB-INF/tags" prefix="lor" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>

<c:if test="${not empty warnings}">
  <div class="infoblock">
    <c:forEach var="warning" items="${warnings}">
      <div>
        ⚠️${' '} <lor:date date="${warning.postdate}"/> ${' '} <lor:user user="${warning.author}" link="true"/>:
      <c:out value="${warning.message}" escapeXml="true"/>
      </div>
    </c:forEach>
  </div>
</c:if>

