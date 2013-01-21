<%@ tag pageEncoding="UTF-8" trimDirectiveWhitespaces="true"%>
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
<%@ attribute name="preparedPagination" required="true" type="ru.org.linux.util.paginator.PreparedPagination" %>
<%@ attribute name="baseTemplate" required="true" type="java.lang.String" %>
<%@ attribute name="pageTemplate" required="true" type="java.lang.String" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="spring" uri="http://www.springframework.org/tags" %>
<%@ taglib prefix="l" uri="http://www.linux.org.ru" %>

<c:if test="${preparedPagination != null}">
    <c:choose>
        <c:when test="${preparedPagination.firstPage}">
            <span class='page-number'>←</span>
        </c:when>
        <c:otherwise>
            <a class='page-number' href='<l:page preparedPagination="${preparedPagination}" pageTemplate="${pageTemplate}" baseTemplate="${baseTemplate}" page="${preparedPagination.index - 1}"/>'>←</a>
        </c:otherwise>
    </c:choose>

    <c:forEach begin="1" end="${preparedPagination.count}" var="index">
        <c:choose>
            <c:when test="${index == preparedPagination.index}">
                <strong class='page-number'>${index}</strong>
            </c:when>
            <c:otherwise>
                <a class='page-number' href='<l:page preparedPagination="${preparedPagination}" pageTemplate="${pageTemplate}" baseTemplate="${baseTemplate}" page="${index}"/>'>${index}</a>
            </c:otherwise>
        </c:choose>
    </c:forEach>

    <c:choose>
        <c:when test="${preparedPagination.lastPage}">
            <span class='page-number'>→</span>
        </c:when>
        <c:otherwise>
            <a class='page-number' href='<l:page preparedPagination="${preparedPagination}" pageTemplate="${pageTemplate}" baseTemplate="${baseTemplate}" page="${preparedPagination.index + 1}"/>'>→</a>
        </c:otherwise>
    </c:choose>
</c:if>



