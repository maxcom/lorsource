<%@ tag import="ru.org.linux.site.PollVariant" %>
<%@ tag import="ru.org.linux.site.Template" %>
<%@ tag import="ru.org.linux.util.StringUtil" %>
<%@ tag import="ru.org.linux.util.ImageInfo" %>
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
<%@ attribute name="highlight" required="false" type="java.util.Set" %>
<%
  out.append(poll.renderPoll());
%>
