<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ page contentType="text/html; charset=utf-8"%>
<%@ taglib tagdir="/WEB-INF/tags" prefix="lor" %>
<%@ taglib prefix="l" uri="http://www.linux.org.ru" %>
<%--
  ~ Copyright 1998-2021 Linux.org.ru
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
<%--@elvariable id="blockInfo" type="ru.org.linux.auth.IPBlockInfo"--%>
<%--@elvariable id="blockModerator" type="ru.org.linux.user.User"--%>
<%--@elvariable id="topics" type="java.util.List<ru.org.linux.spring.SameIPController.TopicItem>"--%>
<%--@elvariable id="comments" type="java.util.List<ru.org.linux.spring.SameIPController.TopicItem>"--%>
<%--@elvariable id="users" type="java.util.List<ru.org.linux.spring.SameIPController.UserItem>"--%>
<%--@elvariable id="ip" type="java.lang.String"--%>
<%--@elvariable id="userAgent" type="java.lang.String"--%>
<jsp:include page="/WEB-INF/jsp/head.jsp"/>

<title>Поиск сообщений по метаданным</title>
<jsp:include page="/WEB-INF/jsp/header.jsp"/>
<div class=nav>
    <div id="navPath">
      Поиск сообщений по метаданным
    </div>

    <c:if test="${ip != null}">
      <div class="nav-buttons">
        [WHOIS
        <a href='http://whois.arin.net/ui/query.do?flushCache=false&q=${ip}&whoisSubmitButton=%20$'>ARIN</a> /
        <a href='http://lacnic.net/cgi-bin/lacnic/whois?lg=EN&query=${ip}'>LACNIC</a> /
        <a href='https://apps.db.ripe.net/search/query.html?searchtext=${ip}'>RIPE</a>
        ]
        [<a href='http://bgp.he.net/ip/${ip}'>BGP</a>]
    </div>
    </c:if>
</div>

<c:if test="${userAgent != null}">
  Показаны сообщения с User-Agent:<br>
  <c:out value="${userAgent}" escapeXml="true"/>
</c:if>

<c:if test="${ip != null}">
  <form action="sameip.jsp">
    <div class="control-group">
      Показаны сообщений с адреса <input class="input-lg" name="ip" type="search" size="20" maxlength="20" value="${ip}">&nbsp;
      <button type="submit" class="btn btn-default">Изменить</button><BR>
    </div>
  </form>

  <div class="infoblock">
    Местоположение (<a href="https://ipwhois.io" target="_blank">ipwhois.io</a>): <span id="geolookup">...</span>
  </div>

  <script>
      $script.ready("jquery", function () {
          $.ajax({
              method: 'GET',
              contentType: 'application/json',
              url: 'https://ipwhois.app/json/${ip}?lang=ru',
              dataType: 'json',
              success: function (json) {
                  if (json.success) {
                      $('#geolookup').text(json.country + " / " + json.region + " / " + json.city)
                  } else {
                      $('#geolookup').text("rejected - " + json.message)
                  }
              }
          });
      })
  </script>

<strong>Текущий статус: </strong>

<c:if test="${blockInfo == null}">
  адрес не заблокирован
</c:if>

<c:if test="${blockInfo != null}">
  <c:if test="${blockInfo.banDate == null}">
    адрес заблокирован постоянно
  </c:if>
  <c:if test="${blockInfo.banDate != null}">
    адрес заблокирован до <lor:date date="${blockInfo.banDate}"/>

    <c:if test="${not blockInfo.blocked}">
      (блокировка истекла)
    </c:if>
  </c:if>
  <br>
  <c:if test="${allowPosting}">
  <em>Зарегистрированным можно постить
  <c:if test="${captchaRequired}">
   с вводом каптчи
  </c:if>
  </em>
  <br />
  </c:if>
  <strong>Причина блокировки: </strong><c:out value="${blockInfo.reason}" escapeXml="true"/><br>
  <strong>Дата блокировки: </strong><lor:date date="${blockInfo.originalDate}"/><br>

  <strong>Адрес блокирован: </strong>
  <c:if test="${blockModerator != null}">
    ${blockModerator.nick}
  </c:if>
  <c:if test="${blockModerator == null}">
    автоматически
  </c:if>
</c:if>

<p>

<fieldset>
<legend>забанить/разбанить IP</legend>
<form method="post" action="banip.jsp">
<lor:csrf/>
<input type="hidden" name="ip" value="${ip}">
 по причине: <br>
<input type="text" name="reason" maxlength="254" size="40" value=""><br>
<select name="time" onchange="checkCustomBan(this.selectedIndex);">
<option value="hour">1 час</option>
<option value="day">1 день</option>
<option value="month">1 месяц</option>
<option value="3month">3 месяца</option>
<option value="6month">6 месяцев</option>
<option value="unlim">постоянно</option>
<option value="remove">не блокировать</option>
<option value="custom">указать (дней)</option>
</select>
<div id="custom_ban" style="display:none;">
<br><input type="text" name="ban_days" value="">
</div><br />

