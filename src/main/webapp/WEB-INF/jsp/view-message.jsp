<%@ page contentType="text/html; charset=utf-8"%>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/functions" prefix="fn" %>
<%@ taglib prefix="l" uri="http://www.linux.org.ru" %>
<%@ taglib tagdir="/WEB-INF/tags" prefix="lor" %>
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
<%--@elvariable id="showAdsense" type="Boolean"--%>
<%--@elvariable id="message" type="ru.org.linux.topic.Topic"--%>
<%--@elvariable id="preparedMessage" type="ru.org.linux.topic.PreparedTopic"--%>
<%--@elvariable id="messageMenu" type="ru.org.linux.topic.TopicMenu"--%>
<%--@elvariable id="memoriesInfo" type="ru.org.linux.user.MemoriesInfo"--%>
<%--@elvariable id="prevMessage" type="ru.org.linux.topic.Topic"--%>
<%--@elvariable id="nextMessage" type="ru.org.linux.topic.Topic"--%>
<%--@elvariable id="template" type="ru.org.linux.site.Template"--%>
<%--@elvariable id="showDeleted" type="Boolean"--%>
<%--@elvariable id="group" type="ru.org.linux.group.Group"--%>
<%--@elvariable id="commentsPrepared" type="java.util.List<ru.org.linux.comment.PreparedComment>"--%>
<%--@elvariable id="page" type="Integer"--%>
<%--@elvariable id="pages" type="ru.org.linux.paginator.PagesInfo"--%>
<%--@elvariable id="unfilteredCount" type="java.lang.Integer"--%>
<%--@elvariable id="moreLikeThisGetter" type="java.util.concurrent.Callable<java.util.List<java.util.List<ru.org.linux.search.MoreLikeThisTopic>>>"--%>
<%--@elvariable id="ogDescription" type="java.lang.String"--%>
<%--@elvariable id="editInfo" type="ru.org.linux.topic.PreparedEditInfoSummary"--%>

<jsp:include page="/WEB-INF/jsp/head.jsp"/>

<title><l:title>${message.title}</l:title> — ${preparedMessage.group.title} — ${preparedMessage.section.title}</title>
<meta property="og:title" content="<l:title>${message.title}</l:title>" >

<c:if test="${preparedMessage.section.imagepost}">
  <meta property="og:image" content="${preparedMessage.image.mediumName}">
  <meta name="twitter:card" content="summary_large_image">
</c:if>
<c:if test="${not preparedMessage.section.imagepost}">
  <meta property="og:image" content="${template.secureMainUrlNoSlash}/img/good-penguin.png">
  <meta name="twitter:card" content="summary">
</c:if>
<meta name="twitter:site" content="@wwwlinuxorgru">
<c:if test="${not empty ogDescription}">
  <meta property="og:description" content="${ogDescription}">
</c:if>

<meta property="og:url" content="${template.secureMainUrlNoSlash}${message.link}">

<link rel="canonical" href="${template.secureMainUrlNoSlash}${message.getLinkPage(page)}">

<c:if test="${prevMessage != null}">
  <link rel="Previous" id="PrevLink" href="${fn:escapeXml(prevMessage.link)}" title="<l:title><l:mkTitle>${prevMessage.title}</l:mkTitle></l:title>">
</c:if>

<c:if test="${nextMessage != null}">
  <link rel="Next" id="NextLink" href="${fn:escapeXml(nextMessage.link)}" title="<l:title><l:mkTitle>${nextMessage.title}</l:mkTitle></l:title>">
</c:if>

<c:if test="${not message.expired}">
  <link rel="alternate" title="Comments RSS" href="${message.link}?output=rss" type="application/rss+xml">
</c:if>

<script type="text/javascript">
  $script.ready('lorjs', function() { initNextPrevKeys(); });
  <c:if test="${not message.expired and template.sessionAuthorized}">
    $script('/js/addComments.js');
  </c:if>

  <c:if test="${not message.expired and not pages.hasNext}">
    $script('/js/realtime.js', "realtime");
    $script.ready('realtime', function() {
        startRealtimeWS(${message.id}, "${message.link}", ${lastCommentId}, "${template.WSUrl}");
    });
  </c:if>

</script>
<jsp:include page="/WEB-INF/jsp/header.jsp"/>

<div class=messages itemscope itemtype="http://schema.org/Article">

