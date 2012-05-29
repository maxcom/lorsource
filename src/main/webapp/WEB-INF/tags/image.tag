<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="l" uri="http://www.linux.org.ru" %>
<%@ tag pageEncoding="UTF-8"%>
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
<%@ attribute name="preparedImage" required="true" type="ru.org.linux.topic.PreparedImage" %>
<%@ attribute name="topic" required="true" type="ru.org.linux.topic.Topic" %>
<%@ attribute name="showImage" required="false" type="java.lang.Boolean" %>
<%@ attribute name="showInfo" required="false" type="java.lang.Boolean" %>
<%@ attribute name="enableSchema" required="false" type="java.lang.Boolean" %>
<c:if test="${showImage!=null and showImage and preparedImage!=null}">
  <p <c:if test="${enableSchema}">itemprop="associatedMedia" itemscope itemtype="http://schema.org/ImageObject"</c:if>>
    <a href="${preparedImage.fullName}"itemprop="contentURL">
      <img itemprop="thumbnail" class="medium-image" src="${preparedImage.mediumName}" alt="<l:title>${topic.title}</l:title>" ${preparedImage.mediumInfo.code}>
      <meta itemprop="caption" content="${topic.title}">
    </a>
  </p>
</c:if>

<c:if test="${showInfo!=null and showInfo}">
  <c:if test="${preparedImage != null}">
    <p>
      &gt;&gt;&gt; <a href="${preparedImage.fullName}">Просмотр</a>
      (<i>${preparedImage.fullInfo.width}x${preparedImage.fullInfo.height},
        ${preparedImage.fullInfo.sizeString}</i>)
    </p>
  </c:if>
  <c:if test="${preparedImage == null}">
    (BAD IMAGE)
  </c:if>
</c:if>

