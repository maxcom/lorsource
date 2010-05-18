<%@ page contentType="text/html; charset=utf-8"%>
<%@ page import="java.sql.Connection,java.util.Set,ru.org.linux.site.*,ru.org.linux.util.ServletParameterParser,ru.org.linux.util.StringUtil"   buffer="200kb"%>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/functions" prefix="fn" %>
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

<%--@elvariable id="showAdsense" type="Boolean"--%>
<%--@elvariable id="message" type="ru.org.linux.site.Message"--%>
<%--@elvariable id="prevMessage" type="ru.org.linux.site.Message"--%>
<%--@elvariable id="nextMessage" type="ru.org.linux.site.Message"--%>
<%--@elvariable id="template" type="ru.org.linux.site.Template"--%>
<%--@elvariable id="showDeleted" type="Boolean"--%>
<%--@elvariable id="comments" type="ru.org.linux.site.CommentList"--%>
<%--@elvariable id="group" type="ru.org.linux.site.Group"--%>

<% Template tmpl = Template.getTemplate(request); %>
<jsp:include page="/WEB-INF/jsp/head.jsp"/>

<%
  Connection db = null;

  try {
    int msgid = (Integer) request.getAttribute("msgid");

    String nick = Template.getNick(session);

    int filterMode = (Integer) request.getAttribute("filterMode");
    int defaultFilterMode = (Integer) request.getAttribute("defaultFilterMode");

    int npage = 0;
    if (request.getAttribute("page") != null) {
      npage = (Integer) request.getAttribute("page");
    }

    boolean showDeleted = (Boolean) request.getAttribute("showDeleted");

    if (showDeleted) {
      npage = -1;
    }

  Message message = (Message) request.getAttribute("message");
  Message prevMessage = (Message) request.getAttribute("prevMessage");
  Message nextMessage = (Message) request.getAttribute("nextMessage");

  String mainurl = message.getLink();
%>

<title>${message.sectionTitle} - ${message.groupTitle} - ${message.title}</title>
<link rel="parent" title="${message.sectionTitle} - ${message.groupTitle}" href="group.jsp?group=${message.groupId}">
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
<div class=messages>

<form method="GET" action="view-message.jsp">
<input type=hidden name=msgid value="<%= msgid %>">
  <table class=nav>
  <tr>
  <td align=left valign=middle id="navPath">
    <a href="<%= Section.getSectionLink(message.getSectionId()) %>"><%= message.getSectionTitle() %></a> -
    <a href="${group.url}">${group.title}</a>
  </td>

    <td align=right>
      <ul>
      <li>[<a href="${message.link}?output=rss">RSS</a>]</li>

      <c:if test="${!showDeleted}">
<%
    if (npage != 0) {
      out.print("<input type=hidden name=page value=\"" + npage + "\">");
    }
%>
        <c:if test="${not template.usingDefaultProfile}">
           <li>[<a href="ignore-list.jsp">Фильтр</a>]</li>
        </c:if>
        <li><select name="filter" onChange="submit();">
<%
    out.print("<option value=\"" + CommentFilter.toString(CommentFilter.FILTER_NONE) + '\"' + (filterMode == CommentFilter.FILTER_NONE ? " selected=\"selected\"" : "") + ">все комментарии</option>");
    out.print("<option value=\"" + CommentFilter.toString(CommentFilter.FILTER_ANONYMOUS) + '\"' + ((filterMode&CommentFilter.FILTER_ANONYMOUS)!=0 ? " selected=\"selected\"" : "") + ">без анонимных</option>");

    if (!tmpl.isUsingDefaultProfile()) {
      out.print("<option value=\"" + CommentFilter.toString(CommentFilter.FILTER_IGNORED) + '\"' + (filterMode == CommentFilter.FILTER_IGNORED ? " selected=\"selected\"" : "") + ">без игнорируемых</option>");
    }
%>
          </select></li>
      </c:if>
        </ul>
    </td>
  </table>
</form>
<c:set var="scrollGroup" value="<%= Section.getScrollMode(message.getSectionId())==Section.SCROLL_GROUP %>"/>

<c:set var="scroller">
<%
  int scroll = Section.getScrollMode(message.getSectionId());

  if (prevMessage == null && nextMessage == null) {
    scroll = Section.SCROLL_NOSCROLL;
  }

  if (scroll != Section.SCROLL_NOSCROLL) {
%>
    <table class=nav>
      <tr>
        <td align=left valign=middle width="35%">
          <table>
            <tr valign=middle>
                <c:if test="${prevMessage != null}">
                  <td style="padding-right: 5px">
                    <a href="${fn:escapeXml(prevMessage.link)}" rel=prev rev=next>←</a>
                  </td>
                  <td align=left valign=top>
                    <a href="${fn:escapeXml(prevMessage.link)}" rel=prev rev=next>
                    <%= StringUtil.makeTitle(prevMessage.getTitle()) %></a>
                    <c:if test="${!scrollGroup}">
                      (${prevMessage.groupTitle})
                    </c:if>
                  </td>
                </c:if>
            </tr>
          </table>
        </td>
        <td align=left valign=middle width="35%">
          <c:if test="${nextMessage != null}">
            <table align="right">
              <tr valign=middle align=right>
                <td>
                  <a href="${fn:escapeXml(nextMessage.link)}" rel=next rev=prev>
                    <%= StringUtil.makeTitle(nextMessage.getTitle()) %>
                  </a>
                  <c:if test="${!scrollGroup}">
                    (${nextMessage.groupTitle})
                  </c:if>
                </td>
                <td align="right" valign="middle" style="padding-left: 5px">
                  <a href="${fn:escapeXml(nextMessage.link)}" rel=next rev=prev>→</a>
                </td>
              </tr>
            </table>
          </c:if>
        </td>
      </tr>
    </table>
<%
   }
%>

</c:set>

<c:set var="bottomScroller">
<%
  int scroll = Section.getScrollMode(message.getSectionId());

  if (scroll != Section.SCROLL_NOSCROLL) {
%>
    <table class=nav>
      <tr>
        <td align=left valign=middle width="35%">
          <table>
            <tr valign=middle>
                <c:if test="${prevMessage != null}">
                  <td style="padding-right: 5px">
                    <a href="${fn:escapeXml(prevMessage.link)}" rel=prev rev=next>←</a>
                  </td>
                  <td align=left valign=top>
                    <a href="${fn:escapeXml(prevMessage.link)}" rel=prev rev=next>
                    <%= StringUtil.makeTitle(prevMessage.getTitle()) %></a>
                    <c:if test="${!scrollGroup}">
                      (${prevMessage.groupTitle})
                    </c:if>
                  </td>
                </c:if>
            </tr>
          </table>
        </td>
        <td align=center valign=middle>
          <table>
            <tr valign=middle>
              <td>
                <a title="<%=  message.getSectionTitle() + " - " + message.getGroupTitle() %>"
                   href="${group.url}">
                  <%= message.getSectionTitle() + " - " + message.getGroupTitle() %>
                </a>
              </td>
            </tr>
          </table>
        <td align=right valign=middle width="35%">
          <c:if test="${nextMessage != null}">
            <table align="right">
              <tr valign=middle align=right>
                <td>
                  <a href="${fn:escapeXml(nextMessage.link)}" rel=next rev=prev>
                    <%= StringUtil.makeTitle(nextMessage.getTitle()) %>
                  </a>
                  <c:if test="${!scrollGroup}">
                    (${nextMessage.groupTitle})
                  </c:if>
                </td>
                <td align="right" valign="middle" style="padding-left: 5px">
                  <a href="${fn:escapeXml(nextMessage.link)}" rel=next rev=prev>→</a>
                </td>
              </tr>
            </table>
          </c:if>
        </td>
      </tr>
    </table>
<%
   }
%>

</c:set>

<c:if test="${showDeleted}">
  <h1 class="optional">Режим показа удаленных комментариев</h1>
</c:if>

<%
  int messages = tmpl.getProf().getInt("messages");
  int pages = message.getPageCount(messages);

  String pageInfo = null;
  if (pages > 1) {
    StringBuilder bufInfo = new StringBuilder();

    bufInfo.append("[страница");


    StringBuilder urlAdd = new StringBuilder();
    if (!message.isExpired()) {
      urlAdd.append("?lastmod="+message.getLastModified().getTime());
    }

    String filterAdd="";

    if (filterMode!=defaultFilterMode) {
      if (urlAdd.length()>0) {
        urlAdd.append("&");
      } else {
        urlAdd.append("?");
      }

      filterAdd="?filter="+CommentFilter.toString(filterMode);
      urlAdd.append("filter=").append(CommentFilter.toString(filterMode));
    }

    if (npage!=-1 && npage!=0) {
      bufInfo.append("&emsp;<a href=\"").append(message.getLinkPage(npage-1)).append(filterAdd).append("\">");
      bufInfo.append('←');
      bufInfo.append("</a>");
    } else {
      bufInfo.append("&emsp;←");
    }


    for (int i = 0; i < pages; i++) {
      bufInfo.append(' ');

      if (i != npage) {
        if (i>0) {
          if (i==pages-1) {
            bufInfo.append("<a href=\"").append(message.getLinkPage(i)).append(urlAdd);
          } else {
            bufInfo.append("<a href=\"").append(message.getLinkPage(i)).append(filterAdd);
          }
        } else {
          bufInfo.append("<a href=\"").append(mainurl).append(filterAdd);
        }

        bufInfo.append("\">").append(i + 1).append("</a>");
      } else {
        bufInfo.append("<strong>").append(i + 1).append("</strong>");
      }
    }

    if (npage!=-1 && npage+1!=pages) {
      bufInfo.append(" <a href=\"").append(message.getLinkPage(npage+1)).append(urlAdd).append("\">→</a>");
    } else {
      bufInfo.append(" →");
    }

    if (Template.isSessionAuthorized(session)) {
      if (npage!=-1) {
        bufInfo.append("&emsp;<a href=\"").append(message.getLinkPage(-1)).append(urlAdd).append("\">все").append("</a>");
      } else {
        bufInfo.append("&emsp;<strong>все").append("</strong>");      
      }
    }

    bufInfo.append(']');
    pageInfo = bufInfo.toString();
  }

  db = LorDataSource.getConnection();

  if (request.getParameter("highlight") != null) {
%>
<lor:message db="<%= db %>" message="${message}" showMenu="true" user="<%= nick %>" highlight="<%= new ServletParameterParser(request).getInt(&quot;highlight&quot;)%>"/>
<%
  } else {
%>
<lor:message db="<%= db %>" message="${message}" showMenu="true" user="<%= nick %>"/>
<%
  }
%>

<c:out value="${scroller}" escapeXml="false"/>

<c:if test="${showAdsense}">
  <div style="text-align: center; margin-top: 1em">
    <jsp:include page="/WEB-INF/jsp/${template.style}/adsense.jsp"/>
  </div>
  <br>
</c:if>

<%
    int offset = 0;
    int limit = 0;
    boolean reverse = tmpl.getProf().getBoolean("newfirst");

    if (npage != -1) {
      limit = messages;
      offset = messages * npage;
    }

    CommentList comments = (CommentList) request.getAttribute("comments");
    Set<Integer> hideSet = (Set<Integer>) request.getAttribute("hideSet");

    CommentFilter cv = new CommentFilter(comments);
%>
<c:set var="commentsPrepared" value="<%= cv.getComments(reverse, offset, limit, hideSet) %>"/>
<c:set var="sortwarning" value="<%= tmpl.getProf().getBoolean(&quot;sortwarning&quot;) %>"/>
<c:if test="${sortwarning && fn:length(commentsPrepared)>0}">
  <div class=nav>
<%
      if (tmpl.getProf().getBoolean("newfirst")) {
        out.print("сообщения отсортированы в порядке убывания даты их написания");
      } else {
        out.print("сообщения отсортированы в порядке возрастания даты их написания");
      }
%>
    </div>
  </c:if>
  <c:if test="<%= pageInfo!=null %>">
    <div class="nav">
      <%= pageInfo %>
    </div>
  </c:if>
  <div class="comment">
    <c:forEach var="comment" items="${commentsPrepared}">
      <lor:comment topic="${message}" showMenu="true" comment="${comment}" db="<%= db %>" comments="${comments}" expired="${message.expired}"/>
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

<% if (Template.isSessionAuthorized(session) && (!message.isExpired() || tmpl.isModeratorSession()) && !showDeleted) { %>
<hr>
<form action="${message.link}" method=POST>
<input type=hidden name=deleted value=1>
<input type=submit value="Показать удаленные комментарии">
</form>
<hr>
<% } %>

<%
  } finally {
    if (db!=null) {
      db.close();
    }
  }
%>

<c:if test="${not template.mobile}">
  <script type="text/javascript">
    <!--
    $(document).ready(function(){
      var iframe = $('<iframe src="/dw.jsp?width=728&amp;height=90&amp;main=0" width="728" height="90" scrolling="no" frameborder="0"></iframe>');
      $('#dw').append(iframe);
    });
    -->
  </script>
<div align=center id="dw">
</div>
</c:if>

<c:if test="${not message.expired and template.sessionAuthorized}">
  <div style="display: none">
    <lor:commentForm topicId="${message.id}" title=""
                     postscore="${message.postScore}" replyto="0" cancel="true"/>
  </div>
</c:if>

<jsp:include page="/WEB-INF/jsp/footer.jsp"/>
