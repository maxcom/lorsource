<%--
  ~ Copyright 1998-2026 Linux.org.ru
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
<%@ page session="false" contentType="text/html; charset=utf-8"%>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib tagdir="/WEB-INF/tags" prefix="lor" %>

<jsp:include page="/WEB-INF/jsp/head.jsp"/>

<title>О сервере</title>
<jsp:include page="/WEB-INF/jsp/header.jsp"/>

<h1>О проекте</h1>

<p>
  Проект &laquo;<i>LINUX.ORG.RU: Клуб пользователей&nbsp;ОС&nbsp;Linux и OpenSource</i>&raquo; был
  основан в&nbsp;октябре
  1998&nbsp;года. Нашей целью является создание основной русскоязычной площадки для общения об
  операционной системе Linux и свободном программном обеспечении.
</p>

<p>
  Проект реализован и&nbsp;развивается исключительно в&nbsp;свободное время авторов.
</p>

<h1>Наша команда</h1>
  <ul>
  <li><a href="/people/maxcom/profile">Максим Валянский</a> (maxcom)&nbsp;&#8212; создатель
  проекта:
  разработка, поддержка, дизайн, новости, информационное наполнение.
  </ul>

  Модераторы:
  <ul>
    <c:forEach var="user" items="${moderators}">
      <li>
        <lor:user user="${user._1()}" link="true" bold="${user._2()}"/>
        <c:if test="${not empty user._1().name}">
          (<c:out escapeXml="true" value="${user._1().name}"/>)
        </c:if>
      </li>
    </c:forEach>
  </ul>

  Корректоры новостей:
  <ul>
    <c:forEach var="user" items="${correctors}">
      <li>
        <lor:user user="${user._1()}" link="true" bold="${user._2()}"/>
        <c:if test="${not empty user._1().name}">
          (<c:out escapeXml="true" value="${user._1().name}"/>)
        </c:if>
      </li>
    </c:forEach>
  </ul>

<h1>Поддержать проект</h1>

CloudTips (СБП, T-Pay, банковские карты): <a href="https://pay.cloudtips.ru/p/daf3803e" target="_blank">форма перевода</a>.

<h1>Хостинг</h1>

<p>
  Размещение сервера и&nbsp;подключение к&nbsp;сети Интернет осуществляется компанией
  &laquo;<a href="https://selectel.ru/?ref_code=3dce4333ba" target="_blank">Selectel</a>&raquo;.
</p>
<p>
  Защиту сайта от DDoS-атак осуществляет <a href="http://qrator.net/" target="_blank">QRATOR</a>.
</p>
<p>
  <a href="https://ping-admin.com/uptime/a0005fd81d119564b8eab22d30a83c521169395.html" target="_blank">
    <img src="//images.ping-admin.ru/i/uptime/f56ef6b27e20049339fbfae23ef4e82a1169395_103.gif" width="88" height="31" border="0" alt="Uptime по данным Ping-Admin.Com - сервиса мониторинга доступности сайтов">
  </a>

<h1>Сервер</h1>
<p>
  Сервер для сайта предоставлен &laquo;<a href="http://www.ittelo.ru/" target="_blank">ITTelo</a>&raquo;.
</p>

<p>
Конфигурация:
  <ul>
    <li>Supermicro 6016T-UF;</li>
    <li>2 x Intel Xeon X5675 12M Cache, 3.06 GHz, 6 ядер;</li>
    <li>96 GB DDR3, ECC, REG;</li>
    <li>2x 960Gb Samsung PM883 (RAID1 mdadm);</li>
    <li>1Tb Seagate Constellation ES.3.</li>
  </ul>
</p>

<h1>Софт</h1>
<p>
  Мы работаем на:
  <ul>
    <li>AlmaLinux OS 9.x;</li>
    <li>СУБД PostgreSQL 16;</li>
    <li>OpenJDK 25;</li>
    <li>Scala 3.10;</li>
    <li>Apache Tomcat 11.0;</li>
    <li>ActiveMQ 6.x;</li>
    <li>Spring 7.x;</li>
    <li>OpenSearch 3.x;</li>
    <li>Nginx 1.x.</li>
  </ul>
</p>

<h1>Исходные тексты</h1>

Исходные тексты  <a target="_blank" href="https://github.com/maxcom/lorsource">доступны</a> под лицензией Apache License 2.0.

<h2>Entypo font</h2>
На сайте используется иконочный шрифт Entypo, Copyright (C) 2012 by Daniel Bruce,
<a href="http://www.entypo.com" target="_blank">http://www.entypo.com</a>. Шрифт с нужным нам набором символов
сгенерирован при помощи <a href="http://fontello.com/" target="_blank">fontello.com</a>.

<h2>BSD Daemon</h2>
Права на изображение <a href="https://www.mckusick.com/beastie/" target="_blank">BSD Daemon</a>
принадлежат <a href="https://www.mckusick.com/beastie/mainpage/copyright.html" target="_blank">Marshall Kirk McKusick</a>.

<h2>Twemoji</h2>
На сайте используются изображения Emoji, созданные проектом <a href="https://github.com/jdecked/twemoji" target="_blank">Twemoji</a>.

<jsp:include page="/WEB-INF/jsp/footer.jsp"/>
