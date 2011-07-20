<%@ tag import="java.util.Calendar" %>
<%@ tag import="ru.org.linux.site.DateFormats" %>
<%@ tag pageEncoding="utf-8" %>

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
<%@ attribute name="date" required="true" type="java.util.Date" %><%
  long diff = System.currentTimeMillis() - date.getTime();
  Calendar c = Calendar.getInstance();
  c.setTime(date);

  Calendar today = Calendar.getInstance();
  today.set(Calendar.HOUR_OF_DAY, 0);
  today.set(Calendar.MINUTE, 0);
  today.set(Calendar.SECOND, 0);
  today.set(Calendar.MILLISECOND, 0);

  Calendar yesterday = (Calendar) today.clone();
  yesterday.roll(Calendar.DAY_OF_MONTH, false);

  if (diff<2*1000*60) {
    out.print("минуту&nbsp;назад");
  } else if (diff<1000*60*60) {
    long min = diff / (1000 * 60);

    if (min%10<5 && min%10>1 && (min>20 || min<10)) {
      out.print(min +"&nbsp;минуты&nbsp;назад");
    } else if (min%10==1 && min>20 ) {
        out.print(min +"&nbsp;минута&nbsp;назад");
    } else {
      out.print(min +"&nbsp;минут&nbsp;назад");
    }
  } else if (c.after(today)) {
    out.print("сегодня&nbsp;" + DateFormats.createTime().format(date));
  } else if (c.after(yesterday)) {
    out.print("вчера&nbsp;" + DateFormats.createTime().format(date));
  } else {
    out.print(DateFormats.createShort().format(date).replace(" ", "&nbsp;"));
  }
%>
