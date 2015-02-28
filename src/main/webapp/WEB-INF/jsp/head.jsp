<%@ page import="org.apache.commons.io.IOUtils" %>
<%@ page import="ru.org.linux.site.Template" %>
<%@ page import="java.io.InputStream" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
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
<%--@elvariable id="template" type="ru.org.linux.site.Template"--%>
<c:if test="${empty template}">
  <c:set var="template" value="<%= Template.getTemplate(request) %>"/>
</c:if>
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

<link rel="stylesheet" type="text/css" href="/${template.style}/combined.css?MAVEN_BUILD_TIMESTAMP">

<c:if test="${template.style=='black' and template.prof.useHover}">
  <link rel=STYLESHEET type="text/css" href="/black/hover.css">
</c:if>

<script type="text/javascript">
  <c:set var="scriptminjs">
  <%
   InputStream reader = request.getServletContext().getResourceAsStream("/js/script.min.js");

   try {
     IOUtils.copy(reader, out);
   } finally{
     IOUtils.closeQuietly(reader);
   }
  %>
  </c:set>
  <c:out value="${scriptminjs}" escapeXml="false"/>
</script>

<!--[if lt IE 9]>
  <script src="/webjars/html5shiv/3.7.2/html5shiv.min.js" type="text/javascript"></script>
<![endif]-->

<script type="text/javascript">
  $script('/webjars/jquery/1.11.2/jquery.min.js', 'jquery');

  $script.ready('jquery', function() {
    $script('/js/plugins.js', 'plugins');
    $script('/js/lor.js?MAVEN_BUILD_TIMESTAMP', 'lorjs');
  });

  $script('/js/highlight.pack.js', function() { hljs.initHighlightingOnLoad(); });
</script>
