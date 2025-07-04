<%@ page contentType="text/html; charset=utf-8"%>
<%@ page import="ru.org.linux.site.Template"%>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib prefix="lor" uri="http://www.linux.org.ru" %>
<%@ taglib tagdir="/WEB-INF/tags" prefix="lorDir" %>
<%@ taglib prefix="fn" uri="http://java.sun.com/jsp/jstl/functions" %>
<%--
  ~ Copyright 1998-2025 Linux.org.ru
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
<%--@elvariable id="template" type="ru.org.linux.site.Template"--%>
<%--@elvariable id="news" type="java.util.List<ru.org.linux.topic.PersonalizedPreparedTopic>"--%>
<%--@elvariable id="uncommited" type="java.lang.Integer"--%>
<%--@elvariable id="hasDrafts" type="java.lang.Boolean"--%>
<%--@elvariable id="currentUser" type="ru.org.linux.user.User"--%>
<%--@elvariable id="briefNews" type="java.util.List<java.util.List<scala.Tuple2<java.lang.String, java.util.Collection<ru.org.linux.topic.BriefTopicRef>>>>"--%>
<jsp:include page="/WEB-INF/jsp/head.jsp"/>

<title>LINUX.ORG.RU — Русская информация об ОС Linux</title>
<meta name="Keywords" content="linux линукс операционная система документация gnu бесплатное свободное програмное обеспечение софт unix юникс software free documentation operating system новости news">
<meta name="Description" content="Все о Linux на русском языке">
<link rel="alternate" title="L.O.R RSS" href="section-rss.jsp?section=1" type="application/rss+xml">
<jsp:include page="/WEB-INF/jsp/header-main.jsp"/>

<div id="mainpage">
<div id="news">
<%--

<c:if test="${showAdsense}">
  <div align="center" width="100%">
  <style>
    @media screen and (max-width: 480px) {
      .yandex-adaptive {
        min-height: 90px;
        width: 100%;
        max-height: 250px;
      }
    }

    @media screen and (min-width: 481px) {
      .yandex-adaptive {
          min-height: 150px;
          height: 150px;
          width: 100%;
          max-height: 150px
      }
    }
  </style>

  <!-- Yandex.RTB R-A-337082-3 -->
  <div id="yandex_rtb_R-A-337082-3" class="yandex-adaptive"></div>
  <script type="text/javascript">
      (function(w, d, n, s, t) {
          w[n] = w[n] || [];
          w[n].push(function() {
              Ya.Context.AdvManager.render({
                  blockId: "R-A-337082-3",
                  renderTo: "yandex_rtb_R-A-337082-3",
                  async: true
              });
          });
          t = d.getElementsByTagName("script")[0];
          s = d.createElement("script");
          s.type = "text/javascript";
          s.src = "//an.yandex.ru/system/context.js";
          s.async = true;
          t.parentNode.insertBefore(s, t);
      })(this, this.document, "yandexContextAsyncCallbacks");
  </script>
  </div>
</c:if>
--%>

  <div style="text-align: center; margin-top: 0.5em; height: 125px" id="interpage">
  </div>
  <script type="text/javascript">
    $script.ready('lorjs', function () {
      var ads = [
        {
          type: 'rimg',
          img320: '/adv/SelectOS_320-100-02.jpg',
          img730: '/adv/SelectOS_730-90-02.jpg',
          img980: '/adv/SelectOS_980-120-02.jpg',
          href: 'https://slc.tl/3igzf'
        },
        {
          type: 'rimg',
          img320: '/adv/MD-4752_320x100.jpg',
          img730: '/adv/MD-4752_730x90.jpg',
          img980: '/adv/MD-4752_980x120.jpg',
          href: 'https://selectel.ru/services/dedicated/?tab=configuratorGpu&c=109%3A1&simpleRamMode=true&utm_source=linux.org&utm_medium=cpc&utm_campaign=gpu_one'
        }
      ];

      init_interpage_adv(ads);
    });
  </script>

