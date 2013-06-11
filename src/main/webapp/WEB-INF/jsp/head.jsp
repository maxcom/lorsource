<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
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
<%--@elvariable id="template" type="ru.org.linux.site.Template"--%>
<!DOCTYPE html>
<html lang=ru>
<head>
<c:if test="${template.style=='tango'}">
  <c:if test="${not pageContext.request.secure}">
    <link href='http://fonts.googleapis.com/css?family=Droid+Sans+Mono|Open+Sans:600&amp;subset=latin,cyrillic' rel='stylesheet' type='text/css'>
  </c:if>
  <c:if test="${pageContext.request.secure}">
    <link href='https://fonts.googleapis.com/css?family=Droid+Sans+Mono|Open+Sans:600&amp;subset=latin,cyrillic' rel='stylesheet' type='text/css'>
  </c:if>
</c:if>
<c:if test="${template.style!='tango'}">
  <c:if test="${not pageContext.request.secure}">
    <link href='http://fonts.googleapis.com/css?family=Droid+Sans+Mono&amp;subset=latin,cyrillic' rel='stylesheet' type='text/css'>
  </c:if>
  <c:if test="${pageContext.request.secure}">
    <link href='https://fonts.googleapis.com/css?family=Droid+Sans+Mono&amp;subset=latin,cyrillic' rel='stylesheet' type='text/css'>
  </c:if>
</c:if>

<LINK REL="stylesheet" TYPE="text/css" HREF="/${template.style}/combined.css?MAVEN_BUILD_TIMESTAMP">

<c:if test="${template.style=='black' and template.prof.useHover}">
  <LINK REL=STYLESHEET TYPE="text/css" HREF="/black/hover.css">
</c:if>

<script src="/js/script.min.js" type="text/javascript"></script>

  <!--[if lt IE 9]>
<script src="/js/html5.js" type="text/javascript"></script>
<![endif]-->

<c:if test="${not pageContext.request.secure}">
    <script src="http://ajax.googleapis.com/ajax/libs/jquery/1.8.3/jquery.min.js" type="text/javascript"></script>
</c:if>

<c:if test="${pageContext.request.secure}">
    <script src="https://ajax.googleapis.com/ajax/libs/jquery/1.8.3/jquery.min.js" type="text/javascript"></script>
</c:if>

<script type="text/javascript">
  if (typeof jQuery == 'undefined') {
      document.write(unescape("%3Cscript src='/js/jquery-1.8.3.min.js' type='text/javascript'%3E%3C/script%3E"));
  }
</script>

<script type="text/javascript">
  $script('/js/plugins.js', 'plugins');
  $script.ready('plugins', function() {
    $script('/js/lor.js?MAVEN_BUILD_TIMESTAMP', 'lorjs');
  });
  $script('/js/highlight.pack.js', function() { hljs.initHighlightingOnLoad(); });
</script>
