<%@ page contentType="text/html; charset=utf-8"%>
<%@ page import="java.net.URLEncoder"  %>
<%@ page import="java.sql.Connection" %>
<%@ page import="java.sql.ResultSet" %>
<%@ page import="java.sql.Statement" %>
<%@ page import="ru.org.linux.site.LorDataSource" %>
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

<jsp:include page="/WEB-INF/jsp/head.jsp"/>

<title>О Сервере</title>
<jsp:include page="/WEB-INF/jsp/header.jsp"/>
<div class=text>

<h1>О Проекте</h1>

Некоммерческий проект &laquo;<i>LINUX.ORG.RU: Русская информация об&nbsp;ОС&nbsp;Linux</i>&raquo; был
основан в&nbsp;октябре
1998&nbsp;года. Нашей целью является создание основного информационного ресурса об
операционной системе Linux в&nbsp;России. Мы&nbsp;стараемся обеспечить возможность
обмена различной Linux-ориентированной информацией, последними новостями,
ссылками, документацией и&nbsp;другими ресурсами.

<h1>Наша кнопочка</h1>
Вы можете использовать эту кнопку для ссылки на наш сайт:<br>
<img width=88 height=31 src="/img/button.gif" alt="Кнопка www.linux.org.ru">
  <p>
  и чудо баннер:<br>
  <img src="/img/linux-banner5.gif" alt="www.linux.org.ru" width="468" height="60">
  </p>

<h1>Хостинг</h1>
Размещение сервера и&nbsp;подключение к&nbsp;сети Интернет осуществляется компанией
ООО &laquo;<a href="http://www.ratel.ru">НИИР-РадиоНет</a>&raquo;.
<p>
	Статистику сервера можно посмотреть тут: <a href="http://linuxhacker.ru/stats">статистика</a>.

<h1>Софт</h1>
  <p>
Мы работаем на
  </p>
    <ul>
      <li>Fedora 11</li>
      <li>СУБД PostgreSQL 8.4</li>
      <li>Apache2 2.2</li>
      <li>OpenJDK 1.6.0</li>
      <li>Apache Tomcat 5.5</li>
      <li>memcached 1.2</li>
    </ul>        

<h1>Наша команда</h1>
Проект реализован и&nbsp;развивается исключительно в&nbsp;свободное время авторов.
  <ul>
  <li><a href="whois.jsp?nick=maxcom">Максим Валянский</a> (maxcom)&nbsp;&#8212; <i>координатор
  проекта</i>:
  разработка, поддержка, дизайн, новости, информационное наполнение.

  <li><a href="whois.jsp?nick=green">Олег Дрокин</a> (green)&nbsp;&#8212; администрирование сервера, железо
  </ul>

  Модераторы:
  <ul>
<%
  Connection db = null;

  try {
    db = LorDataSource.getConnection();

    Statement st = db.createStatement();
    ResultSet rs = st.executeQuery("SELECT nick, name FROM users WHERE canmod ORDER BY id");

    while (rs.next()) {
      String nick = rs.getString("nick");
      String name = rs.getString("name");

      if ("maxcom".equals(nick)) {
        continue;    
      }

      out.print("<li><a href=\"whois.jsp?nick="+URLEncoder.encode(nick)+"\">"+name+"</a> ("+nick+ ')');
    }
%>

</ul>

  Корректоры новостей:
  <ul>
<%
  ResultSet rs2 = st.executeQuery("SELECT nick, name FROM users WHERE corrector ORDER BY id");

  while (rs2.next()) {
    String nick = rs2.getString("nick");
    String name = rs2.getString("name");

    if ("maxcom".equals(nick)) {
      continue;
    }

    out.print("<li><a href=\"whois.jsp?nick="+URLEncoder.encode(nick)+"\">"+name+"</a> ("+nick+ ')');
  }

%>
  </ul>
  <%
  } finally {
    if (db!=null) {
      db.close();
    }
  }
  %>
</div>

<h1>Реклама на сайте</h1>
<p>
  Linux.org.ru&nbsp;&#8212; некоммерческий проект, мы&nbsp;не&nbsp;занимаемся размещением рекламы на&nbsp;страничках сайта
  сверх минимума, необходимого для работы сайта. Вы&nbsp;можете разместить рекламу через Google Adsense
  на&nbsp;страничках нашего сайта по&nbsp;ссылке &laquo;<a href="https://adwords.google.com/select/OnsiteSignupLandingPage?client=ca-pub-6069094673001350&amp;referringUrl=http://www.linux.org.ru/">размещение рекламы на&nbsp;этом сайте</a>&raquo;.

</p>
<h1>Связанные проекты</h1>
<ul>
<li><a href="http://www.lorquotes.ru/">LorQuotes</a>&nbsp;&#8212; избранные цитаты</li>
<li><a href="http://www.lastfm.ru/group/Linux-org-ru">Группа linux.org.ru на&nbsp;last.fm</a></li>
<li><a href="http://community.livejournal.com/l_o_r/">Филиал l.o.r. в&nbsp;ЖЖ</a></li>
</ul>


<jsp:include page="/WEB-INF/jsp/footer.jsp"/>
