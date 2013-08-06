<%@ page contentType="text/html; charset=utf-8"%>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib tagdir="/WEB-INF/tags" prefix="lor" %>
<%@ taglib prefix="fn" uri="http://java.sun.com/jsp/jstl/functions" %>
<%@ taglib prefix="form" uri="http://www.springframework.org/tags/form" %>
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
<%--@elvariable id="briefNews1" type="java.util.List<ru.org.linux.topic.Topic>"--%>
<%--@elvariable id="briefNews2" type="java.util.List<ru.org.linux.topic.Topic>"--%>

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

<section class="infoblock">
   <h2>Еще новости</h2>
   <ul>
       <c:forEach var="msg" items="${briefNews1}">
           <li><lor:dateinterval date="${msg.postdate}"/>&emsp;<a href="${msg.link}"><c:out escapeXml="true" value="${msg.title}"/></a> </li>
       </c:forEach>
   </ul>
    <ul>
        <c:forEach var="msg" items="${briefNews2}">
            <li><lor:dateinterval date="${msg.postdate}"/>&emsp;<a href="${msg.link}"><c:out escapeXml="true" value="${msg.title}"/></a> </li>
        </c:forEach>
    </ul>
</section>

<jsp:include page="/WEB-INF/jsp/footer.jsp"/>
