<%@ page contentType="text/html; charset=utf-8"%>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib prefix="fn" uri="http://java.sun.com/jsp/jstl/functions" %>
<%@ taglib tagdir="/WEB-INF/tags" prefix="lor" %>
<%@ taglib prefix="l" uri="http://www.linux.org.ru" %>
<%@ taglib prefix="spring" uri="http://www.springframework.org/tags" %>
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
<%--@elvariable id="topicsList" type="java.util.List<ru.org.linux.group.TopicsListItem>"--%>
<%--@elvariable id="group" type="ru.org.linux.group.Group"--%>
<%--@elvariable id="section" type="ru.org.linux.section.Section"--%>
<%--@elvariable id="firstPage" type="java.lang.Boolean"--%>
<%--@elvariable id="groupList" type="java.util.List<ru.org.linux.group.Group>"--%>
<%--@elvariable id="lastmod" type="java.lang.Boolean"--%>
<%--@elvariable id="addable" type="java.lang.Boolean"--%>
<%--@elvariable id="offset" type="java.lang.Integer"--%>
<%--@elvariable id="showDeleted" type="java.lang.Boolean"--%>
<%--@elvariable id="template" type="ru.org.linux.site.Template"--%>
<%--@elvariable id="year" type="java.lang.Integer"--%>
<%--@elvariable id="month" type="java.lang.Integer"--%>
<%--@elvariable id="url" type="java.lang.String"--%>
<%--@elvariable id="groupInfo" type="ru.org.linux.group.PreparedGroupInfo"--%>
<%--@elvariable id="hasNext" type="java.lang.Boolean"--%>
<jsp:include page="/WEB-INF/jsp/head.jsp"/>
<script type="text/javascript">
  <!--
  function goto(mySelect) {
      window.location.href = mySelect.options[mySelect.selectedIndex].value;
  }
  -->
</script>

<title>${section.name} — ${group.title}
  <c:if test="${year != null}">
    — Архив ${year}, ${l:getMonthName(month)}
  </c:if>

  <c:if test="${tag != null}">
    (тег ${tag.name})
  </c:if>
</title>
    <link rel="alternate" href="/section-rss.jsp?section=${group.sectionId}&amp;group=${group.id}" type="application/rss+xml">
<jsp:include page="/WEB-INF/jsp/header.jsp"/>
<form>
  <div class=nav>
    <div id="navPath">
      <c:if test="${tag == null}">
        ${section.name} «${group.title}»
      </c:if>

      <c:if test="${tag != null}">
        ${section.name}&nbsp;<a href="${group.url}">«${group.title}»</a>
          (тег <a href="${tag.url.get()}">${tagTitle}</a>)
      </c:if>

      <c:if test="${year != null}">
        — Архив ${year}, ${l:getMonthName(month)}
      </c:if>
    </div>

    <div class="nav-buttons">
      <select name=group onchange="goto(this);" title="Быстрый переход">
        <c:forEach items="${groupList}" var="item">
          <c:if test="${item.id == group.id}">
            <c:if test="${lastmod}">
              <option value="${item.url}?lastmod=true" selected>${item.title}</option>
            </c:if>
            <c:if test="${not lastmod}">
              <option value="${item.url}" selected>${item.title}</option>
            </c:if>
          </c:if>
          <c:if test="${item.id != group.id}">
            <c:if test="${lastmod}">
              <option value="${item.url}?lastmod=true">${item.title}</option>
            </c:if>
            <c:if test="${not lastmod}">
              <option value="${item.url}">${item.title}</option>
            </c:if>
          </c:if>
        </c:forEach>
      </select>
     </div>
 </div>

</form>

