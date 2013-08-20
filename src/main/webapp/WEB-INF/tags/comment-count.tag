<%@ tag pageEncoding="UTF-8" trimDirectiveWhitespaces="true" %>
<%--
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
<%@ attribute name="count" required="true" type="java.lang.Integer" %>
<%@ taglib tagdir="/WEB-INF/tags" prefix="lor" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%
  out.append(Integer.toString(count));

  if (count % 100 >= 10 && count % 100 <= 20) {
    out.append("&nbsp;комментариев");
  } else {
    switch (count % 10) {
      case 1:
        out.append("&nbsp;комментарий");
        break;
      case 2:
      case 3:
      case 4:
        out.append("&nbsp;комментария");
        break;
      default:
        out.append("&nbsp;комментариев");
        break;
    }
  }
%>
