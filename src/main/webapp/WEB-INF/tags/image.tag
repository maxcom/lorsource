<%@ tag import="ru.org.linux.gallery.Image" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="l" uri="http://www.linux.org.ru" %>
<%@ tag pageEncoding="UTF-8"%>
<%--
  ~ Copyright 1998-2018 Linux.org.ru
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
<%@ attribute name="showInfo" required="false" type="java.lang.Boolean" %>
<%@ attribute name="enableEdit" required="false" type="java.lang.Boolean" %>
<%@ attribute name="enableSchema" required="false" type="java.lang.Boolean" %>
<c:if test="${showImage!=null and showImage and image!=null}">
  <div class="medium-image-container" style="max-width: <%= Math.min(image.getFullInfo().getWidth(), Image.MaxScaledSize()) %>px">
  <figure class="medium-image"
    style="position: relative; padding-bottom: ${ 100.0 * image.mediumInfo.height / image.mediumInfo.width }%; margin: 0"
  <c:if test="${enableSchema}">itemprop="associatedMedia" itemscope itemtype="http://schema.org/ImageObject"</c:if>>
    <a href="${image.fullName}" itemprop="contentURL">
      <img
              itemprop="thumbnail"
              class="medium-image"
              src="${image.mediumName}"
              alt="<l:title>${title}</l:title>"
              srcset="${image.image.srcset}"
              sizes="500px" style="position: absolute"
              ${image.mediumInfo.code}>
      <meta itemprop="caption" content="${preparedMessage.message.title}">
    </a>
  </figure>
    <c:if test="${enableEdit && not preparedMessage.section.imagepost}">
      <div>
        <a href="/delete_image?id=${image.image.id}">удалить изображение</a>
      </div>
    </c:if>
  </div>
</c:if>

<c:if test="${showInfo!=null and showInfo and preparedMessage!=null and preparedMessage.section.imagepost}">
  <c:if test="${image != null}">
    <p>
      &gt;&gt;&gt; <a href="${image.fullName}">Просмотр</a>
      (<i>${image.fullInfo.width}x${image.fullInfo.height},
        ${image.fullInfo.sizeString}</i>)
    </p>
  </c:if>
  <c:if test="${image == null}">
    (BAD IMAGE)
  </c:if>
</c:if>