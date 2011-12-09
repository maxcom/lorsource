<%@ tag import="ru.org.linux.poll.PollVariant" %>
<%@ tag import="ru.org.linux.util.StringUtil" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/functions" prefix="fn" %>
<%@ tag pageEncoding="UTF-8"%>
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
<%@ attribute name="poll" required="true" type="ru.org.linux.poll.PreparedPoll" %>
<table class="poll-result">
<c:forEach var="variant" items="${poll.variants}">
    <tr>
        <td>
            <c:choose>
                <c:when test="${poll.poll.multiSelect}">
                    <input type="checkbox" disabled>
                </c:when>
                <c:otherwise>
                    <input type="radio" disabled>
                </c:otherwise>
            </c:choose>
        </td>
        <td>${fn:escapeXml(variant.label)}</td>
    </tr>
</c:forEach>
</table>
