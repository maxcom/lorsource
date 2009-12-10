<%@ tag import="java.sql.Connection" %>
<%@ tag import="java.text.DateFormat" %>
<%@ tag import="java.util.List" %>
<%@ tag import="java.util.Set" %>
<%@ tag import="ru.org.linux.site.*" %>
<%@ tag import="ru.org.linux.util.ServletParameterParser" %>
<%@ tag import="ru.org.linux.util.StringUtil" %>
<%@ tag pageEncoding="UTF-8"%>

<%--
  ~ Copyright 1998-2009 Linux.org.ru
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
<%@ attribute name="date" required="true" type="java.util.Date" %><%
  DateFormat dateFormat = DateFormats.createDefault();

  out.print(dateFormat.format(date));
%>

