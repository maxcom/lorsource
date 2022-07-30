<%@ page import="org.apache.commons.io.IOUtils" %>
<%@ page import="ru.org.linux.site.Template" %>
<%@ page import="java.io.InputStream" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%--
  ~ Copyright 1998-2015 Linux.org.ru
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
  <c:set var="template" value="<%= Template.getTemplate() %>"/>
</c:if>
<!DOCTYPE html>
<html lang=ru>
<head>
<link rel="stylesheet" type="text/css" href="/${template.style}/combined.css?MAVEN_BUILD_TIMESTAMP">

<link rel="yandex-tableau-widget" href="/manifest.json" />
<meta name="referrer" content="always">

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

<script type="text/javascript">
  $script('/webjars/jquery/2.2.4/jquery.min.js', 'jquery');

  $script.ready('jquery', function() {
    $script('/js/plugins.js?MAVEN_BUILD_TIMESTAMP', 'plugins');
    $script('/js/lor.js?MAVEN_BUILD_TIMESTAMP', 'lorjs');
  });

  $script('/js/highlight.pack.js', 'hljs');
  $script.ready(['jquery', 'hljs'], function() {
    $(function() {
      hljs.initHighlighting();
    });
  });

  $script('/js/realtime.js?MAVEN_BUILD_TIMESTAMP', "realtime");
</script>
