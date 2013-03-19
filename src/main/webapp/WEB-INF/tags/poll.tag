<%@ tag import="ru.org.linux.poll.PollVariantResult" %>
<%@ tag import="ru.org.linux.util.StringUtil" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/functions" prefix="fn" %>
<%@ tag pageEncoding="UTF-8"%>
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
<%@ attribute name="poll" required="true" type="ru.org.linux.poll.PreparedPoll" %>
<div class="poll-result">
<c:forEach var="variant" items="${poll.variants}">
    <ol>
        <li>
            <span>${fn:escapeXml(variant.label)} ${variant.votes} (${variant.percentage}%)</span>
            <p class="penguin_progress"><span style="width: ${variant.penguinPercent}%"><span>${variant.alt}%</span></span></p>
        </li>
    </ol>
</c:forEach>
    <p>Всего голосов: ${poll.totalVotes}</p>
    <c:if test="${poll.poll.multiSelect}">
        <p>Всего проголосовавших: ${poll.totalOfVotesPerson}</p>
    </c:if>
</div>