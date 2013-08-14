<%@ page contentType="text/html; charset=utf-8"%>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib tagdir="/WEB-INF/tags" prefix="lor" %>
<%@ taglib prefix="fn" uri="http://java.sun.com/jsp/jstl/functions" %>
<%@ taglib prefix="form" uri="http://www.springframework.org/tags/form" %>
<%@ taglib prefix="l" uri="http://www.linux.org.ru" %>
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
<%--@elvariable id="tag" type="java.lang.String"--%>
<%--@elvariable id="fullNews" type="java.util.List<ru.org.linux.topic.PersonalizedPreparedTopic>"--%>
<%--@elvariable id="gallery" type="java.util.List<ru.org.linux.gallery.PreparedGalleryItem>"--%>
<%--@elvariable id="briefNews" type="java.util.List<java.util.Map<java.lang.String, java.util.Collection<ru.org.linux.topic.Topic>>>"--%>
<%--@elvariable id="forum" type="java.util.List<java.util.Map<java.lang.String, java.util.Collection<ru.org.linux.tag.TagPageController.ForumItem>>>"--%>
<%--@elvariable id="showFavoriteTagButton" type="java.lang.Boolean"--%>
<%--@elvariable id="showUnFavoriteTagButton" type="java.lang.Boolean"--%>
<%--@elvariable id="favsCount" type="java.lang.Integer"--%>

<jsp:include page="/WEB-INF/jsp/head.jsp"/>
<title>${tag}</title>

<jsp:include page="/WEB-INF/jsp/header.jsp"/>

<h1><i class="icon-tag"></i> ${tag}</h1>

<div class="container" style="font-size: medium">
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
    </div>
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
    <c:forEach var="map" items="${briefNews}">
      <section>
        <c:forEach var="entry" items="${map}">
          <h3>${entry.key}</h3>
          <ul>
            <c:forEach var="msg" items="${entry.value}">
              <li><a href="${msg.link}">${msg.title}</a> </li>
            </c:forEach>
          </ul>
        </c:forEach>
      </section>
    </c:forEach>
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
        <h3><a href="${url}">${item.item.title}</a></h3>
        <a href="${url}">
          <img src="${item.item.image.medium}" alt="Скриншот: <l:title>${item.item.title}</l:title>">
        </a><br>
        <lor:dateinterval date="${item.item.commitDate}"/>
      </article>
    </c:forEach>
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
            <h3>${entry.key}</h3>
            <ul>
              <c:forEach var="msg" items="${entry.value}">
                <li>
                  <span class="group-label">${msg.group.title}</span> <a href="${msg.topic.link}"><c:out
                        escapeXml="true" value="${msg.topic.title}"/></a>
                  <c:if test="${msg.topic.commentCount>0}">(${msg.topic.commentCount} комментариев)</c:if>
                </li>
              </c:forEach>
            </ul>
          </c:forEach>
        </section>
      </c:forEach>
    </div>
  </section>
</c:if>

<jsp:include page="/WEB-INF/jsp/footer.jsp"/>
