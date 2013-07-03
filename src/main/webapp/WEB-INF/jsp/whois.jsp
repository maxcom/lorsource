<%@ page contentType="text/html; charset=utf-8" %>
<%@ page buffer="60kb" %>
<%@ taglib tagdir="/WEB-INF/tags" prefix="lor" %>
<%@ taglib prefix="l" uri="http://www.linux.org.ru" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="fn" uri="http://java.sun.com/jsp/jstl/functions" %>
<%@ taglib prefix="spring" uri="http://www.springframework.org/tags" %>
<%--
  ~ Copyright 1998-2012 Linux.org.ru
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
<%--@elvariable id="userStat" type="ru.org.linux.user.UserStatistics"--%>
<%--@elvariable id="template" type="ru.org.linux.site.Template"--%>
<%--@elvariable id="currentUser" type="java.lang.Boolean"--%>
<%--@elvariable id="ignored" type="java.lang.Boolean"--%>
<%--@elvariable id="moderatorOrCurrentUser" type="java.lang.Boolean"--%>
<%--@elvariable id="banInfo" type="ru.org.linux.user.BanInfo"--%>
<%--@elvariable id="remark" type="ru.org.linux.user.Remark"--%>
<%--@elvariable id="hasRemarks" type="java.lang.Boolean"--%>
<%--@elvariable id="sectionStat" type="java.util.List<ru.org.linux.user.PreparedUsersSectionStatEntry>"--%>
<%--@elvariable id="userlog" type="java.util.List<ru.org.linux.user.PreparedUserLogItem>"--%>

<jsp:include page="/WEB-INF/jsp/head.jsp"/>

<title>Информация о пользователе ${user.nick}</title>
<c:if test="${userInfo.url != null}">
    <c:if test="${user.score >= 100 && not user.blocked && user.activated}">
        <link rel="me" href="${fn:escapeXml(userInfo.url)}">
    </c:if>
</c:if>
<LINK REL="alternate" HREF="/people/${user.nick}/?output=rss" TYPE="application/rss+xml">

<jsp:include page="header.jsp"/>

<h1>Информация о пользователе ${user.nick}</h1>

<div id="whois_userpic">
    <l:userpic userpic="${userpic}"/>
    <c:if test="${moderatorOrCurrentUser}">
        <span>
        <c:if test="${user.photo != null}">
            <form name='f_remove_userpic' method='post' action='remove-userpic.jsp'>
                <lor:csrf/>
                <input type='hidden' name='id' value='${user.id}'>
                <button type="submit" class="delete">Удалить</button>
            </form>
        </c:if>
        <c:if test="${currentUser}">
            <form method="get" action="addphoto.jsp">
                <button type="submit">Изменить</button>
            </form>
        </c:if>
        </span>
    </c:if>
</div>
<div>
<h2>Регистрация</h2>

<div class="vcard">
    <b>ID:</b> ${user.id}<br>
    <b>Nick:</b> <span class="nickname">${user.nick}</span>
    <c:if test="${template.sessionAuthorized and !currentUser}">
        <br><b>Комментарий:</b> <c:out value="${remark.text}" escapeXml="true"/></i>
        [<a href="/people/${user.nick}/remark/">Изменить</a>]
    </c:if>
    <br>
    <c:if test="${not empty user.name}">
        <b>Полное имя:</b> <span class="fn">${user.name}</span><br>
    </c:if>

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

    <br>
    <c:if test="${banInfo != null}">
        Блокирован <lor:date date="${banInfo.date}"/>, модератором <lor:user link="true"
                                                                             user="${banInfo.moderator}"/> по причине:
        <c:out escapeXml="true" value="${banInfo.reason}"/>
    </c:if>
</div>
<c:if test="${moderatorOrCurrentUser}">
    <div>
        <c:if test="${not empty user.email}">
            <b>Email:</b> <a href="mailto:${user.email}">${user.email}</a> (виден только вам и модераторам)
            <form action="/lostpwd.jsp" method="POST" style="display: inline">
                <lor:csrf/>
                <input type="hidden" name="email" value="${fn:escapeXml(user.email)}">
                <input type="submit" value="Получить забытый пароль">
            </form>
        </c:if>

        <c:if test="${template.moderatorSession}">
            <form action="/usermod.jsp" method="POST" style="display: inline">
                <lor:csrf/>
                <input type="hidden" name="id" value="${user.id}">
                <input type='hidden' name='action' value='reset-password'>
                <input type="submit" value="Сбросить пароль">
            </form>
        </c:if>
        <br>
        <b>Score:</b> ${user.score}<br>
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
    <br>
