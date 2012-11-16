<%@ page contentType="text/html; charset=utf-8"%>
<%@ page buffer="200kb"%>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/functions" prefix="fn" %>
<%@ taglib prefix="l" uri="http://www.linux.org.ru" %>
<%@ taglib tagdir="/WEB-INF/tags" prefix="lor" %>
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

<%--@elvariable id="showAdsense" type="Boolean"--%>
<%--@elvariable id="message" type="ru.org.linux.topic.Topic"--%>
<%--@elvariable id="preparedMessage" type="ru.org.linux.topic.PreparedTopic"--%>
<%--@elvariable id="messageMenu" type="ru.org.linux.topic.TopicMenu"--%>
<%--@elvariable id="prevMessage" type="ru.org.linux.topic.Topic"--%>
<%--@elvariable id="nextMessage" type="ru.org.linux.topic.Topic"--%>
<%--@elvariable id="template" type="ru.org.linux.site.Template"--%>
<%--@elvariable id="showDeleted" type="Boolean"--%>
<%--@elvariable id="comments" type="ru.org.linux.comment.CommentList"--%>
<%--@elvariable id="group" type="ru.org.linux.group.Group"--%>
<%--@elvariable id="commentsPrepared" type="java.util.List<ru.org.linux.comment.PreparedComment>"--%>
<%--@elvariable id="page" type="Integer"--%>
<%--@elvariable id="unfilteredCount" type="java.lang.Integer"--%>

<jsp:include page="/WEB-INF/jsp/head.jsp"/>

<title><l:title>${message.title}</l:title> - ${preparedMessage.group.title} - ${preparedMessage.section.title}</title>
<meta property="og:title" content="<l:title>${message.title}</l:title>" >

<c:if test="${preparedMessage.section.imagepost}">
  <meta property="og:image" content="${preparedMessage.image.mediumName}">
</c:if>
<c:if test="${not preparedMessage.section.imagepost}">
  <meta property="og:image" content="${template.mainUrlNoSlash}/img/good-penguin.jpg">
</c:if>
<c:if test="${not empty preparedMessage.ogDescription}">
  <meta property="og:description" content="${preparedMessage.ogDescription}">
</c:if>

<meta property="og:url" content="${template.mainUrlNoSlash}${message.link}">

<link rel="canonical" href="${template.mainUrlNoSlash}${message.getLinkPage(page)}">

<c:if test="${prevMessage != null}">
  <link rel="Previous" id="PrevLink" href="${fn:escapeXml(prevMessage.link)}" title="<l:title><l:mkTitle>${prevMessage.title}</l:mkTitle></l:title>">
</c:if>

<c:if test="${nextMessage != null}">
  <link rel="Next" id="NextLink" href="${fn:escapeXml(nextMessage.link)}" title="<l:title><l:mkTitle>${nextMessage.title}</l:mkTitle></l:title>">
</c:if>

<c:if test="${not message.expired}">
  <LINK REL="alternate" TITLE="Comments RSS" HREF="${message.link}?output=rss" TYPE="application/rss+xml">
</c:if>

<script type="text/javascript">
  <!--
  $(document).bind('keydown', {combi:'Ctrl+left', disableInInput: true}, function(){ jump(document.getElementById ('PrevLink')) });
  $(document).bind('keydown', {combi:'Ctrl+right', disableInInput: true}, function(){ jump(document.getElementById ('NextLink')) });
  -->
</script>
<c:if test="${not message.expired and template.sessionAuthorized}">
<script src="/js/addComments.js" type="text/javascript"></script>
</c:if>
<jsp:include page="/WEB-INF/jsp/header.jsp"/>

<div class=messages itemscope itemtype="http://schema.org/Article">

<div class=nav>
  <div id="navPath" itemprop="articleSection">
    <a href="${preparedMessage.section.sectionLink}">${preparedMessage.section.title}</a> -
    <a href="${group.url}">${group.title}</a>
    <c:if test="${preparedMessage.section.premoderated and not message.commited}">
      (не подтверждено)
    </c:if>
  </div>
</div>

<c:set var="scroller"><c:if test="${topScroller}">
  <div class="nav grid-row">
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
</c:if></c:set>

<c:set var="bottomScroller"><c:if test="${bottomScroller}">
  <div class="nav grid-row">
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
</c:if></c:set>

