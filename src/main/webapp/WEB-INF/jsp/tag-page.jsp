<%@ page contentType="text/html; charset=utf-8"%>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib tagdir="/WEB-INF/tags" prefix="lor" %>
<%@ taglib prefix="fn" uri="http://java.sun.com/jsp/jstl/functions" %>
<%@ taglib prefix="l" uri="http://www.linux.org.ru" %>
<%--
  ~ Copyright 1998-2015 Linux.org.ru
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
<%--@elvariable id="tag" type="java.lang.String"--%>
<%--@elvariable id="title" type="java.lang.String"--%>
<%--@elvariable id="fullNews" type="java.util.List<ru.org.linux.topic.PersonalizedPreparedTopic>"--%>
<%--@elvariable id="addNews" type="java.lang.String"--%>
<%--@elvariable id="moreNews" type="java.lang.String"--%>
<%--@elvariable id="addGallery" type="java.lang.String"--%>
<%--@elvariable id="moreGallery" type="java.lang.String"--%>
<%--@elvariable id="gallery" type="java.util.List<ru.org.linux.gallery.PreparedGalleryItem>"--%>
<%--@elvariable id="briefNews" type="java.util.List<java.util.List<scala.Tuple2<java.lang.String, java.util.Collection<ru.org.linux.topic.BriefTopicRef>>>>"--%>
<%--@elvariable id="forum" type="java.util.List<java.util.List<scala.Tuple2<java.lang.String, java.util.Collection<ru.org.linux.topic.BriefTopicRef>>>>"--%>
<%--@elvariable id="showFavoriteTagButton" type="java.lang.Boolean"--%>
<%--@elvariable id="showUnFavoriteTagButton" type="java.lang.Boolean"--%>
<%--@elvariable id="showIgnoreTagButton" type="java.lang.Boolean"--%>
<%--@elvariable id="showUnIgnoreTagButton" type="java.lang.Boolean"--%>
<%--@elvariable id="favsCount" type="java.lang.Integer"--%>
<%--@elvariable id="counter" type="java.lang.Integer"--%>
<%--@elvariable id="relatedTags" type="java.util.List<java.lang.String>"--%>

<jsp:include page="/WEB-INF/jsp/head.jsp"/>
<title>${title}</title>

<jsp:include page="/WEB-INF/jsp/header.jsp"/>

<h1><i class="icon-tag"></i> ${title}</h1>

<div class="infoblock" style="font-size: medium">
    <div class="fav-buttons">
        <c:if test="${showFavoriteTagButton}">
            <c:url var="tagFavUrl" value="/user-filter">
                <c:param name="newFavoriteTagName" value="${tag}"/>
            </c:url>

            <a id="tagFavAdd" href="${tagFavUrl}" title="В избранное"><i class="icon-eye"></i></a>
        </c:if>
        <c:if test="${not template.sessionAuthorized}">
            <a id="tagFavNoth" href="#"><i class="icon-eye"  title="Добавить в избранное"></i></a>
        </c:if>
        <c:if test="${showUnFavoriteTagButton}">
            <c:url var="tagFavUrl" value="/user-filter"/>

            <a id="tagFavAdd" href="${tagFavUrl}" title="Удалить из избранного" class="selected"><i class="icon-eye"></i></a>
        </c:if>
        <br><span id="favsCount" title="Кол-во пользователей, добавивших в избранное">${favsCount}</span>

	<br>

        <c:if test="${showIgnoreTagButton}">
            <c:url var="tagIgnUrl" value="/user-filter">
                <c:param name="newIgnoreTagName" value="${tag}"/>
            </c:url>

            <a id="tagIgnore" href="${tagIgnUrl}" title="Игнорировать"><i class="icon-eye-with-line"></i></a>
        </c:if>
        <c:if test="${not template.sessionAuthorized}">
            <a id="tagIgnNoth" href="#"><i class="icon-eye-with-line"  title="Игнорировать"></i></a>
        </c:if>
        <c:if test="${showUnIgnoreTagButton}">
            <c:url var="tagIgnUrl" value="/user-filter"/>

            <a id="tagIgnore" href="${tagFavUrl}" title="Перестать игнорировать" class="selected"><i class="icon-eye-with-line"></i></a>
        </c:if>
        <br><span id="ignoreCount" title="Кол-во пользователей, игнорирующих тег">${ignoreCount}</span>
    </div>
    <p>
      Всего сообщений: ${counter}
    </p>
    <c:if test="${not empty relatedTags}">
      См. также: <l:tags list="${relatedTags}"/>
    </c:if>
