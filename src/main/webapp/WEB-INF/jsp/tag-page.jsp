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
<%--@elvariable id="briefNews1" type="java.util.List<ru.org.linux.topic.Topic>"--%>
<%--@elvariable id="briefNews2" type="java.util.List<ru.org.linux.topic.Topic>"--%>
<%--@elvariable id="forum1" type="java.util.List<ru.org.linux.tag.TagPageController.ForumItem>"--%>
<%--@elvariable id="forum2" type="java.util.List<ru.org.linux.tag.TagPageController.ForumItem>"--%>

<jsp:include page="/WEB-INF/jsp/head.jsp"/>
<title>${tag}</title>

<jsp:include page="/WEB-INF/jsp/header.jsp"/>

<h1><i class="icon-tag"></i> ${tag}</h1>

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

<c:if test="${not empty briefNews1}">
<section>
   <h2>Еще новости</h2>

   <div class="container">
   <ul class="col-first-half">
       <c:forEach var="msg" items="${briefNews1}">
           <li><lor:dateinterval date="${msg.commitDate}"/>&emsp;<a href="${msg.link}"><c:out escapeXml="true" value="${msg.title}"/></a> </li>
       </c:forEach>
   </ul>
    <ul class="col-second-half">
        <c:forEach var="msg" items="${briefNews2}">
            <li><lor:dateinterval date="${msg.commitDate}"/>&emsp;<a href="${msg.link}"><c:out escapeXml="true" value="${msg.title}"/></a> </li>
        </c:forEach>
    </ul>
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

<c:if test="${not empty forum1}">
  <section>
    <h2>Форум</h2>

    <div class="container" id="tag-page-forum">
      <ul class="col-first-half">
        <c:forEach var="msg" items="${forum1}">
          <li>
              <lor:dateinterval date="${msg.topic.lastModified}"/>&emsp;<span class="group-label">${msg.group.title}</span>&emsp;<a href="${msg.topic.link}"><c:out escapeXml="true" value="${msg.topic.title}"/></a>
              <c:if test="${msg.topic.commentCount>0}">(${msg.topic.commentCount} комментариев)</c:if>
          </li>
        </c:forEach>
      </ul>
      <ul class="col-second-half">
        <c:forEach var="msg" items="${forum2}">
          <li>
              <lor:dateinterval date="${msg.topic.lastModified}"/>&emsp;<span class="group-label">${msg.group.title}</span>&emsp;<a href="${msg.topic.link}"><c:out escapeXml="true" value="${msg.topic.title}"/></a>
            <c:if test="${msg.topic.commentCount>0}">(${msg.topic.commentCount} комментариев)</c:if>
          </li>
        </c:forEach>
      </ul>
    </div>
  </section>
</c:if>

<jsp:include page="/WEB-INF/jsp/footer.jsp"/>
