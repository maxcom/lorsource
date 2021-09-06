<%@ page import="org.joda.time.DateTime" %>
<%@ page contentType="text/html; charset=utf-8" %>
<%@ taglib tagdir="/WEB-INF/tags" prefix="lor" %>
<%@ taglib prefix="l" uri="http://www.linux.org.ru" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="fn" uri="http://java.sun.com/jsp/jstl/functions" %>
<%@ taglib prefix="spring" uri="http://www.springframework.org/tags" %>
<%--
  ~ Copyright 1998-2019 Linux.org.ru
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
<%--@elvariable id="user" type="ru.org.linux.user.User"--%>
<%--@elvariable id="userpic" type="ru.org.linux.user.Userpic"--%>
<%--@elvariable id="userInfo" type="ru.org.linux.user.UserInfo"--%>
<%--@elvariable id="userStat" type="ru.org.linux.user.UserStats"--%>
<%--@elvariable id="template" type="ru.org.linux.site.Template"--%>
<%--@elvariable id="currentUser" type="java.lang.Boolean"--%>
<%--@elvariable id="ignored" type="java.lang.Boolean"--%>
<%--@elvariable id="moderatorOrCurrentUser" type="java.lang.Boolean"--%>
<%--@elvariable id="banInfo" type="ru.org.linux.user.BanInfo"--%>
<%--@elvariable id="remark" type="ru.org.linux.user.Remark"--%>
<%--@elvariable id="hasRemarks" type="java.lang.Boolean"--%>
<%--@elvariable id="userlog" type="java.util.List<ru.org.linux.user.PreparedUserLogItem>"--%>

<jsp:include page="/WEB-INF/jsp/head.jsp"/>

<title>Информация о пользователе ${user.nick}</title>

<c:if test="${user.id!=2}">
    <script type="text/javascript">
        $script(['/webjars/d3/d3.min.js'], 'd3');

        $script.ready('d3', function () {
            $script('/webjars/cal-heatmap/cal-heatmap.js', 'heatmap');
        });

        $script.ready(['heatmap', 'jquery', 'plugins'], function () {
            $(function () {
                if (window.matchMedia("(min-width: 768px)").matches) {
                    moment.locale("ru");

                    var size = 8;

                    if (window.matchMedia("(min-width: 1024px)").matches) {
                        size = 10;
                    }

                    var cal = new CalHeatMap();
                    cal.init({
                        data: "/people/${user.nick}/profile?year-stats",
                        domain: "month",
                        subDomain: "day",
                        range: 12,
                        domainDynamicDimension: false,
                        displayLegend: false,
                        legend: [8, 32, 64, 128],
                        cellSize: size,
                        start: new Date("<%= DateTime.now().minusMonths(11).toString() %>"),
                        tooltip: true,
                        domainLabelFormat: function (date) {
                            return moment(date).format("MMMM");
                        },
                        subDomainDateFormat: function (date) {
                            return moment(date).format("LL");
                        },
                        subDomainTitleFormat: {
                            empty: "{date}",
                            filled: "{date}<br>сообщений: {count}"
                        }
                    });
                }
            });
        });

        function change (dest, source) {
            dest.value = source.options[source.selectedIndex].value;
        }
    </script>
</c:if>

<c:if test="${userInfo.url != null}">
    <c:if test="${user.score >= 100 && not user.blocked && user.activated}">
        <link rel="me" href="${fn:escapeXml(userInfo.url)}">
    </c:if>
</c:if>
<link rel="alternate" href="/people/${user.nick}/?output=rss" type="application/rss+xml">

<jsp:include page="header.jsp"/>

<c:if test="${currentUser}">
    <div style="margin-bottom: 1em">
        <a href="/people/${user.nick}/edit" class="btn btn-default">Редактировать профиль</a>
        <a href="/people/${user.nick}/settings" class="btn btn-default">Настройки</a>

        <form action="logout" method="POST" style="display: inline-block">
            <lor:csrf/>
            <button type="submit" class="btn btn-danger">Выйти</button>
        </form>

        <form action="logout_all_sessions" method="POST" style="display: inline-block">
            <lor:csrf/>
            <button type="submit" class="btn btn-danger">Выйти со всех устройств</button>
        </form>
    </div>
</c:if>