<c:set var="scroller"><c:if test="${topScroller}">
  <div class="nav">
  <div class="grid-row">
    <div class="grid-3-1">
      <table>
        <tr valign=middle>
          <c:if test="${prevMessage != null}">
            <td style="padding-right: 5px">
              <a href="${fn:escapeXml(prevMessage.link)}" rel=prev>←</a>
            </td>
            <td align=left valign=top class="hideon-phone">
              <a href="${fn:escapeXml(prevMessage.link)}" rel=prev>
                <l:title><l:mkTitle>${prevMessage.title}</l:mkTitle></l:title>
              </a>
            </td>
          </c:if>
        </tr>
      </table>
    </div>
    <div class="grid-3-2">
    </div>
    <div class="grid-3-3">
      <c:if test="${nextMessage != null}">
        <table align="right">
          <tr valign=middle align=right>
            <td class="hideon-phone">
              <a href="${fn:escapeXml(nextMessage.link)}" rel=next>
                <l:title><l:mkTitle>${nextMessage.title}</l:mkTitle></l:title>
              </a>
            </td>
            <td align="right" valign="middle" style="padding-left: 5px">
              <a href="${fn:escapeXml(nextMessage.link)}" rel=next>→</a>
            </td>
          </tr>
        </table>
      </c:if>
    </div>
  </div>
  </div>
</c:if></c:set>

<c:set var="bottomScroller"><c:if test="${bottomScroller}">
  <div class="nav">
  <div class="grid-row">
    <div class="grid-3-1">
      <table>
        <tr valign=middle>
          <c:if test="${prevMessage != null}">
            <td style="padding-right: 5px">
              <a href="${fn:escapeXml(prevMessage.link)}" rel=prev>←</a>
            </td>
            <td align=left valign=top class="hideon-phone">
              <a href="${fn:escapeXml(prevMessage.link)}" rel=prev>
                <l:title><l:mkTitle>${prevMessage.title}</l:mkTitle></l:title>
              </a>
            </td>
          </c:if>
        </tr>
      </table>
    </div>
    <div class="grid-3-2">
      <a title="${preparedMessage.section.title} - ${preparedMessage.group.title}"
         href="${group.url}">
          ${preparedMessage.group.title}
      </a>
    </div>
    <div class="grid-3-3">
      <c:if test="${nextMessage != null}">
        <table align="right">
          <tr valign=middle align=right>
            <td class="hideon-phone">
              <a href="${fn:escapeXml(nextMessage.link)}" rel=next>
                <l:title><l:mkTitle>${nextMessage.title}</l:mkTitle></l:title>
              </a>
            </td>
            <td align="right" valign="middle" style="padding-left: 5px">
              <a href="${fn:escapeXml(nextMessage.link)}" rel=next>→</a>
            </td>
          </tr>
        </table>
      </c:if>
    </div>
  </div>
  </div>
</c:if></c:set>

<c:set var="bufInfo">
<c:if test="${pages!=null}">
    <c:if test="${pages.hasPrevious}">
        &emsp;<a class='page-number' href='${pages.previous}#comments'>←</a>
    </c:if>
    <c:if test="${not pages.hasPrevious}">
        &emsp;<span class='page-number'>←</span>
    </c:if>

    <c:forEach var="i" items="${pages.pageLinks}">
        <c:if test="${not i.current}">
            <a class='page-number' href='${i.url}#comments'>${i.index + 1}</a>
        </c:if>
        <c:if test="${i.current}">
            <strong class='page-number'>${i.index + 1}</strong>
        </c:if>
    </c:forEach>

    <c:if test="${pages.hasNext}">
      <a class='page-number' href='${pages.next}#comments'>→</a>
    </c:if>
    <c:if test="${not pages.hasNext}">
        <span class='page-number'>→</span>
    </c:if>
</c:if>
</c:set>

<lor:message
        memoriesInfo="${memoriesInfo}"
        messageMenu="${messageMenu}"
        preparedMessage="${preparedMessage}" 
        message="${message}"
        briefEditInfo="${editInfo}"
        showMenu="true" enableSchema="true"/>

<c:out value="${scroller}" escapeXml="false"/>

<div class="comment" id="comments" style="padding-top: 0.5em">

<%--
<c:if test="${showAdsense}">
<style>
@media screen and (max-width: 480px) {
  .yandex-adaptive {
    min-height: 250px;
    width: 100%;
  }
}

@media screen and (min-width: 481px) {
  .yandex-adaptive { min-height: 90px; width: 100% }
}

@media screen and (min-width: 1024px) {
  .yandex-adaptive { min-height: 120px; width: 100% }
}

