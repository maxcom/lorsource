<%@ tag import="ru.org.linux.poll.PreparedPoll" %>
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
<%@ attribute name="preparedMessage" required="true" type="ru.org.linux.topic.PreparedTopic" %>
<%@ taglib tagdir="/WEB-INF/tags" prefix="lor" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="l" uri="http://www.linux.org.ru" %>
<description><![CDATA[
  <c:if test="${preparedMessage.section.imagepost}">
    <lor:image preparedMessage="${preparedMessage}" showImage="true"/>
  </c:if>

  ${preparedMessage.processedMessage}

  <c:if test="${preparedMessage.section.imagepost}">
    <lor:image preparedMessage="${preparedMessage}" showInfo="true"/>
  </c:if>

  <c:if test="${preparedMessage.section.pollPostAllowed}">
    <%
      PreparedPoll poll = preparedMessage.getPoll();
      out.append(poll.renderPoll());
    %>
  </c:if>
  
  <c:if test="${not empty preparedMessage.tags}">
      <l:tags list="${preparedMessage.tags}"/>
  </c:if>
]]>
</description>