<c:if test="${not currentUser}">
<h1>Информация о пользователе ${user.nick}</h1>
</c:if>

<div id="whois_userpic">
    <l:userpic userpic="${userpic}"/>
    <c:if test="${moderatorOrCurrentUser}">
        <span>
        <c:if test="${user.photo != ''}">
            <form name='f_remove_userpic' method='post' action='remove-userpic.jsp'>
                <lor:csrf/>
                <input type='hidden' name='id' value='${user.id}'>
                <button type="submit" class="btn btn-danger btn-small">Удалить</button>
            </form>
        </c:if>
        <c:if test="${currentUser}">
            <form method="get" action="addphoto.jsp">
                <button type="submit" class="btn btn-default btn-small">Изменить</button>
            </form>
        </c:if>
        </span>
    </c:if>
</div>
<div>

<div class="vcard">
    <b>Nick:</b> <span class="nickname">
        ${user.nick}
        <c:if test="${isFrozen}"> ❄</c:if>
    </span><br>
    <c:if test="${not empty user.name}">
        <b>Полное имя:</b> <span class="fn">${user.name}</span><br>
    </c:if>
    <b>ID:</b> ${user.id}<br>
    <c:if test="${template.sessionAuthorized and !currentUser}">
        <br><b>Комментарий:</b> <c:out value="${remark.text}" escapeXml="true"/>
        [<a href="/people/${user.nick}/remark/">Изменить</a>]
    </c:if>
    <br>

    <c:if test="${not empty userInfo.url and (template.sessionAuthorized or user.maxScore>=50)}">
        <b>URL:</b>

        <c:choose>
            <c:when test="${user.score < 100 || user.blocked || not user.activated}">
                <a class="url" href="${fn:escapeXml(userInfo.url)}" rel="nofollow">${fn:escapeXml(userInfo.url)}</a><br>
            </c:when>
            <c:otherwise>
                <a class="url" href="${fn:escapeXml(userInfo.url)}">${fn:escapeXml(userInfo.url)}</a><br>
            </c:otherwise>
        </c:choose>
    </c:if>

    <c:if test="${not empty userInfo.town}">
        <b>Город:</b> <c:out value="${userInfo.town}" escapeXml="true"/><br>
    </c:if>
    <c:if test="${not empty userInfo.registrationDate}">
        <b>Дата регистрации:</b> <lor:date date="${userInfo.registrationDate}"/><br>
    </c:if>
    <c:if test="${userInfo.lastLogin != null}">
        <b>Последнее посещение:</b> <lor:date date="${userInfo.lastLogin}"/><br>
    </c:if>

    <b>Статус:</b> ${user.status}
    <c:if test="${user.moderator}"> (модератор)</c:if>
    <c:if test="${user.administrator}"> (администратор)</c:if>
    <c:if test="${user.corrector}"> (корректор)</c:if>
    <c:if test="${user.blocked}"> (заблокирован)</c:if>

    <c:if test="${isFrozen}">
        <br>
        <b>Заморожен</b>
            до <lor:date date="${user.frozenUntil}"/>
            модератором <lor:user link="true" user="${freezer}"/>
            по причине <c:out escapeXml="true" value="${user.freezingReason}"/>
        <br>
    </c:if>

    <br>
    <c:if test="${banInfo != null}">
        Блокирован <lor:date date="${banInfo.date}"/>,
            модератором <lor:user link="true" user="${banInfo.moderator}"/>
            по причине: <c:out escapeXml="true" value="${banInfo.reason}"/>
    </c:if>
</div>
<c:if test="${moderatorOrCurrentUser}">
  <div>
    <c:if test="${not empty user.email}">
      <b>Email:</b> <a
            href="mailto:${user.email}">${user.email}</a> (виден только вам и модераторам)
      <form action="/lostpwd.jsp" method="POST" style="display: inline">
        <lor:csrf/>
        <input type="hidden" name="email" value="${fn:escapeXml(user.email)}">
        <button type="submit" class="btn btn-small btn-default">Получить забытый пароль</button>
      </form>
    </c:if>

    <c:if test="${template.moderatorSession}">
        <a href="/people/${user.nick}/profile?reset-password" class="btn btn-small btn-danger">Сбросить пароль</a>
    </c:if>
    <br>

    <b>Score:</b> ${user.score}
    <c:if test="${template.moderatorSession and user.score<50}">
      <form action="/usermod.jsp" method="POST" style="display: inline">
        <lor:csrf/>
        <input type="hidden" name="id" value="${user.id}">
        <input type='hidden' name='action' value='score50'>
        <button type="submit" class="btn btn-small btn-default">Установить score=50</button>
      </form>
    </c:if>
    <br>

    <c:if test="${not currentUser && template.moderatorSession}">
      <b>Игнорируется:</b> ${userStat.ignoreCount}<br>
    </c:if>
  </div>
