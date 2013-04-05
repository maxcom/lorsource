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
<%--@elvariable id="favsCount" type="java.lang.Integer"--%>
<%--@elvariable id="sectionList" type="java.util.List<ru.org.linux.section.Section>"--%>
<%--@elvariable id="tag" type="java.lang.String"--%>
<%--@elvariable id="offset" type="java.lang.Integer"--%>
<%--@elvariable id="section" type="java.lang.Integer"--%>
<jsp:include page="/WEB-INF/jsp/head.jsp"/>
<title>${ptitle}</title>

<jsp:include page="/WEB-INF/jsp/header.jsp"/>
  <div class=nav>
    <div id="navPath">
      <i class="icon-tag"></i> ${navtitle}
    </div>
    <div class="nav-buttons">
      <ul>
        <c:if test="${isShowIgnoreTagButton}">
          <li>
          <c:url var="tagIgnUrl" value="/user-filter">
            <c:param name="newIgnoredTagName" value="${tag}"/>
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

        <c:if test="${counter>10}">
          <li><a href="${url}" <c:if test="${section == 0}">class="current"</c:if>>Все</a></li>

          <c:forEach items="${sectionList}" var="cursection">
            <li>
              <a href="${url}?section=${cursection.id}"
                 <c:if test="${section == cursection.id}">class="current"</c:if>>${cursection.name}</a>
            </li>
          </c:forEach>
        </c:if>
      </ul>
    </div>
</div>

<div class="infoblock" style="font-size: medium">
  <div class="fav-buttons">
  <c:if test="${isShowFavoriteTagButton}">
      <c:url var="tagFavUrl" value="/user-filter">
        <c:param name="newFavoriteTagName" value="${tag}"/>
      </c:url>

      <a id="tagFavAdd" href="${tagFavUrl}" title="В избранное"><i class="icon-eye"></i></a>
  </c:if>
  <c:if test="${not template.sessionAuthorized}">
      <a id="tagFavNoth" href="#"><i class="icon-eye"  title="Добавить в избранное"></i></a>
  </c:if>
  <c:if test="${isShowUnFavoriteTagButton}">
      <c:url var="tagFavUrl" value="/user-filter"/>

      <a id="tagFavAdd" href="${tagFavUrl}" title="Удалить из избранного" class="selected"><i class="icon-eye"></i></a>
  </c:if>
    <br><span id="favsCount" title="Кол-во пользователей, добавивших в избранное">${favsCount}</span>
  </div>

  Всего сообщений: ${counter}
  <br><br>
</div>

<c:forEach var="msg" items="${messages}">
  <lor:news preparedMessage="${msg.preparedTopic}" messageMenu="${msg.topicMenu}" multiPortal="${section==0}" moderateMode="false"/>
</c:forEach>

<table class="nav">
  <tr>
    <c:if test="${not empty prevLink}">
      <td align="left" width="35%">
        <a href="${prevLink}">← назад</a>
      </td>
    </c:if>
    <c:if test="${not empty nextLink}">
      <td width="35%" align="right">
        <a href="${nextLink}">вперед →</a>
      </td>
    </c:if>
  </tr>
</table>
<c:if test="${not template.sessionAuthorized}">
<script type="text/javascript">
  $script.ready('plugins', function() {
    $(function() {
        $("#tagFavNoth").click(function(event) {
            event.preventDefault();
            event.stopPropagation();
            $("#tagFavNoth").popover('show');
        });
        $("#tagFavNoth").popover({
            content: "Для добавления в избранное надо залогиниться!"
        });
    });
  });
</script>
</c:if>

<c:if test="${template.sessionAuthorized}">
<script type="text/javascript">
  function tag_filter(event) {
    event.preventDefault();

    var data = { tagName: "${tag}"};

    var el = $('#tagFavAdd');
    var add = !el.hasClass("selected");

    if (add) {
      data['add'] = 'add';
    } else {
      data['del'] = 'del';
    }

    data['csrf'] = "${fn:escapeXml(csrfToken)}";

    $.ajax({
      url: "/user-filter/favorite-tag",
      type: "POST",
      dataType: "json",
      data: data
    }).done(function(t) {
      if (t.error) {
        alert(t.error);
      } else {
        el.attr('title', add?"Удалить из избранного":"В избранное");

        $('#favsCount').text(t['count']);

        if (add) {
          el.addClass("selected");
        } else {
          el.removeClass("selected");
        }
      }
    });
  }

  $(document).ready(function() {
    $("#tagFavAdd").bind("click", tag_filter);
  });
</script>
</c:if>
<jsp:include page="/WEB-INF/jsp/footer.jsp"/>
