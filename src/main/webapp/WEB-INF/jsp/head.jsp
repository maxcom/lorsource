<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="sec" uri="http://www.springframework.org/security/tags" %>
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

<!DOCTYPE html>
<html lang=ru>
<head>
<LINK REL="STYLESHEET" TYPE="text/css" HREF="/common.css">
<LINK REL="STYLESHEET" TYPE="text/css" HREF="/fontello/fontello-2dc47ee2.css">
<LINK REL="stylesheet" TYPE="text/css" HREF="/${currentStyle}/combined.css">


<sec:authorize access="isAuthenticated()">
  <c:if test="${currentStyle == 'black' and principal.useHover}">
    <LINK REL=STYLESHEET TYPE="text/css" HREF="/black/hover.css">
  </c:if>
</sec:authorize>

<!--[if lt IE 9]>
<script src="/js/html5.js" type="text/javascript"></script>
<![endif]-->

<c:if test="${not pageContext.request.secure}">
    <script src="http://ajax.googleapis.com/ajax/libs/jquery/1.7.2/jquery.min.js" type="text/javascript"></script>
</c:if>

<c:if test="${pageContext.request.secure}">
    <script src="https://ajax.googleapis.com/ajax/libs/jquery/1.7.2/jquery.min.js" type="text/javascript"></script>
</c:if>

<script type="text/javascript">
  if (typeof jQuery == 'undefined') {
      document.write(unescape("%3Cscript src='/js/jquery-1.7.2.min.js' type='text/javascript'%3E%3C/script%3E"));
  }
</script>

<script src="/js/head.js" type="text/javascript"></script>