</c:if>

<c:if test="${fn:length(favoriteTags)>0}">
    <b>Избранные теги:</b>
    <c:forEach var="tagName" items="${favoriteTags}" varStatus="status">
        <spring:url value="/tag/{tag}" var="tagLink">
            <spring:param name="tag" value="${tagName}"/>
        </spring:url>
        <a class="tag" href="${tagLink}">${tagName}</a><c:if test="${not status.last}">, </c:if>
    </c:forEach>

    <c:if test="${currentUser}">
        &emsp;<a href="<c:url value="/user-filter"/>">изменить</a>
    </c:if>
    <br>
</c:if>
<c:if test="${moderatorOrCurrentUser && fn:length(ignoreTags)>0}">
    <b>Игнорированные теги:</b>
    <c:forEach var="tagName" items="${ignoreTags}" varStatus="status">
        <spring:url value="/tag/{tag}" var="tagLink">
            <spring:param name="tag" value="${tagName}"/>
        </spring:url>
        <a class="tag" href="${tagLink}">${tagName}</a><c:if test="${not status.last}">, </c:if>
    </c:forEach>
    <br>
</c:if>

<c:if test="${template.sessionAuthorized and !currentUser and not user.moderator}">
    <c:if test="${ignored}">
        <form name='i_unblock' method='post' action='<c:url value="/user-filter/ignore-user"/>'>
            <lor:csrf/>
            <input type='hidden' name='id' value='${user.id}'>
            Вы игнорируете этого пользователя &nbsp;
            <button type='submit' class="btn btn-small btn-default" name='del'>не игнорировать</button>
        </form>
    </c:if>

    <c:if test="${not ignored}">
        <form name='i_block' method='post' action='<c:url value="/user-filter/ignore-user"/>'>
            <lor:csrf/>
            <input type='hidden' name='nick' value='${user.nick}'>
            Вы не игнорируете этого пользователя &nbsp;
            <button type='submit' class="btn btn-small btn-default" name='add'>игнорировать</button>
        </form>
    </c:if>
</c:if>

