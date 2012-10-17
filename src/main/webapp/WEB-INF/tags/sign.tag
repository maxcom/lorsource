<%@ tag import="ru.org.linux.site.Template" %>
<%@ tag
        pageEncoding="UTF-8"
%><%--
  ~ Copyright 1998-2012 Linux.org.ru
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
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib tagdir="/WEB-INF/tags" prefix="lor" %>
<%@ attribute name="shortMode" type="java.lang.Boolean" %><%@
        attribute name="user" type="ru.org.linux.user.User" %><%@
        attribute name="postdate" type="java.sql.Timestamp" %>

<lor:user rel="author" decorate="true" link="true" user="${user}"/>

<c:if test="${not shortMode and not user.anonymous}">
  ${user.stars}

  <c:if test="${template.moderatorSession}">
    (Score: ${user.score} MaxScore: ${user.maxScore})
  </c:if>
</c:if>

(<lor:date date="${postdate}"/>)

