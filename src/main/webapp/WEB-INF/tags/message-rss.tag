<%@ tag import="ru.org.linux.poll.PreparedPoll" %>
<%@ tag import="ru.org.linux.topic.Topic" %>
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
<%@ attribute name="preparedMessage" required="true" type="ru.org.linux.topic.PreparedTopic" %>
<%@ taglib tagdir="/WEB-INF/tags" prefix="lor" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<description><![CDATA[
  <c:if test="${preparedMessage.section.imagepost}">
    <lor:image preparedImage="${preparedMessage.image}" topic="${preparedMessage.message}" showImage="true"/>
  </c:if>

  ${preparedMessage.processedMessage}

  <c:if test="${preparedMessage.section.imagepost}">
    <lor:image preparedImage="${preparedMessage.image}" topic="${preparedMessage.message}" showInfo="true"/>
  </c:if>

  <c:if test="${preparedMessage.section.votePoll}">
    <%
      PreparedPoll poll = preparedMessage.getPoll();
      out.append(poll.renderPoll());
    %>
  </c:if>
  
  <c:if test="${not empty preparedMessage.tags}">
    <p>
      <lor:tags list="${preparedMessage.tags}"/>
    </p>
  </c:if>  
]]>
</description>