<!-- ability to freeze a user temporary -->
<c:if test="${(template.moderatorSession and user.blockable) or template.currentUser.administrator}">
    <br />
    <div style="border: 1px dotted; padding: 1em;">
        <span>Заморозить, разморозить, изменить время заморозки.</span>
        <form method='post' action='usermod.jsp'>
            <lor:csrf/>
            <input type='hidden' name='id' value='${user.id}'>

            <!-- reason -->
            <div class="control-group">

                <label class="control-label" for="reason-input">Причина</label>
                <div class="controls">
                    <select name=reason_select
                        onChange="change(reason,reason_select);">

                        <option value=""></option>
                        <option value="3.1 Дубль">3.1 Дубль</option>
                        <option value="3.2 Неверная кодировка">
                            3.2 Неверная кодировка
                        </option>
                        <option value="3.3 Некорректное форматирование">
                            3.3 Некорректное форматирование
                        </option>
                        <option value="3.4 Пустое сообщение">
                            3.4 Пустое сообщение
                        </option>
                        <option value="4.1 Offtopic">
                            4.1 Offtopic
                        </option>
                        <option value="4.2 Вызывающе неверная информация">
                            4.2 Вызывающе неверная информация
                        </option>
                        <option value="4.3 Провокация flame">
                            4.3 Провокация flame
                        </option>
                        <option value="4.4 Обсуждение действий модераторов">
                            4.4 Обсуждение действий модераторов
                        </option>
                        <option value="4.5 Тестовые сообщения">
                            4.5 Тестовые сообщения
                        </option>
                        <option value="4.6 Спам">4.6 Спам</option>
                        <option value="4.7 Флуд">4.7 Флуд</option>
                        <option value="4.8 Дискуссия не на русском языке">
                            4.8 Дискуссия не на русском языке
                        </option>
                        <option value="5.1 Нецензурные выражения">
                            5.1 Нецензурные выражения
                        </option>
                        <option value="5.2 Оскорбление участников дискуссии">
                            5.2 Оскорбление участников дискуссии
                        </option>
                        <option value="5.3 Национальные/политические/религиозные споры">
                            5.3 Национальные/политические/религиозные споры
                        </option>
                        <option value="5.4 Личная переписка">
                            5.4 Личная переписка
                        </option>
                        <option value="5.5 Преднамеренное нарушение правил русского языка">
                            5.5 Преднамеренное нарушение правил русского языка
                        </option>
                        <option value="6 Нарушение copyright">
                            6 Нарушение copyright
                        </option>
                        <option value="6.2 Warez">
                            6.2 Warez
                        </option>
                        <option value="7.1 Ответ на некорректное сообщение">
                            7.1 Ответ на некорректное сообщение
                        </option>
                    </select>
                    <input id="reason-input" type="text" name="reason" required
                        placeholder="Остудись" value="" />
                </div>
            </div>

            <!-- until (duration) -->
            <div class="control-group">
                <label class="control-label" for="shift">
                    Период
                </label>

                <div class="controls">
                    <select name="shift">
                        <option value="-P1D">Разморозить</option>
                        <option value="PT5M">5 минут</option>
                        <option value="PT10M">10 минут</option>
                        <option value="PT15M">15 минут</option>
                        <option value="PT20M">20 минут</option>
                        <option value="PT30M">30 минут</option>
                        <option value="PT1H">час</option>
                        <option value="PT2H">2 часа</option>
                        <option value="PT3H">3 часа</option>
                        <option value="PT6H">6 часов</option>
                        <option value="PT9H">9 часов</option>
                        <option value="PT12H">12 часов</option>
                        <option value="P1D">сутки</option>
                        <option value="P2D">двое суток</option>
                        <option value="P3D">3 дня</option>
                        <option value="P5D">5 дней</option>
                        <option value="P7D">неделя</option>
                        <option value="P14D">две недели</option>
                        <option value="P30D">месяц</option>
                        <option value="P60D">2 месяца</option>
                        <option value="P90D">3 месяца</option>
                    </select>
                </div>
            </div>

            <div class="control-group">
              <div class="controls">
                <button type="submit" name="action" value="freeze">
                     Отправить
                </button>
              </div>
            </div>
        </form>
    </div>
</c:if>

<c:if test="${(template.moderatorSession and user.blockable) or template.currentUser.administrator}">
    <br>

    <div style="border: 1px dotted; padding: 1em;">
        <form method='post' action='usermod.jsp'>
            <lor:csrf/>
            <input type='hidden' name='id' value='${user.id}'>
            <c:if test="${user.blocked}">
                <button type='submit' name='action' value="unblock">разблокировать</button>
            </c:if>
            <c:if test="${not user.blocked}">
                <label>Причина: <input type="text" name="reason" size="40" required></label>
                <button type='submit' name='action' value="block">заблокировать</button><br>

                [<a href="/people/${user.nick}/profile?wipe">перейти к блокировке с удалением сообщений</a>]
            </c:if>
        </form>
    </div>
</c:if>
<p>
    <c:if test="${template.sessionAuthorized or user.maxScore>=50}">
    <div>
            ${userInfoText}
    </div>
    </c:if>

    <c:if test="${template.moderatorSession}">

<p>

<form name='f_remove_userinfo' method='post' action='usermod.jsp'>
    <lor:csrf/>
    <input type='hidden' name='id' value='${user.id}'>
    <input type='hidden' name='action' value='remove_userinfo'>
    <button type='submit'>Удалить текст</button>
</form>

<p>

    <c:if test="${user.corrector or user.score > user.correctorScore}">

<form name='f_toggle_corrector' method='post' action='usermod.jsp'>
    <lor:csrf/>
    <input type='hidden' name='id' value='${user.id}'>
    <input type='hidden' name='action' value='toggle_corrector'>
    <c:choose>
        <c:when test="${user.corrector}">
            <button type='submit'>Убрать права корректора</button>
        </c:when>
        <c:otherwise>
            <button type='submit'>Сделать корректором</button>
        </c:otherwise>
    </c:choose>