<nav>
  <spring:url value="${group.url}" var="newUrl">
    <c:if test="${tag!=null}">
      <spring:param name="tag" value="${tag.name}"/>
    </c:if>
  </spring:url>

  <spring:url value="${group.url}" var="activeUrl">
    <spring:param name="lastmod" value="true"/>
    <c:if test="${tag!=null}">
      <spring:param name="tag" value="${tag.name}"/>
    </c:if>
  </spring:url>

  <spring:url value="add.jsp" var="addUrl">
    <spring:param name="group" value="${group.id}"/>
    <c:if test="${tag!=null}">
      <spring:param name="tags" value="${tag.name}"/>
    </c:if>
  </spring:url>

  <c:if test="${year!=null}">
    <a class="btn btn-default" href="${newUrl}">Новые</a>
    <a class="btn btn-default" href="${activeUrl}">Активные</a>
    <a href="${group.url}archive/" class="btn btn-selected">Архив</a>
  </c:if>
  <c:if test="${year==null}">
    <c:if test="${!lastmod}">
      <a class="btn btn-selected" href="${newUrl}">Новые</a>
      <a class="btn btn-default" href="${activeUrl}">Активные</a>
    </c:if>
    <c:if test="${lastmod}">
      <a class="btn btn-default" href="${newUrl}">Новые</a>
      <a class="btn btn-selected" href="${activeUrl}">Активные</a>
    </c:if>
    <a href="${group.url}archive/" class="btn btn-default">Архив</a>
    <c:if test="${template.moderatorSession}">
      <a href="groupmod.jsp?group=${group.id}" class="btn btn-default">Править</a>
    </c:if>
    <c:if test="${addable}">
      <a href="${addUrl}" class="btn btn-primary">Добавить</a>
    </c:if>
  </c:if>
</nav>

<c:if test="${!empty groupImagePath}">
    <div align=center>
        <img src="${groupImagePath}" ${groupImageInfo.code} alt="Группа ${group.title}" />
    </div>
</c:if>
<c:if test="${year == null && offset==0}">
  <lor:groupinfo group="${groupInfo}" activeTags="${activeTags}"/>
</c:if>
<div class=forum>
<table class="message-table">
<thead>
<tr>
  <th>Тема
  </th>
  <th>Последнее сообщение<br>
    <c:if test="${year==null}">
    <c:if test="${lastmod}">
      <span style="font-weight: normal">[<a href="${url}" style="text-decoration: underline">отменить</a>]</span>
    </c:if>
    <c:if test="${not lastmod}">
      <span style="font-weight: normal">[<a href="${url}?lastmod=true" style="text-decoration: underline">упорядочить</a>]</span>
    </c:if>
    </c:if>
  </th>
  <th><i class="icon-comment"></i></th>
</tr>
</thead>
<tbody>

<c:forEach var="topic" items="${topicsList}">

<tr>
  <td>
    <c:if test="${topic.deleted}">
      <c:choose>
        <c:when test="${template.moderatorSession}">
          <a href="/undelete?msgid=${topic.topicId}"><img src="/img/del.png" alt="[X]" width="15" height="15"></a>
        </c:when>
        <c:otherwise>
          <img src="/img/del.png" alt="[X]" width="15" height="15">
        </c:otherwise>
      </c:choose>
    </c:if>
    <c:if test="${topic.sticky and not topic.deleted}">
      <i class="icon-pin icon-pin-color" title="Прикрепленная тема"></i>
    </c:if>
    <c:if test="${topic.resolved}">
      <img src="/img/solved.png" width="15" height="15" alt="Решено" title="Решено"/>
    </c:if>

    <c:set var="topic_tags">
      <c:forEach var="tag" items="${topic.tags}">
        <span class="tag">${tag}</span>
      </c:forEach>
    </c:set>

    <c:if test="${firstPage}">
        <a href="${topic.firstPageUrl}">
          ${topic_tags}<c:out value=" "/><l:title>${topic.title}</l:title>
        </a>
    </c:if>

    <c:if test="${not firstPage}">
      <a href="${topic.canonicalUrl}">
          ${topic_tags}<c:out value=" "/><l:title>${topic.title}</l:title>
      </a>
    </c:if>

    <c:if test="${topic.pages>1}">
      (стр.
      <c:forEach var="i" begin="1" end="${topic.pages-1}"><c:out value=" "/><c:if test="${i==(topic.pages-1) and firstPage and year==null}"><a href="${group.url}${topic.topicId}/page${i}?lastmod=${topic.lastCommentId}">${i+1}</a></c:if><c:if test="${i!=(topic.pages-1) or not firstPage or year!=null}"><a href="${group.url}${topic.topicId}/page${i}">${i+1}</a></c:if></c:forEach>)
    </c:if>
    (<lor:user user="${topic.topicAuthor}"/>)
  </td>

  <td class="dateinterval">
    <lor:dateinterval date="${topic.postdate}"/>, <lor:user user="${topic.author}"/>
  </td>

  <td class=numbers>
      <c:if test="${topic.commentCount==0}">-</c:if><c:if test="${topic.commentCount>0}">${topic.commentCount}</c:if>
  </td>
