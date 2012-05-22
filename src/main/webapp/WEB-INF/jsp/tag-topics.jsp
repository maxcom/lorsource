<%@ page contentType="text/html; charset=utf-8"%>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib tagdir="/WEB-INF/tags" prefix="lor" %>
<%@ taglib prefix="fn" uri="http://java.sun.com/jsp/jstl/functions" %>
<%@ taglib prefix="form" uri="http://www.springframework.org/tags/form" %>
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
<%--@elvariable id="counter" type="java.lang.Integer"--%>
<jsp:include page="/WEB-INF/jsp/head.jsp"/>
	<title>${ptitle}</title>

<c:if test="${rssLink != null}">
  <LINK REL="alternate" HREF="${rssLink}" TYPE="application/rss+xml">
</c:if>

<jsp:include page="/WEB-INF/jsp/header.jsp"/>
  <div class=nav>
    <div id="navPath">
      <i class="icon-tag"></i> ${navtitle}
    </div>
    <div class="nav-buttons">
      <ul>
        <c:if test="${isShowFavoriteTagButton}">
          <li>
            <c:url var="tagFavUrl" value="/user-filter">
              <c:param name="newFavoriteTagName" value="${topicListRequest.tag}"/>
            </c:url>

            <a id="tagFavAdd" href="${tagFavUrl}">В избранные теги</a>
          </li>
        </c:if>
        <c:if test="${isShowUnFavoriteTagButton}">
          <li>
            <c:url var="tagFavUrl" value="/user-filter"/>

            <a id="tagFavAdd" href="${tagFavUrl}">Удалить из избранного</a>
          </li>
        </c:if>
        <c:if test="${isShowIgnoreTagButton}">
          <li>
          <c:url var="tagIgnUrl" value="/user-filter">
            <c:param name="newIgnoredTagName" value="${topicListRequest.tag}"/>
          </c:url>

          <a id="tagIgnAdd" href="${tagIgnUrl}">Игнорировать тег</a>
            </li>
        </c:if>
        <c:if test="${isShowUnIgnoreTagButton}">
          <li>
          <c:url var="tagIgnUrl" value="/user-filter"/>

          <a id="tagIgnAdd" href="${tagIgnUrl}">Не игнорировать тег</a>
          </li>
        </c:if>

      <c:if test="${rssLink != null}">
        <li><a href="${rssLink}">RSS</a></li>
      </c:if>
      </ul>
      <c:if test="${sectionList != null}">
        <form:form commandName="topicListRequest" id="filterForm" action="${url}" method="get">
          <form:select path="section" onchange="$('#group').val('0'); $('#filterForm').submit();">
            <form:option value="0" label="Все" />
            <form:options items="${sectionList}" itemLabel="title" itemValue="id" />
          </form:select>
          <noscript><input type='submit' value='&gt;'></noscript>
        </form:form>
      </c:if>
    </div>
</div>
<H1 class="optional">${ptitle}</H1>

<div class="infoblock">
  Всего сообщений: ${counter}
</div>

<c:forEach var="msg" items="${messages}">
  <lor:news preparedMessage="${msg.preparedTopic}" messageMenu="${msg.topicMenu}" multiPortal="${section==null && group==null}" moderateMode="false"/>
</c:forEach>

<c:if test="${params !=null}">
  <c:set var="aparams" value="${params}&"/>
</c:if>

<table class="nav">
  <tr>
    <c:if test="${topicListRequest.offset < 200 && fn:length(messages) == 20}">
      <td align="left" width="35%">
        <a href="${url}?${aparams}offset=${topicListRequest.offset+20}">← предыдущие</a>
      </td>
    </c:if>
    <c:if test="${topicListRequest.offset > 20}">
      <td width="35%" align="right">
        <a href="${url}?${aparams}offset=${topicListRequest.offset-20}">следующие →</a>
      </td>
    </c:if>
    <c:if test="${topicListRequest.offset == 20}">
      <td width="35%" align="right">
        <c:if test="${params!=null}">
          <a href="${url}?${params}">следующие →</a>
        </c:if>
        <c:if test="${params==null}">
          <a href="${url}">следующие →</a>
        </c:if>
      </td>
    </c:if>
  </tr>
</table>

<script type="text/javascript">
  function tag_filter(url, event, newText, add) {
    event.preventDefault();

    var data = { tagName: "${topicListRequest.tag}"};

    if (add) {
      data['add'] = 'add';
    } else {
      data['del'] = 'del';
    }

    $.ajax({
      url: url,
      type: "POST",
      dataType: "json",
      data: data
    }).done(function(t) {
      if (t.error) {
        alert(t.error);
      } else {
        $(event.target).unbind("click");
        text = $(event.target).text();
        $(event.target).text(newText);
        $(event.target).bind("click", function(event) {
          tag_filter(url, event, text, !add);
        });
      }
    });
  }

  $(document).ready(function() {
    addFav = function(event) {
      <c:if test="${isShowFavoriteTagButton}">
        tag_filter("/user-filter/favorite-tag", event, "Удалить из избранного", true );
      </c:if>
      <c:if test="${not isShowFavoriteTagButton}">
        tag_filter("/user-filter/favorite-tag", event, "В избранные теги", false );
      </c:if>
    };

    addIgn = function(event) {
      <c:if test="${isShowIgnoreTagButton}">
        tag_filter("/user-filter/ignore-tag", event, "Не игнорировать тег", true);
      </c:if>
      <c:if test="${isShowUnIgnoreTagButton}">
        tag_filter("/user-filter/ignore-tag", event, "Игнорировать тег", false);
      </c:if>
    };

    $("#tagFavAdd").bind("click", addFav);
    $("#tagIgnAdd").bind("click", addIgn);
  });
</script>

<jsp:include page="/WEB-INF/jsp/footer.jsp"/>