</style>
<div id="yandex_rtb" class="yandex-adaptive"></div>
<script type="text/javascript">
    if (window.matchMedia("(min-width: 1024px)").matches) {
        <!-- Yandex.RTB R-A-337082-5 -->
        (function (w, d, n, s, t) {
            w[n] = w[n] || [];
            w[n].push(function () {
                Ya.Context.AdvManager.render({
                    blockId: "R-A-337082-5",
                    renderTo: "yandex_rtb",
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
    } else {
        <!-- Yandex.RTB R-A-337082-2 -->
        (function (w, d, n, s, t) {
                w[n] = w[n] || [];
                w[n].push(function () {
                    Ya.Context.AdvManager.render({
                        blockId: "R-A-337082-2",
                        renderTo: "yandex_rtb",
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
    }
</script>
  <p>
</c:if>
--%>

  <div style="text-align: center; margin-top: 0.5em; height: 105px" id="interpage">
  </div>
  <script type="text/javascript">
      $script.ready('lorjs', function () {
          var ads = [
              {
                  type: 'rimg',
                  img730: '/adv/linux-banner-730-90.png',
                  img320: '/adv/linux-banner-320-100.png',
                  href: 'https://otus.ru/lessons/linux-professional/?utm_source=partners&utm_medium=cpm&utm_campaign=linux&utm_content=kungfu-test&utm_term=linux-org-ru'
              },
              {
                  type: 'rimg',
                  img730: '/adv/spec-730x90.png',
                  img320: '/adv/spec-320x100.png',
                  href: 'https://otus.ru/lessons/linux-specialization/?utm_source=partners&utm_medium=cpm&utm_campaign=spec-linux&utm_content=all-lesson&utm_term=linux-org-ru#broadcast'
              }
          ];

          init_interpage_adv(ads);
      });
  </script>

<c:if test="${threadMode}">
  <div class=nav>
    Показаны ответы на комментарий. <a href="${message.link}?cid=${threadRoot}">Показать</a> все комментарии.
  </div>
</c:if>

<c:if test="${filterMode!=defaultFilterMode}">
  <div class=nav>
    Показаны все комментарии. <a href="${message.getLinkPage(page)}">Скрыть</a> игнорируемые.
  </div>
</c:if>

<c:if test="${not empty bufInfo}">
    <c:if test="${not showDeleted}">
        <div class="nav">
            ${bufInfo}
        </div>
    </c:if>
</c:if>
    <c:forEach var="comment" items="${commentsPrepared}">
      <l:comment enableSchema="true" commentsAllowed="${messageMenu.commentsAllowed}" topic="${message}"
                 showMenu="true" comment="${comment}"/>
    </c:forEach>
</div>

<div id="realtime" style="display: none"></div>

<c:if test="${not messageMenu.commentsAllowed}">
  <div class="infoblock">
    <c:choose>
      <c:when test="${message.deleted}">
        Вы не можете добавлять комментарии в эту тему. Тема удалена.
      </c:when>
      <c:when test="${message.expired}">
        Вы не можете добавлять комментарии в эту тему. Тема перемещена в архив.
      </c:when>
      <c:otherwise>
        ${preparedMessage.postscoreInfo}
      </c:otherwise>
    </c:choose>
  </div>
</c:if>

<c:if test="${fn:length(commentsPrepared) > 0}">
  <c:if test="${ not empty bufInfo }">
    <div class="nav">
      ${bufInfo}
    </div>
  </c:if>

  <c:out value="${bottomScroller}" escapeXml="false"/>
</c:if>
</div>

<c:if test="${fn:length(commentsPrepared)!=unfilteredCount}">
  <div class=nav>
    Показано ${fn:length(commentsPrepared)} сообщений из ${unfilteredCount}. Показать <a href="${message.getLinkPage(page)}?filter=show">все</a>.
  </div>
</c:if>

<c:if test="${template.sessionAuthorized && (!message.expired || template.moderatorSession) && !showDeleted && !message.draft}">
    <hr>
    <form action="${message.link}" method=POST>
    <lor:csrf/>
    <input type=hidden name=deleted value=1>
    <input type=submit value="Показать удаленные комментарии">
    </form>
    <hr>
</c:if>

<% out.flush(); %>

<c:set var="moreLikeThis" value="${moreLikeThisGetter.call()}"/>

<c:if test="${not empty moreLikeThis}">
  <section id="related-topics">
    <h2>Похожие темы</h2>

    <div id="related-topics-list">
      <c:forEach var="sublist" items="${moreLikeThis}">
        <ul>
          <c:forEach var="topic" items="${sublist}">
            <li>
              <span class="group-label">${topic.section}</span>
              <a href="${topic.link}">${topic.title}</a> (${topic.year})
            </li>
          </c:forEach>
        </ul>
      </c:forEach>
    </div>
  </section>
</c:if>

<c:if test="${not message.expired and template.sessionAuthorized}">
  <div style="display: none">
    <c:url var="form_action_url" value="/add_comment.jsp" />
    <lor:commentForm
            topic="${message}"
            replyto="0"
            cancel="true"
            mode="${template.formatMode}"
            ipBlockInfo="${ipBlockInfo}"
            form_action_url="${form_action_url}"
            postscoreInfo="${preparedMessage.postscoreInfo}" modes="${modes}" />
  </div>
</c:if>

<jsp:include page="/WEB-INF/jsp/footer.jsp"/>
