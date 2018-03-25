<%@ tag import="com.google.common.base.Strings" trimDirectiveWhitespaces="true" %><%@ tag
        pageEncoding="UTF-8"
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
  --%><%@
        attribute name="link" required="false" type="java.lang.Boolean" %><%@
        attribute name="rel" required="false" type="java.lang.String" %><%@
        attribute name="user" type="ru.org.linux.user.User" %><%@
        attribute name="itemprop" type="java.lang.String" required="false" %><%@
        taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %><%@
        taglib prefix="fn" uri="http://java.sun.com/jsp/jstl/functions" %><%--
--%><c:if test="${user.blocked}"><s></c:if><%--
--%><c:choose><%--
--%><c:when test="${link!=null and link and not user.anonymous}"><%--
--%><a <%= Strings.isNullOrEmpty(rel)?"":"rel=\""+rel+ '"' %> <%= Strings.isNullOrEmpty(itemprop)?"":"itemprop=\""+itemprop+ '"' %> href="/people/${user.nick}/profile">anonymous</a><%--
--%></c:when><%--
--%><c:otherwise><%--
--%>anonymous<%--
--%></c:otherwise><%--
--%></c:choose><%--
--%><c:if test="${user.blocked}"><%--
--%></s><%--
--%></c:if>