</tr>
</c:forEach>
</tbody>
<tfoot>
  <tr>
    <td colspan="3">
      <form action="${url}" method="GET" style="font-weight: normal; display: inline;">
        <c:if test="${lastmod}">
          <input type="hidden" name="lastmod" value="true">
        </c:if>
        <label>фильтр:
          <select name="showignored" onchange="submit();">
            <option value="t" <c:if test="${showIgnored}">selected</c:if> >все темы</option>
            <option value="f" <c:if test="${not showIgnored}">selected</c:if> >без игнорируемых</option>
            </select> [<a style="text-decoration: underline" href="<c:url value="/user-filter"/>">настроить</a>]
        </label>
      </form>
    </td>
  </tr>
</tfoot>
</table>

<c:if test="${not showDeleted}">
<div class="container" style="margin-bottom: 1em">
<div style="float: left">
  <c:if test="${prevPage>=0}">
    <spring:url value="${url}" var="prevUrl">
      <c:if test="${lastmod}">
        <spring:param name="lastmod" value="true"/>
      </c:if>
      <c:if test="${showIgnored}">
        <spring:param name="showignored" value="t"/>
      </c:if>
      <c:if test="${prevPage > 0}">
        <spring:param name="offset" value="${prevPage}"/>
      </c:if>
      <c:if test="${tag!=null}">
        <spring:param name="tag" value="${tag.name}"/>
      </c:if>
    </spring:url>

    <a rel="prev" href="${prevUrl}">← предыдущие</a>
  </c:if>
</div>
<div style="float: right">
  <c:if test="${hasNext}">
    <spring:url value="${url}" var="nextUrl">
      <c:if test="${lastmod}">
        <spring:param name="lastmod" value="true"/>
      </c:if>
      <c:if test="${showIgnored}">
        <spring:param name="showignored" value="t"/>
      </c:if>
      <c:if test="${tag!=null}">
        <spring:param name="tag" value="${tag.name}"/>
      </c:if>

      <spring:param name="offset" value="${nextPage}"/>
    </spring:url>

    <a rel="next" href="${nextUrl}">следующие →</a>
  </c:if>
  <c:if test="${not hasNext}">
    <a href="${group.url}archive/">архив</a>
  </c:if>
</div>
</div>
</c:if>

</div>

<c:if test="${not lastmod and not showDeleted and year==null and template.sessionAuthorized}">
  <hr>
  <form action="${url}" method=POST>
    <lor:csrf/>
    <input type=hidden name=showDeleted value=true>
    <input type=submit value="Показать удаленные сообщения">
  </form>
  <hr>
</c:if>
<c:if test="${not lastmod and showDeleted and year==null and template.sessionAuthorized and hasNext}">
    <hr>
    <form action="${url}" method=POST>
        <lor:csrf/>
      <input type=hidden name=showDeleted value=true>
        <input type=hidden name=offset value="${nextPage}">
        <input type=submit value="Показать еще удаленные">
    </form>
    <hr>
</c:if>

<p>
  <i class="icon-rss"></i>
  <a href="section-rss.jsp?section=${group.sectionId}&amp;group=${group.id}">
    RSS-подписка на новые темы
  </a>
</p>

<jsp:include page="/WEB-INF/jsp/footer.jsp"/>