</form>
</c:if>
</c:if>

<c:if test="${currentUser}">
    <h2>Действия</h2>
    <ul>
        <li><a href="<c:url value="/user-filter"/>">Настройка фильтрации сообщений</a></li>
        <c:if test="${hasRemarks}">
            <li>
                <a href="/people/${user.nick}/remarks">Просмотр заметок о пользователях</a>
            </li>
        </c:if>
    </ul>
</c:if>

<h2>Статистика</h2>

<div id="cal-heatmap"></div>

<c:if test="${userStat.incomplete}">
  <div class="infoblock">
  Внимание! Статистику пользователя не удалось полностью загрузить. Попробуйте обновить страницу позже.
  </div>
</c:if>

<c:if test="${userStat.firstTopic != null}">
    <b>Первая созданная тема:</b> <lor:date date="${userStat.firstTopic}"/><br>
    <b>Последняя созданная тема:</b> <lor:date date="${userStat.lastTopic}"/><br>
</c:if>
<c:if test="${userStat.firstComment != null}">
    <b>Первый комментарий:</b> <lor:date date="${userStat.firstComment}"/><br>
    <b>Последний комментарий:</b> <lor:date date="${userStat.lastComment}"/><br>
</c:if>
<c:if test="${userStat.commentCount>0}">
    <b>Число комментариев:</b> ${userStat.commentCount}
</c:if>
<p>

    <c:if test="${user.id!=2}">
    <c:if test="${not empty userStat.topicsBySection}">

<div class="forum">
    <table class="message-table" style="width: auto">
        <thead>
        <tr>
            <th>Раздел</th>
            <th>Число тем</th>
        </tr>
        <tbody>
        <c:forEach items="${userStat.topicsBySection}" var="i">
        <tr>
            <td>${i.section.name}</td>
            <td><a href="/people/${user.nick}/?section=${i.section.id}">${i.count}</a></td>
        </tr>
        </c:forEach>
    </table>
</div>
</c:if>

<h2>Сообщения пользователя</h2>
<ul>
    <c:if test="${not empty userStat.topicsBySection}">
        <li>
            <a href="/people/${user.nick}/">Темы</a>
        </li>
    </c:if>

    <c:if test="${userStat.commentCount>0 || template.moderatorSession}">
        <li>
          <a href="search.jsp?range=COMMENTS&user=${user.nick}&sort=DATE">Комментарии</a>

          <c:if test="${template.moderatorSession}">
            (<a href="/people/${user.nick}/deleted-comments">удаленные</a>)
          </c:if>
        </li>
    </c:if>

    <c:if test="${moderatorOrCurrentUser}">
        <li>
            <a href="show-replies.jsp?nick=${user.nick}">Уведомления</a>
        </li>
        <c:if test="${watchPresent}">
            <li>
                <a href="/people/${user.nick}/tracked">Отслеживаемые темы</a>
            </li>
        </c:if>
    </c:if>
    <c:if test="${favPresent}">
        <li>
            <a href="/people/${user.nick}/favs">Избранные темы</a>
        </li>
    </c:if>
    <c:if test="${hasDrafts}">
        <li>
            <a href="/people/${user.nick}/drafts">Черновики</a>
        </li>
    </c:if>
</ul>
</c:if>

<c:if test="${not empty userlog}">
    <h2>Журнал действий</h2>

    <div class=forum>
        <table class="message-table" width="100%">
            <tbody>

            <c:forEach items="${userlog}" var="item">

            <tr>
                <td>
                    <strong>${item.item.action.description}</strong>
                    <c:if test="${not item.self}">
                        &emsp;<img src="/img/tuxlor.png"><lor:user user="${item.actionUser}"/>
                    </c:if><br>
                    <c:if test="${not empty item.item.options}">
                        <c:forEach items="${item.options}" var="option">
                            ${option.key}: ${option.value}<br>
                        </c:forEach>
                    </c:if>
                </td>

                <td>
                    <lor:dateinterval date="${item.item.actionDate.toDate()}"/>
                </td>
            </tr>
            </c:forEach>
        </table>
    </div>
</c:if>
</div>
<jsp:include page="footer.jsp"/>
