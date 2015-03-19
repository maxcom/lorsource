<%@ tag import="org.joda.time.DateTime" %>
<%@ tag import="ru.org.linux.site.DateFormats" %>
<%@ tag pageEncoding="utf-8" trimDirectiveWhitespaces="true" %>
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
<%@ attribute name="date" required="true" type="java.util.Date" %><time datetime="<%= DateFormats.iso8601().print(date.getTime()) %>"><%
  long diff = System.currentTimeMillis() - date.getTime();
  DateTime c = new DateTime(date.getTime());

  DateTime today = DateTime.now().withTimeAtStartOfDay();
  DateTime yesterday = DateTime.now().minusDays(1).withTimeAtStartOfDay();

  if (diff<2*1000*60) {
    out.print("минуту назад");
  } else if (diff<1000*60*60) {
    long min = diff / (1000 * 60);

    if (min%10<5 && min%10>1 && (min>20 || min<10)) {
      out.print(min +"&nbsp;минуты назад");
    } else if (min%10==1 && min>20 ) {
        out.print(min +"&nbsp;минута назад");
    } else {
      out.print(min +"&nbsp;минут назад");
    }
  } else if (c.isAfter(today)) {
    out.print("сегодня " + DateFormats.time().print(c));
  } else if (c.isAfter(yesterday)) {
    out.print("вчера " + DateFormats.time().print(c));
  } else {
    out.print(DateFormats.getShort().print(c));
  }
%></time>