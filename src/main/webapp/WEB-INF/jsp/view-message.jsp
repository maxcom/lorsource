<%@ page contentType="text/html; charset=utf-8"%>
<%@ page import="org.apache.commons.lang.math.RandomUtils,ru.org.linux.comment.CommentFilter,ru.org.linux.site.Template,ru.org.linux.topic.Topic"   buffer="200kb"%>
<%@ page import="ru.org.linux.util.StringUtil" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/functions" prefix="fn" %>
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

<% Template tmpl = Template.getTemplate(request); %>
<jsp:include page="/WEB-INF/jsp/head.jsp"/>

<%
  int filterMode = (Integer) request.getAttribute("filterMode");

  int npage = (Integer) request.getAttribute("page");

  Topic message = (Topic) request.getAttribute("message");
  Topic prevMessage = (Topic) request.getAttribute("prevMessage");
  Topic nextMessage = (Topic) request.getAttribute("nextMessage");
%>

<title>${message.title} - ${preparedMessage.group.title} - ${preparedMessage.section.title}</title>
<meta property="og:title" content="${message.title}" >

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

<link rel="canonical" href="${template.mainUrlNoSlash}<%= message.getLinkPage(npage) %>">

<c:if test="${prevMessage != null}">
  <link rel="Previous" id="PrevLink" href="${fn:escapeXml(prevMessage.link)}" title="<%= StringUtil.makeTitle(prevMessage.getTitle()) %>">
</c:if>

<c:if test="${nextMessage != null}">
  <link rel="Next" id="NextLink" href="${fn:escapeXml(nextMessage.link)}" title="<%= StringUtil.makeTitle(nextMessage.getTitle()) %>">
</c:if>

<LINK REL="alternate" TITLE="Comments RSS" HREF="${message.link}?output=rss" TYPE="application/rss+xml">
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

<form method="GET" action="view-message.jsp">
<input type=hidden name=msgid value="${message.id}">
  <div class=nav>
  <div id="navPath" itemprop="articleSection">
    <a href="${preparedMessage.section.sectionLink}">${preparedMessage.section.title}</a> -
    <a href="${group.url}">${group.title}</a>
    <c:if test="${preparedMessage.section.premoderated and not message.commited}">
        (не подтверждено)
    </c:if>
  </div>

    <div class="nav-buttons">
      <ul>
      <li><a href="${message.link}?output=rss">RSS</a></li>

      <c:if test="${!showDeleted}">
        <input type=hidden name=page value="${page}">
        <c:if test="${not template.usingDefaultProfile}">
           <li><a href="<c:url value="/user-filter"/>">Фильтр</a></li>
        </c:if>
      </ul>

        <select name="filter" onChange="submit();">
<%
    out.print("<option value=\"" + CommentFilter.toString(CommentFilter.FILTER_NONE) + '\"' + (filterMode == CommentFilter.FILTER_NONE ? " selected=\"selected\"" : "") + ">все комментарии</option>");
    out.print("<option value=\"" + CommentFilter.toString(CommentFilter.FILTER_ANONYMOUS) + '\"' + ((filterMode&CommentFilter.FILTER_ANONYMOUS)!=0 ? " selected=\"selected\"" : "") + ">без анонимных</option>");

    if (!tmpl.isUsingDefaultProfile()) {
      out.print("<option value=\"" + CommentFilter.toString(CommentFilter.FILTER_IGNORED) + '\"' + (filterMode == CommentFilter.FILTER_IGNORED ? " selected=\"selected\"" : "") + ">без игнорируемых</option>");
    }
%>
          </select>
      </c:if>
    </div>
  </div>
</form>

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
                <%= StringUtil.makeTitle(prevMessage.getTitle()) %>
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
                <%= StringUtil.makeTitle(nextMessage.getTitle()) %>
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
                <%= StringUtil.makeTitle(prevMessage.getTitle()) %>
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
                <%= StringUtil.makeTitle(nextMessage.getTitle()) %>
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

<c:if test="${showDeleted}">
  <h1 class="optional">Режим показа удаленных комментариев</h1>
</c:if>