</c:if>
<c:if test="${moderatorOrCurrentUser && fn:length(ignoreTags)>0}">
    <b>Игнорированные теги:</b>
    <c:forEach var="tagName" items="${ignoreTags}">
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
            <input type='submit' name='del' value='не игнорировать'>
        </form>
    </c:if>

    <c:if test="${not ignored}">
        <form name='i_block' method='post' action='<c:url value="/user-filter/ignore-user"/>'>
            <lor:csrf/>
            <input type='hidden' name='nick' value='${user.nick}'>
            Вы не игнорируете этого пользователя &nbsp;
            <input type='submit' name='add' value='игнорировать'>
        </form>
    </c:if>
</c:if>

<c:if test="${(template.moderatorSession and user.blockable) or template.currentUser.administrator}">
    <br>

    <div style="border: 1px dotted; padding: 1em;">
        <form method='post' action='usermod.jsp'>
            <lor:csrf/>
            <input type='hidden' name='id' value='${user.id}'>
            <c:if test="${user.blocked}">
                <input type='submit' name='action' value='unblock'>
            </c:if>
            <c:if test="${not user.blocked}">
                <label>Причина: <input type="text" name="reason" size="40" required></label>
                <input type='submit' name='action' value='block'><br>

                [<a href="/people/${user.nick}/profile?wipe">перейти к блокировке с удалением сообщений</a>]
            </c:if>
        </form>
    </div>
</c:if>
<p>
    <c:if test="${template.sessionAuthorized or user.maxScore>=50}">
    <cite>
            ${userInfoText}
    </cite>
    </c:if>

    <c:if test="${template.moderatorSession}">

<p>

<form name='f_remove_userinfo' method='post' action='usermod.jsp'>
    <lor:csrf/>
    <input type='hidden' name='id' value='${user.id}'>
    <input type='hidden' name='action' value='remove_userinfo'>
    <input type='submit' value='Удалить текст'>
</form>

<p>

    <c:if test="${user.corrector or user.score > user.correctorScore}">

<form name='f_toggle_corrector' method='post' action='usermod.jsp'>
    <lor:csrf/>
    <input type='hidden' name='id' value='${user.id}'>
    <input type='hidden' name='action' value='toggle_corrector'>
    <c:choose>
        <c:when test="${user.corrector}">
            <input type='submit' value='Убрать права корректора'>
        </c:when>
        <c:otherwise>
            <input type='submit' value='Сделать корректором'>
        </c:otherwise>
    </c:choose>
</form>
</c:if>
</c:if>

<c:if test="${currentUser}">
    <h2>Действия</h2>
    <ul>
        <li><a href="/people/${user.nick}/edit">Изменить регистрацию</a></li>
        <li><a href="/people/${user.nick}/settings">Изменить настройки</a></li>
        <li><a href="<c:url value="/user-filter"/>">Настройка фильтрации сообщений</a></li>
        <c:if test="${hasRemarks}">
            <li>
                <a href="/people/${user.nick}/remarks">Просмотр заметок о пользователях</a>
            </li>
        </c:if>
    </ul>
</c:if>

<h2>Статистика</h2>
<c:if test="${userStat.firstTopic != null}">
    <b>Первая созданная тема:</b> <lor:date date="${userStat.firstTopic}"/><br>
    <b>Последняя созданная тема:</b> <lor:date date="${userStat.lastTopic}"/><br>
</c:if>
<c:if test="${userStat.firstComment != null}">
    <b>Первый комментарий:</b> <lor:date date="${userStat.firstComment}"/><br>
    <b>Последний комментарий:</b> <lor:date date="${userStat.lastComment}"/><br>
</c:if>
<c:if test="${not user.anonymous}">
    <b>Число комментариев:</b> <c:if
        test="${not userStat.exactCommentCount}">приблизительно </c:if> ${userStat.commentCount}
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
        <c:forEach items="${sectionStat}" var="i">
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

    <c:if test="${userStat.commentCount>0}">
        <li>
            <a href="show-comments.jsp?nick=${user.nick}">Комментарии</a>
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
