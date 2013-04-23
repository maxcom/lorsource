<%@ page contentType="text/html; charset=utf-8"%>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib prefix="fn" uri="http://java.sun.com/jsp/jstl/functions" %>
<%@ taglib tagdir="/WEB-INF/tags" prefix="lor" %>
<%@ taglib prefix="l" uri="http://www.linux.org.ru" %>
<%@ taglib prefix="spring" uri="http://www.springframework.org/tags" %>
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

<title>${section.name} - ${group.title}
  <c:if test="${year != null}">
    - Архив ${year}, ${l:getMonthName(month)}
  </c:if>
</title>
    <LINK REL="alternate" HREF="/section-rss.jsp?section=${group.sectionId}&amp;group=${group.id}" TYPE="application/rss+xml">
<jsp:include page="/WEB-INF/jsp/header.jsp"/>
<form>
  <div class=nav>
    <div id="navPath">
      <a href="${group.sectionLink}">${section.name}</a> - ${group.title}
      <c:if test="${year != null}">
        - Архив ${year}, ${l:getMonthName(month)}
      </c:if>
    </div>

    <div class="nav-buttons">
      <ul>
      <li><a href="${group.url}archive/">Архив</a></li>
      <c:if test="${year==null}">
        <c:if test="${template.moderatorSession}">
          <li><a href="groupmod.jsp?group=${group.id}">Править группу</a></li>
        </c:if>
        <c:if test="${addable}">
          <li><a href="add.jsp?group=${group.id}">Добавить сообщение</a></li>
        </c:if>
        <li><a href="section-rss.jsp?section=${group.sectionId}&amp;group=${group.id}">RSS</a></li>
  </ul>
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
      </c:if>
     </div>
 </div>

</form>

<c:if test="${!empty groupImagePath}">
    <div align=center>
        <img src="${groupImagePath}" ${groupImageInfo.code} alt="Группа ${group.title}" />
    </div>
</c:if>
<c:if test="${year == null && offset==0}">
  <lor:groupinfo group="${groupInfo}"/>
</c:if>
<div class=forum>
<table class="message-table">
<thead>
<tr>
  <th>Тема<br>
    <form action="${url}" method="GET" style="font-weight: normal; display: inline;">
      фильтр:
      <c:if test="${lastmod}">
        <input type="hidden" name="lastmod" value="true">
      </c:if>
        <select name="showignored" onchange="submit();">
          <option value="t" <c:if test="${showIgnored}">selected</c:if> >все темы</option>
          <option value="f" <c:if test="${not showIgnored}">selected</c:if> >без игнорируемых</option>
          </select> [<a style="text-decoration: underline" href="<c:url value="/user-filter"/>">настроить</a>]
    </form>
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
  <th>Число ответов<br>всего/день/час</th>
</tr>
</thead>
<tbody>

<c:forEach var="topic" items="${topicsList}">

<tr>
  <td>
    <c:if test="${topic.deleted}">
      <c:choose>
        <c:when test="${template.moderatorSession}">
          <a href="/undelete.jsp?msgid=${topic.msgid}"><img src="/img/del.png" alt="[X]" width="15" height="15"></a>
        </c:when>
        <c:otherwise>
          <img src="/img/del.png" alt="[X]" width="15" height="15">
        </c:otherwise>
      </c:choose>
    </c:if>
    <c:if test="${topic.sticky and not topic.deleted}">
      <img src="/img/paper_clip.gif" width="15" height="15" alt="Прикреплено" title="Прикреплено">
    </c:if>
    <c:if test="${topic.resolved}">
      <img src="/img/solved.png" width="15" height="15" alt="Решено" title="Решено"/>
    </c:if>

    <c:set var="topic_tags">
      <c:forEach var="tag" items="${topic.tags}">
        <span class="tag">${tag}</span>
      </c:forEach>
    </c:set>

    <c:if test="${firstPage and topic.pages<=1}">
        <a href="${group.url}${topic.msgid}?lastmod=${topic.lastmod.time}">
          ${topic_tags}<c:out value=" "/><l:title>${topic.subj}</l:title>
        </a>
    </c:if>

    <c:if test="${not firstPage or topic.pages>1}">
      <a href="${group.url}${topic.msgid}">
          ${topic_tags}<c:out value=" "/><l:title>${topic.subj}</l:title>
      </a>
    </c:if>

    <c:if test="${topic.pages>1}">
      (стр.
      <c:forEach var="i" begin="1" end="${topic.pages-1}"><c:out value=" "/><c:if test="${i==(topic.pages-1) and firstPage and year==null}"><a href="${group.url}${topic.msgid}/page${i}?lastmod=${topic.lastmod.time}">${i+1}</a></c:if><c:if test="${i!=(topic.pages-1) or not firstPage or year!=null}"><a href="${group.url}${topic.msgid}/page${i}">${i+1}</a></c:if></c:forEach>)
    </c:if>
    (<lor:user user="${topic.author}"/>)
  </td>

  <td class="dateinterval">
    <lor:dateinterval date="${topic.lastmod}"/>
  </td>

  <td class=numbers>
      <c:if test="${topic.stat1==0}">-</c:if><c:if test="${topic.stat1>0}"><b>${topic.stat1}</b></c:if>/<c:if test="${topic.stat3==0}">-</c:if><c:if test="${topic.stat3>0}"><b>${topic.stat3}</b></c:if>/<c:if test="${topic.stat4==0}">-</c:if><c:if test="${topic.stat4>0}"><b>${topic.stat4}</b></c:if> 
  </td> </tr>
</c:forEach>
</tbody>
<tfoot>
<tr><td colspan=3><p>
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
  </spring:url>

  <a rel="prev" href="${prevUrl}">← назад</a>
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

      <spring:param name="offset" value="${nextPage}"/>
    </spring:url>

    <a rel="next" href="${nextUrl}">вперед →</a>
  </c:if>
  <c:if test="${not hasNext}">
    <a href="${group.url}archive/">архив</a>
  </c:if>
</div>
</tfoot>
</table>
</div>
<c:if test="${not lastmod and not showDeleted and year==null and template.sessionAuthorized}">
  <hr>
  <form action="${url}" method=POST>
    <lor:csrf/>
    <input type=hidden name=deleted value=1>
    <input type=submit value="Показать удаленные сообщения">
  </form>
  <hr>
</c:if>
<jsp:include page="/WEB-INF/jsp/footer.jsp"/>
