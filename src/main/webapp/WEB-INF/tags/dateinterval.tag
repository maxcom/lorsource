<%@ tag import="org.joda.time.DateTime" %>
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
<%@ attribute name="date" required="true" type="java.util.Date" %><time datetime="<%= DateFormats.iso8601().print(date.getTime()) %>"><%
  long diff = System.currentTimeMillis() - date.getTime();
  boolean comp = compact!=null && compact;
  DateTime c = new DateTime(date.getTime());
  DateTimeZone timezone = (DateTimeZone) request.getAttribute("timezone");

  DateTime today = DateTime.now().withZone(timezone).withTimeAtStartOfDay();
  DateTime yesterday = DateTime.now().withZone(timezone).minusDays(1).withTimeAtStartOfDay();

  if (diff<2*1000*60) {
    if (comp) {
      out.print("1 мин");
    } else {
      out.print("минуту назад");
    }
  } else if (diff<1000*60*60) {
    long min = diff / (1000 * 60);

    if (comp) {
      out.print(min+"&nbsp;мин");
    } else {
      if (min % 10 < 5 && min % 10 > 1 && (min > 20 || min < 10)) {
        out.print(min + "&nbsp;минуты назад");
      } else if (min % 10 == 1 && min > 20) {
        out.print(min + "&nbsp;минута назад");
      } else {
        out.print(min + "&nbsp;минут назад");
      }
    }
  } else if (c.isAfter(today)) {
    if (comp) {
      out.print(DateFormats.time(timezone).print(c));
    } else {
      out.print("сегодня " + DateFormats.time(timezone).print(c));
    }
  } else if (c.isAfter(yesterday)) {
    if (comp) {
      out.print("вчера");
    } else {
      out.print("вчера " + DateFormats.time(timezone).print(c));
    }
  } else {
    if (comp) {
      out.print(DateFormats.date(timezone).print(c));
    } else {
      out.print(DateFormats.getShort(timezone).print(c));
    }
  }
%></time>