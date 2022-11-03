<%@ tag import="ru.org.linux.site.DateFormats" %>
<%@ tag import="org.joda.time.DateTimeZone" %>
<%@ tag pageEncoding="utf-8" trimDirectiveWhitespaces="true" %>
<%--
  ~ Copyright 1998-2022 Linux.org.ru
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
<%@ attribute name="compact" required="false" type="java.lang.Boolean"%>
<%@ attribute name="date" required="true" type="java.util.Date" %><%
  boolean comp = compact!=null && compact;

  if (comp) {
    out.print("<time data-format=\"compact-interval\" datetime=\"" + DateFormats.Iso8601().print(date.getTime())+"\">");
    out.print(DateFormats.formatCompactInterval(date, (DateTimeZone) request.getAttribute("timezone")));
  } else {
    out.print("<time data-format=\"interval\" datetime=\"" + DateFormats.Iso8601().print(date.getTime())+"\">");
    out.print(DateFormats.formatInterval(date, (DateTimeZone) request.getAttribute("timezone")));
  }

  out.print("</time>");
%></time>