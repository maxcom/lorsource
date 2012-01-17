<%@ tag import="ru.org.linux.poll.PreparedPoll" %>
<%@ tag import="ru.org.linux.topic.Topic" %>
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
<%@ attribute name="preparedMessage" required="true" type="ru.org.linux.topic.PreparedTopic" %>
<%@ taglib tagdir="/WEB-INF/tags" prefix="lor" %>
<description><![CDATA[<%
    Topic topic = preparedMessage.getMessage();

    if (preparedMessage.getSection().isImagepost()) {
%>
  <lor:image preparedImage="${preparedMessage.image}" topic="${preparedMessage.message}" showImage="true"/>
  <lor:image preparedImage="${preparedMessage.image}" topic="${preparedMessage.message}" showInfo="true"/>
  <%
    } else if (topic.isVotePoll()) {
      PreparedPoll poll = preparedMessage.getPoll();
      out.append(poll.renderPoll());
    } else {
      out.append(preparedMessage.getProcessedMessage());
    }
%>]]>
</description>