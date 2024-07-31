<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ page contentType="text/html; charset=utf-8"%>
<%@ taglib tagdir="/WEB-INF/tags" prefix="lor" %>
<%@ taglib prefix="l" uri="http://www.linux.org.ru" %>
<%--
  ~ Copyright 1998-2024 Linux.org.ru
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
<%--@elvariable id="comments" type="java.util.List<ru.org.linux.comment.PreparedCommentsListItem>"--%>
<%--@elvariable id="users" type="java.util.List<ru.org.linux.spring.SameIPController.UserItem>"--%>
<%--@elvariable id="ip" type="java.lang.String"--%>
<%--@elvariable id="userAgent" type="java.lang.String"--%>
<jsp:include page="/WEB-INF/jsp/head.jsp"/>

<title>Поиск сообщений по метаданным</title>
<jsp:include page="/WEB-INF/jsp/header.jsp"/>

<h1>Поиск сообщений по метаданным</h1>

<c:if test="${userAgent != null}">
  Показаны сообщения с User-Agent:<br>
  <c:out value="${userAgent}" escapeXml="true"/>
</c:if>

<c:if test="${ip != null}">
  <form action="sameip.jsp">
    <c:if test="${ua != null}">
      <input type="hidden" name="ua" value="${ua}">
    </c:if>
    <div class="control-group">
      <div class="controls">
        <input class="input-lg" name="ip" type="search" size="17" maxlength="17" value="${ip}" id="ip-field" pattern="[0-9]+\.[0-9]+\.[0-9]+\.[0-9]+">

        <c:forEach items="${masks}" var="v">
          <c:if test="${v._1() == mask}">
            <button name="mask" value="${v._1()}" type="submit" class="btn btn-selected">${v._2()}</button>
          </c:if>
          <c:if test="${v._1() != mask}">
            <button name="mask" value="${v._1()}" type="submit" class="btn btn-default">${v._2()}</button>
          </c:if>
        </c:forEach>

        <select name="score" class="btn btn-default" onchange="this.form.submit()">
          <c:forEach items="${scores}" var="v">
            <c:if test="${v._1() == score}">
              <option value="${v._1()}" type="submit" selected>${v._2()}</option>
            </c:if>
            <c:if test="${v._1() != score}">
              <option value="${v._1()}" type="submit">${v._2()}</option>
            </c:if>
          </c:forEach>
        </select>
      </div>
    </div>
  </form>

  <c:if test="${!hasMask}">
    <div>
      <strong>Текущий статус: </strong>

      <c:if test="${blockInfo == null}">
        адрес не заблокирован
      </c:if>

      <c:if test="${blockInfo != null}">
        адрес заблокирован

        <c:if test="${blockModerator == null}">
          автоматически
        </c:if>

        <c:if test="${blockModerator != null}">
          модератором <lor:user user="${blockModerator}"/>
        </c:if>

        <c:out value=" "/> <lor:date date="${blockInfo.originalDate}"/>

        <c:if test="${blockInfo.banDate != null}">
          до <lor:date date="${blockInfo.banDate}"/>
          <c:if test="${not blockInfo.blocked}">
            (блокировка истекла)
          </c:if>
        </c:if>

        <c:if test="${blockInfo.banDate == null}">
          постоянно
        </c:if>

        <br>
        <c:if test="${allowPosting}">
          Зарегистрированным можно постить
            <c:if test="${captchaRequired}">
              с вводом каптчи
            </c:if>
          <br>
        </c:if>
        <strong>Причина блокировки: </strong><c:out value="${blockInfo.reason}" escapeXml="true"/><br>
      </c:if>
    </div>
  </c:if>

  <div>
    <strong>Местоположение ${ip} (<a href="https://ipwhois.io" target="_blank">ipwhois.io</a>)</strong>: <span id="geolookup">...</span>
  </div>

  <script>
      $script.ready("jquery", function () {
          $.ajax({
              method: 'GET',
              contentType: 'application/json',
              url: 'https://ipwhois.app/json/${ip}',
              dataType: 'json',
              success: function (json) {
                  if (json.success) {
                      $('#geolookup').text(json.country + " / " + json.region + " / " + json.city + " (" + json.org + ")")
                  } else {
                      $('#geolookup').text("rejected - " + json.message)
                  }
              }
          });
      })
  </script>

