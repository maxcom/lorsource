<%@ page contentType="text/html; charset=utf-8"%>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
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
      <li>Fedora 14</li>
      <li>СУБД PostgreSQL 8.4</li>
      <li>OpenJDK 1.6.0</li>
      <li>Apache Tomcat 6</li>
      <li>memcached 1.2</li>
      <li>ActiveMQ 5.3.2</li>
      <li>Spring 3.0</li>
      <li>Wiki: JamWiki 1.0.6</li>
      <li>Поиск: Apache Solr 3.4.0</li>
    </ul>

<h1>Исходные тексты</h1>

  Исходные тексты доступны под лицензией Apache License 2.0: <a href="https://github.com/maxcom/lorsource">https://github.com/maxcom/lorsource</a>

<h1>Наша команда</h1>
Проект реализован и&nbsp;развивается исключительно в&nbsp;свободное время авторов.
  <ul>
  <li><a href="/people/maxcom/profile">Максим Валянский</a> (maxcom)&nbsp;&#8212; <i>координатор
  проекта</i>:
  разработка, поддержка, дизайн, новости, информационное наполнение.

  <li><a href="/people/green/profile">Олег Дрокин</a> (green)&nbsp;&#8212; администрирование сервера, железо
  </ul>

  Модераторы:
  <ul>
<c:forEach var="user" items="${moderators}">
  <li>
    <c:url var="whois" value="/people/${user.nick}/profile"/>
    <a href="${whois}"><c:out escapeXml="true" value="${user.name}"/></a> (<c:out escapeXml="true" value="${user.nick}"/>)
  </li>
</c:forEach>

</ul>

  Корректоры новостей:
  <ul>
    <c:forEach var="user" items="${correctors}">
      <li>
        <c:url var="whois" value="/people/${user.nick}/profile"/>
        <c:choose>
            <c:when test="${not empty user.name}">
                <a href="${whois}"><c:out escapeXml="true" value="${user.name}"/></a> (<c:out escapeXml="true" value="${user.nick}"/>)
            </c:when>
            <c:otherwise>
                <a href="${whois}"><c:out escapeXml="true" value="${user.nick}"/></a>
            </c:otherwise>
        </c:choose>
      </li>
    </c:forEach>
  </ul>
</div>

<h1>Связанные проекты</h1>
<ul>
<li><a href="http://www.lorquotes.ru/">LorQuotes</a>&nbsp;&#8212; избранные цитаты</li>
<li><a href="http://www.lastfm.ru/group/Linux-org-ru">Группа linux.org.ru на&nbsp;last.fm</a></li>
<li><a href="http://community.livejournal.com/l_o_r/">Филиал l.o.r. в&nbsp;ЖЖ</a></li>
</ul>


<jsp:include page="/WEB-INF/jsp/footer.jsp"/>
