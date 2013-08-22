<%@ page import="ru.org.linux.tag.TagCloudDao" %>
<%@ page import="ru.org.linux.topic.TagTopicListController" %>
<%--
  ~ Copyright 1998-2013 Linux.org.ru
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
<%@ page contentType="text/html;charset=UTF-8" language="java" trimDirectiveWhitespaces="true" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="lor" uri="http://www.linux.org.ru" %>
<%@ taglib prefix="fn" uri="http://java.sun.com/jsp/jstl/functions"%>

  <h2>Облако Меток</h2>

  <div class="boxlet_content">
    <div align="center">
      <c:forEach var="tag" items="${tags}">
        <%
          TagCloudDao.TagDTO tag = (TagCloudDao.TagDTO) pageContext.getAttribute("tag");
        %>
        <c:url value="<%= TagTopicListController.tagListUrl(tag.getValue()) %>" var="tag_url"/>
        <a class="cloud${tag.weight}" href="${fn:escapeXml(tag_url)}">${tag.value}</a>
      </c:forEach>
    </div>
    <p>
      <a href="/tags/">все метки...</a>
    </p>
  </div>