<%
  int messages = tmpl.getProf().getMessages();
  int pages = message.getPageCount(messages);

  String pageInfo = null;
  if (pages > 1) {
    StringBuilder bufInfo = new StringBuilder();

    StringBuilder urlAdd = new StringBuilder();
    if (!message.isExpired()) {
      urlAdd.append("?lastmod=").append(message.getLastModified().getTime());
    }

    String filterAdd="";

    if (filterMode!= (Integer) request.getAttribute("defaultFilterMode")) {
      if (urlAdd.length()>0) {
        urlAdd.append('&');
      } else {
        urlAdd.append('?');
      }

      filterAdd="?filter="+CommentFilter.toString(filterMode);
      urlAdd.append("filter=").append(CommentFilter.toString(filterMode));
    }

    if (npage!=-1 && npage!=0) {
      bufInfo.append("&emsp;<a class=\"page-number\" href=\"").append(message.getLinkPage(npage-1)).append(filterAdd).append("\">");
      bufInfo.append('←');
      bufInfo.append("</a>");
    } else {
      bufInfo.append("&emsp;<span  class=\"page-number\">←</span>");
    }

    for (int i = 0; i < pages; i++) {
      bufInfo.append(' ');

      if (i != npage) {
        if (i>0) {
          if (i==pages-1) {
            bufInfo.append("<a class=\"page-number\" href=\"").append(message.getLinkPage(i)).append(urlAdd);
          } else {
            bufInfo.append("<a class=\"page-number\" href=\"").append(message.getLinkPage(i)).append(filterAdd);
          }
        } else {
          bufInfo.append("<a class=\"page-number\" href=\"").append(message.getLink()).append(filterAdd);
        }

        bufInfo.append("\">").append(i + 1).append("</a>");
      } else {
        bufInfo.append("<strong class=\"page-number\">").append(i + 1).append("</strong>");
      }
    }

    if (npage!=-1 && npage+1!=pages) {
      if (npage+1==pages-1) {
        bufInfo.append(" <a class=\"page-number\" href=\"").append(message.getLinkPage(npage+1)).append(urlAdd).append("\">→</a>");
      } else {
        bufInfo.append(" <a class=\"page-number\" href=\"").append(message.getLinkPage(npage+1)).append(filterAdd).append("\">→</a>");
      }
    } else {
      bufInfo.append(" <span class=\"page-number\">→</span>");
    }

    pageInfo = bufInfo.toString();
  }
%>

<lor:message
        messageMenu="${messageMenu}"
        preparedMessage="${preparedMessage}" 
        message="${message}"
        showMenu="true" enableSchema="true"/>

<c:out value="${scroller}" escapeXml="false"/>

<c:if test="${showAdsense}">
<%
  if (RandomUtils.nextInt(3)==0) {
%>
<jsp:include page="/WEB-INF/jsp/croco-adv.jsp"/>
<%
  } else {
%>
  <div style="text-align: center; margin-top: 1em">
    <jsp:include page="/WEB-INF/jsp/${template.style}/adsense.jsp"/>
  </div>
<% } %>
  <br>
</c:if>

<c:if test="${fn:length(commentsPrepared)>0 and template.prof.showNewFirst}">
  <div class=nav>
    сообщения отсортированы в порядке убывания даты их написания
  </div>
</c:if>

<c:if test="<%= pageInfo!=null %>">
    <c:if test="${not showDeleted}">
        <div class="nav">
            <%= pageInfo %>
        </div>
    </c:if>
</c:if>
<div class="comment">
    <c:forEach var="comment" items="${commentsPrepared}">
      <lor:comment commentsAllowed="${messageMenu.commentsAllowed}" topic="${message}" showMenu="true" comment="${comment}" comments="${comments}" expired="${message.expired}"/>
    </c:forEach>
  </div>
<c:if test="${fn:length(commentsPrepared) > 0}">
  <c:if test="<%= pageInfo!=null %>">
    <div class="nav">
      <%= pageInfo %>
    </div>
  </c:if>

  <c:out value="${bottomScroller}" escapeXml="false"/>
</c:if>
</div>

<% if (tmpl.isSessionAuthorized() && (!message.isExpired() || tmpl.isModeratorSession()) && !(Boolean) request.getAttribute("showDeleted")) { %>
<hr>
<form action="${message.link}" method=POST>
<input type=hidden name=deleted value=1>
<input type=submit value="Показать удаленные комментарии">
</form>
<hr>
<% } %>

<c:if test="${not message.expired and template.sessionAuthorized}">
  <div style="display: none">
    <lor:commentForm
            topic="${message}"
            title=""
            replyto="0"
            cancel="true"
            mode="${template.formatMode}"
            ipBlockInfo="${ipBlockInfo}"
            postscoreInfo="${preparedMessage.postscoreInfo}" />
  </div>
</c:if>

<jsp:include page="/WEB-INF/jsp/footer.jsp"/>
