<%@ tag import="ru.org.linux.site.PollVariant" %>
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
<%@ attribute name="poll" required="true" type="ru.org.linux.site.PreparedPoll" %>
<table class="poll-result">
<c:forEach var="variant" items="${poll.variants}">
    <tr>
        <td>${fn:escapeXml(variant.label)}</td>
        <td>${variant.votes}</td>
        <%
            PollVariant variant = (PollVariant)jspContext.getAttribute("variant");
            int variantWidth = 20*variant.getVotes()/poll.getMaximumValue();
            request.setAttribute("width", variantWidth*16); // пингвин 16px вширь!
            request.setAttribute("alt", StringUtil.repeat("*", variantWidth));
        %>
        <td width="380"><div class="penguin" style="width:${width}px;">&nbsp;<span class="none">${alt}</span></div></td>
    </tr>
</c:forEach>
    <tr><td colspan=2>Всего голосов: ${poll.totalVotes}</td></tr>
    <c:if test="${poll.poll.multiSelect}">
        <tr><td colspan=2>Всего проголосовавших: ${poll.totalOfVotesPerson}</td></tr>
    </c:if>
</table>
