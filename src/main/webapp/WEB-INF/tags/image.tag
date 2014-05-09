<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="l" uri="http://www.linux.org.ru" %>
<%@ tag pageEncoding="UTF-8"%>
<%--
  ~ Copyright 1998-2014 Linux.org.ru
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
<%@ attribute name="preparedMessage" required="true" type="ru.org.linux.topic.PreparedTopic" %>
<%@ attribute name="showImage" required="false" type="java.lang.Boolean" %>
<%@ attribute name="showInfo" required="false" type="java.lang.Boolean" %>
<%@ attribute name="enableEdit" required="false" type="java.lang.Boolean" %>
<%@ attribute name="enableSchema" required="false" type="java.lang.Boolean" %>
<c:if test="${showImage!=null and showImage and preparedMessage.image!=null}">
  <figure class="medium-image" <c:if test="${enableSchema}">itemprop="associatedMedia" itemscope itemtype="http://schema.org/ImageObject"</c:if>>
    <a href="${preparedMessage.image.fullName}"itemprop="contentURL">
      <img itemprop="thumbnail" class="medium-image" src="${preparedMessage.image.mediumName}" alt="<l:title>${preparedMessage.message.title}</l:title>" ${preparedMessage.image.mediumInfo.code}>
      <meta itemprop="caption" content="${preparedMessage.message.title}">

      <c:if test="${enableEdit && not preparedMessage.section.imagepost}">
        <div>
          <a href="/delete_image?id=${preparedMessage.image.image.id}">удалить изображение</a>
        </div>
      </c:if>
    </a>
  </figure>
</c:if>

<c:if test="${showInfo!=null and showInfo}">
  <c:if test="${preparedMessage.image != null}">
    <p>
      &gt;&gt;&gt; <a href="${preparedMessage.image.fullName}">Просмотр</a>
      (<i>${preparedMessage.image.fullInfo.width}x${preparedMessage.image.fullInfo.height},
        ${preparedMessage.image.fullInfo.sizeString}</i>)
    </p>
  </c:if>
  <c:if test="${preparedMessage.image == null}">
    (BAD IMAGE)
  </c:if>
</c:if>