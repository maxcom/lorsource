<%@ page import="org.apache.commons.lang.math.RandomUtils" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ page contentType="text/html;charset=UTF-8" language="java" %>

<c:set var="rand" value="<%= RandomUtils.nextInt(6) %>"/>

<c:choose>
  <c:when test="${rand == 0}">
    <a rel="nofollow" href="http://selectel.ru/about/promo/dedicatedisp100mbit/?utm_source=lor&utm_medium=banner&utm_content=5504&utm_campaign=sale"><img src="/adv/selectel-xeon.png" width="728" height="90" border="0"></a>
  </c:when>

  <c:when test="${rand == 1}">
    <a rel="nofollow" href="http://selectel.ru/about/promo/core2quad-sale/?utm_source=lor&utm_medium=banner&utm_content=quad50&utm_campaign=sale"><img src="/adv/selectel-quad.png" width="728" height="90" border="0"></a>
  </c:when>

  <c:when test="${rand == 2}">
    <a rel="nofollow" href="http://selectel.ru/services/dedicated/?utm_source=lor&utm_medium=banner&utm_content=dedic1&utm_campaign=service"><img src="/adv/selectel-dedic1.png" width="728" height="90" border="0"></a>
  </c:when>

  <c:when test="${rand == 3}">
    <a rel="nofollow" href="http://selectel.ru/services/dedicated/?utm_source=lor&utm_medium=banner&utm_content=dedic2&utm_campaign=service"><img src="/adv/selectel-dedic2.png" width="728" height="90" border="0"></a>
  </c:when>

  <c:when test="${rand == 4}">
    <a rel="nofollow" href="http://selectel.ru/services/cloud/?utm_source=lor&utm_medium=banner&utm_content=cloud&utm_campaign=service"><img src="/adv/selectel-cloud.png" width="728" height="90" border="0"></a>
  </c:when>

  <c:when test="${rand == 5}">
    <a rel="nofollow" href="http://selectel.ru/services/cloud-storage/?utm_source=lor&utm_medium=banner&utm_content=cloud-storage&utm_campaign=service"><img src="/adv/selectel-storage.png" width="728" height="90" border="0"></a>
  </c:when>

</c:choose>
