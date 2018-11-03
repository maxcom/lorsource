<%@ tag import="ru.org.linux.poll.PreparedPoll" %>
<%@ tag pageEncoding="UTF-8"%>
<%--
  ~ Copyright 1998-2018 Linux.org.ru
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
<%@ attribute name="preparedTopic" required="true" type="ru.org.linux.topic.PreparedTopic" %>
<%@ taglib tagdir="/WEB-INF/tags" prefix="lor" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="l" uri="http://www.linux.org.ru" %>
<description><![CDATA[
  <c:if test="${preparedTopic.section.imagepost}">
    <lor:image title="${preparedTopic.message.title}" image="${preparedTopic.image}" preparedMessage="${preparedTopic}" showImage="true"/>
  </c:if>

  ${preparedTopic.processedMessage}

  <c:if test="${preparedTopic.section.imagepost}">
    <lor:image title="${preparedTopic.message.title}" image="${preparedTopic.image}" preparedMessage="${preparedTopic}" showInfo="true"/>
  </c:if>

  <c:if test="${preparedTopic.section.pollPostAllowed}">
    <%
      PreparedPoll poll = preparedTopic.getPoll();
      out.append(poll.renderPoll());
    %>
  </c:if>
  
  <c:if test="${not empty preparedTopic.tags}">
      <l:tags list="${preparedTopic.tags}"/>
  </c:if>
]]>
</description>