<c:if test="${(template.moderatorSession or template.correctorSession) and (uncommited > 0)}">
<nav>
    <c:if test="${uncommitedCounts.size() > 1}">
      <a class="btn btn-default" href="view-all.jsp">Неподтверждённых: ${uncommited}</a>
    </c:if>

    <c:forEach var="item" items="${uncommitedCounts}">
      <a class="btn btn-default" href="view-all.jsp?section=${item._1().id}">Неподтверждённые ${item._1().name.toLowerCase()}: ${item._2()}</a>
    </c:forEach>
</nav>
</c:if>

<c:forEach var="msg" items="${news}">
  <lorDir:news preparedMessage="${msg.preparedTopic}" messageMenu="${msg.topicMenu}"
               multiPortal="${template.prof.showGalleryOnMain}" moderateMode="false"/>
</c:forEach>

<c:if test="${not empty briefNews}">
<section>
   <h2>Еще новости</h2>

  <div class="container" id="main-page-news">
    <c:forEach var="map" items="${briefNews}" varStatus="iter">
      <section>
        <c:forEach var="entry" items="${map}">
          <h3>${entry._1()}</h3>
          <ul>
            <c:forEach var="msg" items="${entry._2()}">
              <li>
                <c:if test="${msg.group.defined}">
                  <span class="group-label">${msg.group.get()}</span>
                </c:if>
                <a href="${msg.url}"><l:title>${msg.title}</l:title></a>
                <c:if test="${msg.commentCount>0}">(<lorDir:comment-count count="${msg.commentCount}"/>)</c:if>
              </li>
            </c:forEach>
          </ul>
        </c:forEach>
      </section>
    </c:forEach>
  </div>
</section>
</c:if>

<nav>
  <c:if test="${not empty addNews}">
    <a href="${addNews}" class="btn btn-primary">Добавить новость</a>
  </c:if>
  <a href="/news/" class="btn btn-default">Все новости</a>
  <a class="btn btn-default" href="/view-all.jsp?section=1">Неподтверждённые новости</a>
</nav>

  <p>
    <i class="icon-rss"></i>
    <a href="section-rss.jsp?section=1">
      RSS-подписка на новости
    </a>
  </p>

  <p>
    <i class="icon-telegram"></i>
    <a href="https://t.me/best_of_lor">
      Канал в Telegram
    </a>
  </p>
</div>

<aside id=boxlets>

  <c:if test="${template.sessionAuthorized}">
    <div class=boxlet>
      <h2>Добро пожаловать!</h2>

      <div class="boxlet_content">
        Ваш статус: ${currentUser.status}
        <ul>
          <li><a href="/people/${currentUser.nick}/">Мои темы</a></li>
          <c:if test="${favPresent}">
              <li><a href="/people/${currentUser.nick}/favs">Избранные темы</a></li>
          </c:if>
          <li><a href="search.jsp?range=COMMENTS&user=${currentUser.nick}&sort=DATE">Мои комментарии</a></li>
          <li><a href="/people/${currentUser.nick}/reactions">Мои реакции</a></li>
          <c:if test="${hasDrafts}">
              <li>
                  <a href="/people/${currentUser.nick}/drafts">Черновики</a>
              </li>
          </c:if>
        </ul>
        <ul>
          <li><a href="/people/${currentUser.nick}/settings">Настройки</a></li>
          <li><a href="/people/${currentUser.nick}/edit">Редактировать профиль</a></li>
        </ul>
      </div>
    </div>
  </c:if>

  <% out.flush(); %>

  <div align="center" class="boxlet">
    <div class="boxlet_content" style="padding-top: 1em">
      <a href="http://qrator.net/" rel="nofollow" target="_blank">
        <img src="/adv/qrator-box.png" width="250" style="max-width: 100%; height: auto" alt="Protected by Qrator">
      </a>
    </div>
  </div>

  <c:forEach var="boxlet" items="${template.prof.boxlets}">
    <div class="boxlet">
        <c:import url="/${boxlet}.boxlet"/>
    </div>
  </c:forEach>
</aside>
</div>

<jsp:include page="/WEB-INF/jsp/footer-main.jsp"/>
