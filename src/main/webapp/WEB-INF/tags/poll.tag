<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/functions" prefix="fn" %>
<%@ tag pageEncoding="UTF-8"%>
<%--
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
<%@ attribute name="poll" required="true" type="ru.org.linux.poll.PreparedPoll" %>
<div class="poll-result">
    <ol>
<c:forEach var="variant" items="${poll.variants}">
        <li>
            <c:choose>
                <c:when test="${variant.userVoted}">
                    <span class="penguin_label"><b>${fn:escapeXml(variant.label)}</b></span><span class="penguin_percent"><b>${variant.votes} (${variant.percentage}%)</b></span>
                </c:when>
                <c:otherwise>
                    <span class="penguin_label">${fn:escapeXml(variant.label)}</span><span class="penguin_percent">${variant.votes} (${variant.percentage}%)</span>
                </c:otherwise>
            </c:choose>
            <p class="penguin_progress"><span style="width: ${variant.penguinPercent}%"><span>${variant.alt}</span></span></p>
        </li>
</c:forEach>
    </ol>
</div>
<div class="poll-sum">
    <p>Всего голосов: ${poll.totalVotes} <c:if test="${poll.poll.multiSelect}">, всего проголосовавших: ${poll.totalOfVotesPerson} </c:if> </p>
</div>
