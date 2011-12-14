<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ page contentType="text/html; charset=utf-8"%>
<%@ taglib tagdir="/WEB-INF/tags" prefix="lor" %>
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
<%--@elvariable id="blockInfo" type="ru.org.linux.admin.ipmanage.IPBlockInfo"--%>
<%--@elvariable id="blockModerator" type="ru.org.linux.user.User"--%>
<%--@elvariable id="topics" type="java.util.List<ru.org.linux.admin.ipmanage.SameIPController.TopicItem>"--%>
<%--@elvariable id="comments" type="java.util.List<ru.org.linux.admin.ipmanage.SameIPController.TopicItem>"--%>
<%--@elvariable id="users" type="java.util.List<ru.org.linux.admin.ipmanage.SameIPController.UserItem>"--%>
<%--@elvariable id="ip" type="java.lang.String"--%>
<%--@elvariable id="tor" type="java.lang.Boolean"--%>
<jsp:include page="/WEB-INF/jsp/head.jsp"/>

<title>Поиск писем с IP-адреса</title>
<jsp:include page="/WEB-INF/jsp/header.jsp"/>
<%
  String ip = (String) request.getAttribute("ip");
%>
<table class=nav>
  <tr>
    <td align=left valign=middle id="navPath">
      <strong>Интерфейс модератора - Сообщения с ${ip}</strong>
    </td>

    <td align=right valign=middle>

      [<a href="http://www.radio-msu.net/serv/wwwnslookup/nph-wwwtr.cgi?server=${ip}">NSLOOKUP</a>]
      [WHOIS
      <a href='http://whois.arin.net/ui/query.do?flushCache=false&q=${ip}&whoisSubmitButton=%20$'>ARIN</a> /
      <a href='http://www.apnic.net/apnic-bin/whois.pl?search=${ip}'>APNIC</a> /
      <a href='http://lacnic.net/cgi-bin/lacnic/whois?lg=EN&query=${ip}'>LACNIC</a>
      ]
    </td>
  </tr>

</table>

<h1 class="optional">Сообщения с <%= ip %> (за 3 дня)</h1>

<strong>Текущий статус: </strong>

<c:if test="${tor}">
  адрес заблокирован: tor.ahbl.org; база:
</c:if>

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
  <strong>Причина блокировки: </strong><c:out value="${blockInfo.reason}" escapeXml="true"/><br>
  <strong>Дата блокировки: </strong><lor:date date="${blockInfo.originalDate}"/><br>
  <strong>Адрес блокирован: </strong>${blockModerator.nick}
</c:if>

<p>

<form method="post" action="banip.jsp">
<input type="hidden" name="ip" value="<%= ip %>">
забанить/разбанить IP по причине: <br>
<input type="text" name="reason" maxlength="254" size="40" value=""><br>
<select name="time" onchange="checkCustomBan(this);">
<c:forEach var="banPeriod" items="${banPeriods}">
<option value="${banPeriod.key}">${banPeriod.value}</option>
</c:forEach>
</select>
<div id="custom_ban" style="display:none;">
<br><input type="text" name="ban_days" value="">
</div>
<p>
<input type="submit" name="ban" value="ban ip">
<script type="text/javascript">
<!--
function checkCustomBan(selectObject) {
  var custom_ban_div = document.getElementById('custom_ban');
  if (custom_ban_div==null || typeof(custom_ban_div)!="object") {
    return;
  }
  if ($("option:selected", selectObject).val() != "${customPeriodName}") {
    custom_ban_div.style.display='none';
  } else {
    custom_ban_div.style.display='block';
  }
}
// -->
</script>
</form>

<form method="post" action="delip.jsp">
<input type="hidden" name="ip" value="<%= ip %>">
Удалить темы и сообщения с IP по причине: <br>
<input type="text" name="reason" maxlength="254" size="40" value=""><br>
за последний(ие) <select name="time" onchange="checkCustomDel(this.selectedIndex);">
<option value="hour">1 час</option>
<option value="day">1 день</option>
<option value="3day">3 дня</option>
</select>
<p>
<input type="submit" name="del" value="del from ip">
</form>

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
    <a href="view-message.jsp?msgid=${topic.id}" rev=contents>${topic.title}</a>
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

<h2>Комментарии за 24 часа</h2>

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
    <a href="jump-message.jsp?msgid=${topic.topicId}&amp;cid=${topic.id}" rev=contents>${topic.title}</a>
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
      <b>${item.userAgent}</b>
    </c:if>
    <c:if test="${not item.sameUa}">
      ${item.userAgent}
    </c:if>
  </td>
</tr>
</c:forEach>
</table>
</div>
<jsp:include page="/WEB-INF/jsp/footer.jsp"/>
