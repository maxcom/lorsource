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
<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="lor" uri="http://www.linux.org.ru" %>
<%@ taglib prefix="l" uri="http://www.linux.org.ru" %>
<%@ taglib prefix="fn" uri="http://java.sun.com/jsp/jstl/functions"%>
<h2>Мининовости</h2>
<div class="boxlet_content">
    Последние мининовости:
    <ul>
        <c:forEach items="${topics}" var="topic">
            <li>
                <c:url value="${topic.url}" var="msg_link">
                    <c:if test="${topic.pages == 1}">
                        <c:param name="lastmod" value="${topic.lastmod.time}"/>
                    </c:if>
                </c:url>
                <a href="${fn:escapeXml(msg_link)}"><l:title>${topic.title}</l:title></a>
                <c:if test="${topic.pages gt 1}">
                    <c:url value="${topic.url}/page${topic.pages-1}" var="page_link">
                        <c:param name="lastmod" value="${topic.lastmod.time}"/>
                    </c:url>
                    (стр. <a href="${fn:escapeXml(page_link)}">${topic.pages}</a>)
                </c:if>
                (${topic.answers})
            </li>
        </c:forEach>
    </ul>
</div>