<c:if test="${pages > 1}">
    <c:set var="urlAdd" value='' />
    <c:set var="bufInfo" value='' />
    <c:set var="filterAdd" value='' />

    <c:if test="${not message.expired}">
        <c:set var="urlAdd" value="?lastmod=${message.lastModified.time}" />
    </c:if>

    <c:if test="${filterMode != defaultFilterMode}">
        <c:choose>
            <c:when test="${empty urlAdd}">
                <c:set var="urlAdd" value="?" />
            </c:when>
            <c:otherwise>
                <c:set var="urlAdd" value="${urlAdd}&" />
            </c:otherwise>
        </c:choose>
        <c:set var="filterAdd" value="?filter=${filterMode}" />
        <c:set var="urlAdd" value="${urlAdd}filter=${filterMode}" />
    </c:if>
    <c:if test="${page != -1 and page != 0}">
        <c:set var="bufInfo" value="&emsp;<a class='page-number' href='${message.getLinkPage(page-1)}${filterAdd}'>←</a>" />
    </c:if>
    <c:if test="${page == -1 or page == 0}">
        <c:set var="bufInfo" value="&emsp;<span class='page-number'>←</span>" />
    </c:if>

    <c:forEach var="i" begin="0" end="${pages-1}">
        <c:set var="bufInfo" value="${bufInfo} " />
        <c:if test="${i != page}">
            <c:if test="${i == pages-1}">
                <c:set var="bufInfo" value="${bufInfo}<a class='page-number' href='${message.getLinkPage(i)}${urlAdd}'" />
            </c:if>
            <c:if test="${i != pages-1}">
                <c:set var="bufInfo" value="${bufInfo}<a class='page-number' href='${message.getLinkPage(i)}${filterAdd}'" />
            </c:if>
            <c:set var="bufInfo" value="${bufInfo}>${i + 1}</a>" />
        </c:if>
        <c:if test="${i == page}">
            <c:set var="bufInfo" value="${bufInfo}<strong class='page-number'>${i + 1}</strong>" />
        </c:if>
    </c:forEach>

    <c:if test="${page != -1 and page + 1 != pages}">
        <c:if test="${page + 1 == pages - 1}">
            <c:set var="bufInfo" value="${bufInfo} <a class='page-number' href='${message.getLinkPage(page+1)}${urlAdd}'>→</a>" />
        </c:if>
        <c:if test="${page + 1 != pages - 1}">
            <c:set var="bufInfo" value="${bufInfo} <a class='page-number' href='${message.getLinkPage(page+1)}${filterAdd}'>→</a>" />
        </c:if>
    </c:if>
    <c:if test="${page == -1 or page + 1 == pages}">
        <c:set var="bufInfo" value="${bufInfo} <span class='page-number'>→</span>" />
    </c:if>
</c:if>

<lor:message
        messageMenu="${messageMenu}"
        preparedMessage="${preparedMessage}" 
        message="${message}"
        showMenu="true" enableSchema="true"/>

<c:out value="${scroller}" escapeXml="false"/>

<c:if test="${showAdsense}">
  <div style="text-align: center; margin-top: 1em">
    <jsp:include page="/WEB-INF/jsp/${template.style}/adsense.jsp"/>
  </div>
  <br>
</c:if>

<c:if test="${fn:length(commentsPrepared)>0 and template.prof.showNewFirst}">
  <div class=nav>
    сообщения отсортированы в порядке убывания даты их написания
  </div>
</c:if>

<c:if test="${fn:length(commentsPrepared)!=unfilteredCount}">
  <div class=nav>
    Показано ${fn:length(commentsPrepared)} сообщений из ${unfilteredCount}. Показать <a href="${message.getLinkPage(page)}?filter=show">все</a>.
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
<div class="comment">
    <c:forEach var="comment" items="${commentsPrepared}">
      <lor:comment enableSchema="true" commentsAllowed="${messageMenu.commentsAllowed}" topic="${message}" showMenu="true" comment="${comment}" comments="${comments}" expired="${message.expired}"/>
    </c:forEach>
  </div>
<c:if test="${fn:length(commentsPrepared) > 0}">
  <c:if test="${ not empty bufInfo }">
    <div class="nav">
      ${bufInfo}
    </div>
  </c:if>

  <c:out value="${bottomScroller}" escapeXml="false"/>
</c:if>
</div>

<c:if test="${template.sessionAuthorized && (!message.expired || template.moderatorSession) && !showDeleted}">
    <hr>
    <form action="${message.link}" method=POST>
    <lor:csrf/>
    <input type=hidden name=deleted value=1>
    <input type=submit value="Показать удаленные комментарии">
    </form>
    <hr>
</c:if>

<c:if test="${not message.expired and template.sessionAuthorized}">
  <div style="display: none">
    <c:url var="form_action_url" value="/add_comment.jsp" />
    <lor:commentForm
            topic="${message}"
            title=""
            replyto="0"
            cancel="true"
            mode="${template.formatMode}"
            ipBlockInfo="${ipBlockInfo}"
            form_action_url="${form_action_url}"
            postscoreInfo="${preparedMessage.postscoreInfo}" />
  </div>
</c:if>

<jsp:include page="/WEB-INF/jsp/footer.jsp"/>
