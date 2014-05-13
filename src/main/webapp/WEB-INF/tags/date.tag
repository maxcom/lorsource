<%@ tag pageEncoding="UTF-8" trimDirectiveWhitespaces="true" %><%@
        tag import="ru.org.linux.site.DateFormats" %>
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
  --%><%@ attribute name="date" required="true" type="java.util.Date" %><%@
        taglib tagdir="/WEB-INF/tags" prefix="lor" %><%@
        taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %><%@
        attribute name="itemprop" type="java.lang.String" required="false" %><%--
--%><time datetime="<%= DateFormats.iso8601().print(date.getTime()) %>" <c:if test="${not empty itemprop}">itemprop="${itemprop}"</c:if>><%
  out.print(DateFormats.getDefault().print(date.getTime()));
%></time>