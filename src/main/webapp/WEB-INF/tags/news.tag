<%--
  ~ Copyright 1998-2022 Linux.org.ru
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
<%@ tag import="com.google.common.net.InternetDomainName" %>
<%@ tag import="ru.org.linux.group.Group" %>
<%@ tag import="ru.org.linux.site.Template" %>
<%@ tag import="ru.org.linux.topic.Topic" %>
<%@ tag import="ru.org.linux.util.BadImageException" %>
<%@ tag import="ru.org.linux.util.StringUtil" %>
<%@ tag import="ru.org.linux.util.image.ImageInfo" %>
<%@ tag import="java.io.IOException" %>
<%@ tag import="java.net.URI" %>
<%@ tag import="ru.org.linux.spring.SiteConfig" %>
<%@ tag pageEncoding="UTF-8" trimDirectiveWhitespaces="true"%>
<%@ attribute name="preparedMessage" required="true" type="ru.org.linux.topic.PreparedTopic" %>
<%@ attribute name="messageMenu" required="true" type="ru.org.linux.topic.TopicMenu" %>
<%@ attribute name="multiPortal" required="true" type="java.lang.Boolean" %>
<%@ attribute name="moderateMode" required="true" type="java.lang.Boolean" %>
<%@ attribute name="minorAsMajor" required="false" type="java.lang.Boolean" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib tagdir="/WEB-INF/tags" prefix="lor" %>
<%@ taglib prefix="l" uri="http://www.linux.org.ru" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/functions" prefix="fn" %>
<%--@elvariable id="template" type="ru.org.linux.site.Template"--%>
<c:set var="message" value="${preparedMessage.message}"/>

<%
  Template tmpl = Template.getTemplate();
  Topic message = preparedMessage.getMessage();
  int pages = message.getPageCount(tmpl.getProf().getMessages());
%>

<c:set var="commentsLinks">
  <c:if test="${messageMenu.showComments && message.commentCount > 0}">
  <a href="${message.link}#comments"><lor:comment-count count="${message.commentCount}"/></a><%
      if (pages != 1) {
        int PG_COUNT=3;

        out.append("&nbsp;(стр.");
        boolean dots = false;

        for (int i = 1; i < pages; i++) {
          if (pages>PG_COUNT*3 && (i>PG_COUNT && i<pages-PG_COUNT)) {
            if (!dots) {
              out.append(" ...");
              dots = true;
            }

            continue;
          }

          out.append(" <a href=\"").append(message.getLinkPage(i)).append("#comments\">").append(Integer.toString(i + 1)).append("</a>");
        }

        out.append(')');
      }
  %>
  </c:if>
</c:set>

<c:if test="${not message.minor || minorAsMajor}">
<article class=news id="topic-${message.id}">
<%
  boolean votepoll = preparedMessage.getSection().isPollPostAllowed();

  String image = preparedMessage.getGroup().getImage();
  Group group = preparedMessage.getGroup();
%>
<h1>
  <a href="${fn:escapeXml(message.link)}"><l:title>${message.title}</l:title></a>
</h1>

  <c:if test="${multiPortal}">
    <div class="group">
        ${preparedMessage.section.title} — ${preparedMessage.group.title}
      <c:if test="${not message.commited and preparedMessage.section.premoderated}">
        <span>(не подтверждено)</span>
      </c:if>
    </div>
  </c:if>

  <c:if test="${preparedMessage.image != null}">
    <lor:image title="${preparedMessage.message.title}" image="${preparedMessage.image}" preparedMessage="${preparedMessage}" showImage="true"/>
  </c:if>

<c:set var="group" value="${preparedMessage.group}"/>

<c:if test="${group.image != null}">
<div class="entry-userpic">
  <a href="${group.url}">
  <%
    try {
      String htmlPathPrefix = ((SiteConfig) request.getAttribute("configuration")).getHTMLPathPrefix();
      ImageInfo info = new ImageInfo(htmlPathPrefix + "tango" + image);
      out.append("<img class=\"groupimg\" src=\"/").append("tango").append(image).append("\" ").append(info.getCode()).append(" alt=\"Группа ").append(group.getTitle()).append("\">");
    } catch (BadImageException e) {
      out.append("[bad image] <img class=newsimage src=\"/").append("tango").append(image).append("\" " + " alt=\"Группа ").append(group.getTitle()).append("\">");
    } catch (IOException e) {
      out.append("[bad image] <img class=newsimage src=\"/").append("tango").append(image).append("\" " + " alt=\"Группа ").append(group.getTitle()).append("\">");
    }
  %>
    </a>