</c:if>

<c:if test="${not empty newUsers}">
  <h2>Новые пользователи за 3 дня</h2>
  <div class=forum>
    <table width="100%" class="message-table">
      <thead>
      <tr><th>Nick</th><th>Дата регистрации</th><th>Последнее посещение</th></tr>
      <tbody>
      <c:forEach items="${newUsers}" var="user">
      <tr>
        <td>
            <lor:user user="${user._1()}" link="true"/>
        </td>
        <td>
          <lor:date date="${user._2()}"/>
        </td>
        <td>
          <c:if test="${not user._1().activated}">
            не активирован
          </c:if>
          <c:if test="${user._1().activated}">
            <lor:dateinterval date="${user._3()}"/>
          </c:if>
        </td>
      </tr>
      </c:forEach>
    </table>
  </div>

</c:if>

<h2>Сообщения за 5 дней
  <c:if test="${hasMoreComments}">(показаны первые ${rowsLimit})</c:if>
</h2>

<div class=comments>
<c:forEach items="${comments}" var="comment">
<a href="${comment.link}" class="comments-item">
  <div class="comments-group"><p>
    <span class="group-label">${comment.groupTitle}</span><br class="hideon-phone hideon-tablet">
    <lor:user user="${comment.author}"/>
  </p>
  </div>
  <div class="comments-title">
    <div class="text-preview-box">
      <div class="text-preview">
        <c:if test="${comment.comment}"><i class="icon-comment"></i></c:if>
        <l:title>${comment.title}</l:title>
      </div>
    </div>
  </div>
  <div class="comments-text">
    <div class="text-preview-box">
      <div class="text-preview">
        <c:if test="${comment.deleted}">
        <s>
          </c:if>
          <c:out value="${comment.textPreview}"/>
          <c:if test="${comment.deleted}">
        </s>
        </c:if>
      </div>
    </div>
    <c:if test="${comment.deleted}">
      <img src="/img/del.png" alt="[X]" width="15" height="15">
      Удалено по причине: <c:out escapeXml="true" value="${comment.reason}"/>
    </c:if>
  </div>
  <div class="comments-date">
    <p>
      <lor:dateinterval date="${comment.postdate}" compact="true"/>
    </p>
  </div>
</a>
</c:forEach>
</div>

<h2>Пользователи за год (по топикам и комментариям)
  <c:if test="${hasMoreUsers}">(показаны первые ${rowsLimit})</c:if>
</h2>
<div class=forum>
<table width="100%" class="message-table">
<thead>
<tr><th>Последний комментарий</th><th>Пользователь</th><th>User Agent</th></tr>
<tbody>
<c:forEach items="${users}" var="item">
<tr>
  <td>
      <lor:date date="${item.lastdate}"/>
  </td>
  <td>
    <c:if test="${item.blocked}">
      <s>
    </c:if>
    <a href="/people/${item.nick}/profile">${item.nick}</a>
    <c:if test="${item.blocked}">
      </s>
    </c:if>
  </td>
  <td>
    <c:out escapeXml="true" value="${item.userAgent}"/>
  </td>
</tr>
</c:forEach>
</table>
</div>

<c:if test="${ip != null}">

<c:if test="${!hasMask}">
  <h2>Управление</h2>

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
      <option value="5day">5 дней</option>
    </select>
      <p>
        <button type="submit" name="del" class="btn btn-danger">del from ip</button>
    </form>
  </fieldset>
</c:if>
</c:if>
<jsp:include page="/WEB-INF/jsp/footer.jsp"/>