<c:choose>
 <c:when test="${allowPosting}">
  <c:set var="checked" value="checked=\"true\"" />
 </c:when>
 <c:otherwise>
  <c:set var="disabled" value="disabled=\"disabled\"" />
 </c:otherwise>
</c:choose>
<label><input id="allowPosting" type="checkbox" name="allow_posting" value="true" ${checked} onchange="allowPostingOnChange(this);">разрешить постить ранее зарегистрированным</label>
<c:if test="${captchaRequired}">
  <c:set var="checked2" value="checked=\"true\"" />
</c:if>
<label><input id="captchaRequired" type="checkbox" name="captcha_required" value="true" ${checked2} ${disabled}>требовать ввод каптчи</label>

<p>
<button type="submit" name="ban" class="btn btn-default">ban ip</button>
<script type="text/javascript">
function allowPostingOnChange(object) {

  var captchaRequired = $('#captchaRequired');
  if ($(object).is(':checked')) {
    captchaRequired.removeAttr("disabled");
  } else {
    captchaRequired.attr("disabled","disabled");
  }
}

function checkCustomBan(idx) {
  var custom_ban_div = document.getElementById('custom_ban');
  if (custom_ban_div==null || typeof(custom_ban_div)!="object") {
    return;
  }
  if (idx!=7) {
    custom_ban_div.style.display='none';
  } else {
    custom_ban_div.style.display='block';
  }
}
</script>
</form>
</fieldset>

<fieldset>
<legend>Удалить темы и сообщения с IP</legend>
<form method="post" action="delip.jsp">
<lor:csrf/>
<input type="hidden" name="ip" value="${ip}">
по причине: <br>
<input type="text" name="reason" maxlength="254" size="40" value=""><br>
за последний(ие) <select name="time" onchange="checkCustomDel(this.selectedIndex);">
<option value="hour">1 час</option>
<option value="day">1 день</option>
<option value="3day">3 дня</option>
</select>
<p>
<button type="submit" name="del" class="btn btn-danger">del from ip</button>
</form>
</fieldset>
</c:if>

<h2>Темы за 3 дня</h2>

<div class=forum>
<table width="100%" class="message-table">
<thead>
<tr><th>Раздел</th><th>Группа</th><th>Заглавие</th><th>Дата</th></tr>
<tbody>
<c:forEach items="${topics}" var="topic">
<tr>
  <td>
    ${topic.ptitle}
  </td>
  <td>
    ${topic.gtitle}
  </td>
  <td>
    <c:if test="${topic.deleted}">
      <s>
    </c:if>
    <a href="view-message.jsp?msgid=${topic.id}" rev=contents><l:title>${topic.title}</l:title></a>
    <c:if test="${topic.deleted}">
      </s>
    </c:if>
  </td>
  <td>
    <lor:date date="${topic.postdate}"/>
  </td>
</tr>
</c:forEach>
</table>
</div>

<h2>Комментарии за 3 дня</h2>

<div class=forum>
<table width="100%" class="message-table">
<thead>
<tr><th>Раздел</th><th>Группа</th><th>Заглавие темы</th><th>Дата</th></tr>
<tbody>
<c:forEach items="${comments}" var="topic">
<tr>
  <td>${topic.ptitle}</td>
  <td>${topic.gtitle}</td>
  <td>
    <c:if test="${topic.deleted}">
      <s>
    </c:if>
    <a href="jump-message.jsp?msgid=${topic.topicId}&amp;cid=${topic.id}" rev=contents><l:title>${topic.title}</l:title></a>
    <c:if test="${topic.deleted}">
      </s>
    </c:if>
  </td>
  <td>
    <lor:date date="${topic.postdate}"/>
  </td>
</tr>
</c:forEach>
</table>
</div>

<c:if test="${ip != null}">
<h2>Все пользователи, использовавшие данный IP</h2>
<div class=forum>
<table width="100%" class="message-table">
<thead>
<tr><th>Последний визит</th><th>Пользователь</th><th>User Agent</th></tr>
<tbody>
<c:forEach items="${users}" var="item">
<tr>
  <td>
      <lor:date date="${item.lastdate}"/>
  </td>
  <td>
    <a href="/people/${item.nick}/profile">${item.nick}</a>
  </td>
  <td>
    <c:if test="${item.sameUa}">
      <b><c:out escapeXml="true" value="${item.userAgent}"/></b>
    </c:if>
    <c:if test="${not item.sameUa}">
      <c:out escapeXml="true" value="${item.userAgent}"/>
    </c:if>
  </td>
</tr>
</c:forEach>
</table>
</div>
</c:if>
<jsp:include page="/WEB-INF/jsp/footer.jsp"/>
