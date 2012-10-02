<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="sec" uri="http://www.springframework.org/security/tags" %>
<%@ page contentType="text/html; charset=utf-8"%>
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
<!DOCTYPE html PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN" "http://www.w3.org/TR/html4/loose.dtd">

<html lang=ru>
<head>

<LINK REL="STYLESHEET" TYPE="text/css" HREF="/${currentStyle}/style.css" TITLE="Normal">
<LINK REL="STYLESHEET" TYPE="text/css" HREF="/${currentStyle}/dw-main.css">
<base target="_top">
</head>
<body>
<table border="0" cellspacing="0" cellpadding="0">
      <tr>
        <td><marquee behavior="scroll" direction="up" height="400" ScrollAmount="1" ScrollDelay="100" onMouseOver="this.stop()" onMouseOut="this.start()">
          <script type="text/javascript" language="Javascript">

      var site_id = 40;
      var dw_rss_feed = 'https://www.ibm.com/developerworks/ru/views/rss'
      +'/customrssatom.jsp?feed_by=rss&zone_by=IBM+Systems'
      +',Java+technology,Web+services,Linux,XML,Open+sourc'
      +'e&type_by=Articles,Tutorials&search_by=&pubdate=01'
      +'/01/2007&max_entries=10&encoding=UTF-8';

      var num_of_articles = 10;
      var enc = 'UTF-8';

    </script>
<c:if test="${pageContext.request.secure}">
  <script type="text/javascript"
    src="https://www.ibm.com/developerworks/everywhere/ew.js" language="Javascript">
  </script>
</c:if>
<c:if test="${not pageContext.request.secure}">
    <script type="text/javascript"
      src="http://www.ibm.com/developerworks/everywhere/ew.js" language="Javascript">
    </script>
</c:if>
          
          </marquee>
        </td>
      </tr>
    </table>
</body>
