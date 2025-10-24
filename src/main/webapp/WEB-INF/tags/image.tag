<%@ tag import="ru.org.linux.gallery.Image" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="l" uri="http://www.linux.org.ru" %>
<%@ tag pageEncoding="UTF-8"%>
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
<%@ attribute name="preparedMessage" required="false" type="ru.org.linux.topic.PreparedTopic" %>
<%@ attribute name="image" required="true" type="ru.org.linux.topic.PreparedImage" %>
<%@ attribute name="title" required="true" type="java.lang.String" %>
<%@ attribute name="showImage" required="false" type="java.lang.Boolean" %>
<%@ attribute name="enableEdit" required="false" type="java.lang.Boolean" %>
<%@ attribute name="sizes" required="false" type="java.lang.String" %>
<%@ attribute name="heightLimit" required="false" type="java.lang.String" %>
<%@ attribute name="enableSchema" required="false" type="java.lang.Boolean" %>
<c:set var="sizesValue" value="${(empty sizes) ? '100vw' : sizes}" />
<c:set var="heightLimitValue" value="${(empty heightLimit) ? '90vh' : heightLimit}" />

<c:if test="${showImage!=null and showImage and image!=null}">
  <div class="medium-image-container" style="max-width: <%= Math.min(image.getFullInfo().getWidth(), Image.MaxScaledSize()) %>px; max-height: ${heightLimitValue};
    width: min(var(--image-width), calc(${heightLimitValue} * ${image.mediumInfo.width} / ${image.mediumInfo.height}))">
  <figure class="medium-image" <%-- padding продублирован Pale Moon и других для браузеров, не умеющих min() --%>
    style="position: relative; padding-bottom: ${ 100.0 * image.mediumInfo.height / image.mediumInfo.width }%; padding-bottom: min(${ 100.0 * image.mediumInfo.height / image.mediumInfo.width }%, ${heightLimitValue}); margin: 0"
  <c:if test="${enableSchema}">itemprop="associatedMedia" itemscope itemtype="http://schema.org/ImageObject"</c:if>>
    <c:if test="${preparedMessage.section.imagepost || image.fullInfo.width >= 1920 || image.fullInfo.height >= 1080}">
      <a href="${image.fullName}" itemprop="contentURL">
    </c:if>
      <img
              itemprop="thumbnail"
              class="medium-image"
              src="${image.mediumName}"
              alt="<l:title>${title}</l:title>"
              srcset="${image.srcset}"
              sizes="${sizesValue}" style="position: absolute; max-height: ${heightLimitValue}"
              ${image.loadingCode}
              ${image.mediumInfo.code}>
      <meta itemprop="caption" content="${preparedMessage.message.title}">
    <c:if test="${preparedMessage.section.imagepost || image.fullInfo.width >= 1920 || image.fullInfo.height >= 1080}">
      </a>
    </c:if>
  </figure>
    <c:if test="${enableEdit}">
      <div>
        <a href="/delete_image?id=${image.image.id}">удалить изображение</a>
      </div>
    </c:if>
  </div>
</c:if>