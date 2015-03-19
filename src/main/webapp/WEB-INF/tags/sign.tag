<%@ tag
        pageEncoding="UTF-8" trimDirectiveWhitespaces="true"
%><%--
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
<%--@elvariable id="template" type="ru.org.linux.site.Template"--%>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib tagdir="/WEB-INF/tags" prefix="lor" %>
<%@ attribute name="shortMode" type="java.lang.Boolean" %><%@
        attribute name="user" type="ru.org.linux.user.User" %><%@
        attribute name="author" type="java.lang.Boolean" required="false" %><%@
        attribute name="postdate" type="java.util.Date" %><%@
        attribute name="timeprop" type="java.lang.String" required="false" %>

<c:if test="${author}">
  <lor:user rel="author" itemprop="creator" link="true" user="${user}"/>
</c:if>
<c:if test="${not author}">
  <lor:user itemprop="creator" link="true" user="${user}"/>
</c:if>

<c:if test="${not shortMode and not user.anonymous}">
  <c:out value=" "/>${user.stars}

  <c:if test="${template.moderatorSession}">
    (Score: ${user.score} MaxScore: ${user.maxScore})
  </c:if>
</c:if>

(<lor:date date="${postdate}" itemprop="${timeprop}"/>)