</div>

<section>
    <c:forEach var="msg" items="${fullNews}">
        <lor:news
                preparedMessage="${msg.preparedTopic}"
                messageMenu="${msg.topicMenu}"
                multiPortal="false"
                minorAsMajor="true"
                moderateMode="false"/>
    </c:forEach>
</section>

<c:if test="${not empty briefNews}">
<section>
   <h2>Еще новости</h2>

  <div class="container" id="tag-page-news">
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
                <c:if test="${msg.commentCount>0}">(<lor:comment-count count="${msg.commentCount}"/>)</c:if>
              </li>
            </c:forEach>
          </ul>
        </c:forEach>
      </section>
    </c:forEach>
  </div>

  <div class="tag-page-buttons">
    <div>
        <a href="${addNews}" class="btn btn-primary">Добавить</a>
        <c:if test="${not empty moreNews}">
          <a href="${moreNews}" class="btn btn-default">Все новости</a>
        </c:if>
    </div>
  </div>
</section>
</c:if>

<c:if test="${not empty gallery}">
<section class="infoblock">
  <h2>Галерея</h2>

  <div id="tag-page-gallery">
    <c:forEach var="item" items="${gallery}">
      <article>
        <c:url var="url" value="${item.item.link}"/>
        <h3><a href="${url}"><l:title>${item.item.title}</l:title></a></h3>
        <a href="${url}">
          <img
                  src="${item.item.image.medium}"
                  srcset="${item.item.image.srcset}"
                  sizes="(min-width: 40em) 32vw, 100vw"
                  alt="Скриншот: <l:title>${item.item.title}</l:title>">
        </a><br>
        ${item.user.nick}, <lor:dateinterval date="${item.item.commitDate}"/>
      </article>
    </c:forEach>
  </div>

  <div class="tag-page-buttons">
    <div>
      <c:if test="${not empty addGallery}">
        <a href="${addGallery}" class="btn btn-primary">Добавить</a>
      </c:if>
      <c:if test="${not empty moreGallery}">
        <a href="${moreGallery}" class="btn btn-default">Все изображения</a>
      </c:if>
    </div>
  </div>
</section>
</c:if>

<c:if test="${not empty forum}">
  <section>
    <h2>Форум</h2>

    <div class="container" id="tag-page-forum">
      <c:forEach var="map" items="${forum}">
        <section>
          <c:forEach var="entry" items="${map}">
            <h3>${entry._1()}</h3>
            <ul>
              <c:forEach var="msg" items="${entry._2()}">
                <li>
                  <c:if test="${msg.group.defined}">
                    <span class="group-label">${msg.group.get()}</span>
                  </c:if>
                  <a href="${msg.url}">${msg.title}</a>
                  <c:if test="${msg.commentCount>0}">(<lor:comment-count count="${msg.commentCount}"/>)</c:if>
                </li>
              </c:forEach>
            </ul>
          </c:forEach>
        </section>
      </c:forEach>
    </div>

    <div class="tag-page-buttons">
      <div>
        <c:if test="${not empty addForum}">
          <a href="${addForum}" class="btn btn-primary">Добавить</a>
        </c:if>
        <c:if test="${not empty moreForum}">
          <a href="${moreForum}" class="btn btn-default">Все темы</a>
        </c:if>
      </div>
    </div>
  </section>
</c:if>

<script type="text/javascript">
  $script.ready('lorjs', function() {
    tag_memories_form_setup("${tag}", "${fn:escapeXml(csrfToken)}");
  });
</script>

<jsp:include page="/WEB-INF/jsp/footer.jsp"/>
