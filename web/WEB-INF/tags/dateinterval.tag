<%@ tag import="java.text.SimpleDateFormat" %>
<%@ tag import="java.text.DateFormat" %>
<%@ tag import="java.util.Calendar" %>
<%@ tag import="ru.org.linux.site.Template" %>
<%@ tag import="ru.org.linux.site.DateFormats" %>
<%@ tag pageEncoding="utf-8" %> 
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
    out.print("минуту назад");
  } else if (diff<1000*60*60) {
    long min = diff / (1000 * 60)-1;

    if (min%10<5 && min%10>1) {
      out.print(min +" минуты назад");
    } else if (min%10==1 && min>20 ) {
        out.print(min +" минута назад");
    } else {
      out.print(min +" минут назад");
    }
  } else if (c.after(today)) {
    out.print("сегодня, " + c.get(Calendar.HOUR)+":"+c.get(Calendar.MINUTE));
  } else if (c.after(yesterday)) {
    out.print("вчера, " + c.get(Calendar.HOUR)+":"+c.get(Calendar.MINUTE));
  } else {
    out.print(DateFormats.createShort().format(date).replace(" ", "&nbsp;"));
  }
%>