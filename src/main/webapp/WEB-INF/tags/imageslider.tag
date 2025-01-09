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
<%@ tag import="ru.org.linux.gallery.Image" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="l" uri="http://www.linux.org.ru" %>
<%@ tag pageEncoding="UTF-8"%>
<%@ attribute name="main" required="true" type="ru.org.linux.topic.PreparedImage" %>
<%@ attribute name="title" required="true" type="java.lang.String" %>
<%@ attribute name="classes" required="false" type="java.lang.String" %>
<%@ attribute name="additional" required="true" type="java.util.List<ru.org.linux.topic.PreparedImage>" %>
<%@ attribute name="heightLimit" required="false" type="java.lang.String" %>
<c:set var="heightLimitValue" value="${(empty heightLimit) ? '90vh' : heightLimit}" />
<div class="slider-parent" style="width: min(var(--image-width), calc(${heightLimitValue} * ${main.mediumInfo.width} / ${main.mediumInfo.height}))">
  <div class="swiffy-slider slider-indicators-round ${classes} slider-item-ratio slider-item-ratio-contain"
       style="--swiffy-slider-item-ratio: ${main.fullInfo.width}/${main.fullInfo.height}">
    <div class="slider-container">
      <a href="${main.fullName}">
        <img
          src="${main.mediumName}"
          alt="<l:title>${title}</l:title>"
          srcset="${main.srcset}"
          sizes="${sizes}"
          style="max-width: 100%; height: auto"
          ${main.loadingCode}
          ${main.mediumInfo.code}>
      </a>

      <c:forEach var="image" items="${additional}">
        <a href="${image.fullName}">
          <img
            src="${image.mediumName}"
            alt="<l:title>${title}</l:title>"
            srcset="${image.srcset}"
            sizes="${sizes}"
            style="max-width: 100%; height: auto; max-height: 100%; top: 50%; transform: translateY(-50%)"
            ${image.loadingCode}
            ${image.mediumInfo.code}>
        </a>
      </c:forEach>
    </div>

    <button type="button" class="slider-nav"></button>
    <button type="button" class="slider-nav slider-nav-next"></button>

    <div class="slider-indicators">
      <a href="${main.fullName}" class="active"></a>
      <c:forEach var="image" items="${additional}">
        <a href="${image.fullName}"></a>
      </c:forEach>
    </div>
  </div>
</div>