</div>
</c:if>

<div class="entry-body">
<div class=msg>
  ${preparedMessage.processedMessage}
<c:if test="${message.url!=null}">
<%
  String url = message.getUrl();

  if (url.isEmpty()) {
    url = message.getLink();
  }

  out.append("<p>&gt;&gt;&gt; <a href=\"").append(StringUtil.escapeHtml(url)).append("\">").append(message.getLinktext()).append("</a>");

  if (moderateMode) {
    try {
      String host = URI.create(url).getHost();

      if (host != null) {
        String shortHost = InternetDomainName.from(host).topPrivateDomain().toString();

        out.append(" (" + shortHost + ")");
      } else {
        out.append(" (Invalid URL, no host part!)");
      }
    } catch (IllegalStateException ex) {
      out.append(" (" + ex.getMessage() + ")");
    } catch (IllegalArgumentException ex) {
      out.append(" (" + ex.getMessage() + ")");
    }
  }
%>
</c:if>
<c:if test="${preparedMessage.image != null}">
  <lor:image title="${preparedMessage.message.title}" image="${preparedMessage.image}" preparedMessage="${preparedMessage}" showInfo="true"/>
</c:if>
<%
  if (votepoll) {
      %>
        <c:choose>
            <c:when test="${not message.commited || not preparedMessage.message.expired}">
                <lor:poll-form poll="${preparedMessage.poll.poll}"
                    enabled="${!preparedMessage.message.expired && message.commited and currentUser !=null and preparedMessage.poll.userVotePossible}"/>
            </c:when>
            <c:otherwise>
                <lor:poll poll="${preparedMessage.poll}"/>
            </c:otherwise>
        </c:choose>

        <c:if test="${message.commited}">
          <p>&gt;&gt;&gt; <a href="${message.link}">Результаты</a>
        </c:if>
  <%
  }
%>
  </div>
<c:if test="${not empty preparedMessage.tags}">
  <l:tags list="${preparedMessage.tags}"/>
</c:if>

  <div class=sign>
  <c:choose>
    <c:when test="${preparedMessage.section.premoderated and message.commited}">
      <lor:sign shortMode="true" postdate="${message.commitDate}" user="${preparedMessage.author}" timeprop="datePublished"/>
    </c:when>
    <c:otherwise>
      <lor:sign shortMode="true" postdate="${message.postdate}" user="${preparedMessage.author}" timeprop="dateCreated"/>
    </c:otherwise>
  </c:choose>
  <c:if test="${preparedMessage.remark != null}">
    <span class="user-remark"><c:out value="${preparedMessage.remark.text}" escapeXml="true"/></span>
  </c:if>
  
</div>
<div class="nav">
<c:if test="${not moderateMode and messageMenu.commentsAllowed and message.commentCount == 0}">
  [<a href="comment-message.jsp?topic=${message.id}">Добавить&nbsp;комментарий</a>]
</c:if>
  <c:if test="${moderateMode}">
    <c:if test="${messageMenu.commitable}">
      [<a href="commit.jsp?msgid=${message.id}">Подтвердить</a>]
    </c:if>
  </c:if>
  <c:if test="${moderateMode || message.draft}">
    <c:if test="${messageMenu.deletable}">
       [<a href="delete.jsp?msgid=${message.id}">Удалить</a>]
    </c:if>
    <c:if test="${messageMenu.editable}">
       [<a href="edit.jsp?msgid=${message.id}">Править</a>]
    </c:if>
  </c:if>
  <c:out value="${commentsLinks}" escapeXml="false"/>
  </div>
  </div>
</article>
</c:if>

<c:if test="${message.minor && !minorAsMajor}">
<article class="infoblock mini-news" id="topic-${message.id}">
  <a href="${fn:escapeXml(message.link)}"><l:title>${message.title}</l:title></a>

<c:if test="${multiPortal}">
    <c:if test="${not message.commited and preparedMessage.section.premoderated}">
      <span> (не подтверждено)</span>
    </c:if>
</c:if>

  <c:if test="${messageMenu.showComments && message.commentCount>0}">
    (<lor:comment-count count="${message.commentCount}"/>)
  </c:if>
</article>
</c:if>
