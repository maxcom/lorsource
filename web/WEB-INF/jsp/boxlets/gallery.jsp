<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<h2><a href="view-news.jsp?section=3">Галерея</a></h2>

<div class="boxlet_content">
  <h3>Последние скриншоты</h3>
  <c:forEach var="item" items="{$items}">
    <div align="center">
      <c:url var="url" value="/view-message.jsp">
        <c:param name="msgid" value="${item.msgid}"/>
      </c:url>
      <a href="${url}">
        <c:choose>
          <c:when test="${not empty item.info}">
            <img src="${item.icon}" alt="Скриншот: ${item.title}" ${item.info.code}/>
          </c:when>
          <c:otherwise>
            [bad image] <img src="${item.icon}" alt="Скриншот: $item.title"/>
          </c:otherwise>
        </c:choose>
      </a>
    </div>
    <i>
      <c:choose>
        <c:when test="${not empty item.imginfo}">
           ${item.imginfo.width}x${item.imginfo.height}
        </c:when>
        <c:otherwise>
          [bad image]
        </c:otherwise>
      </c:choose>
      <c:url value="/whois.jsp" var="nickurl">
        <c:param name="nick" value="${item.nick}"/>
      </c:url>
    </i> ${item.title} от <a href="${nickurl}">${item.nick}</a> (${item.stat})
  </c:forEach>
  
  <a href="view-news.jsp?section=3">другие скриншоты...</a>
</div>