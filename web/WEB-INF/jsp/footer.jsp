<%@ page contentType="text/html; charset=utf-8"%>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%--
  ~ Copyright 1998-2010 Linux.org.ru
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
</div>
<div id="ft">

<p id="ft-info">
  <a href="server.jsp">О Сервере</a> -
  <a href="rules.jsp">Правила форума</a><br>
  <a href="${template.mainUrl}">${template.mainUrl}</a>
</p>

<div id="ft-buttons">
  <!-- begin of Top100 logo -->
  <a href="http://top100.rambler.ru/home?id=29833" target="_blank"><img src="http://top100-images.rambler.ru/top100/banner-88x31-rambler-black2.gif" alt="Rambler's Top100" width="88" height="31" border="0" /></a>
  <!-- end of Top100 logo -->
  <!--TopList LOGO-->
  <a target=_top href="http://top.list.ru/jump?from=71642"><img src="http://top.list.ru/counter?id=71642;t=11;l=1" border=0 height=31 width=88 alt="TopList"></a>
  <!--TopList LOGO-->
</div>

<script type="text/javascript">
var gaJsHost = (("https:" == document.location.protocol) ? "https://ssl." : "http://www.");
document.write(unescape("%3Cscript src='" + gaJsHost + "google-analytics.com/ga.js' type='text/javascript'%3E%3C/script%3E"));
</script>
<script type="text/javascript">
try {
var pageTracker = _gat._getTracker("UA-2184304-1");
pageTracker._setCustomVar(1, "Authorized", "${template.sessionAuthorized}");
pageTracker._setCustomVar(2, "Style", "${template.style}");
pageTracker._trackPageview();
pageTracker._trackPageLoadTime();
} catch(err) {}</script>

</div>

<c:if test="${template.style == 'tango'}">
  </div>
</c:if>

<!-- begin of Top100 code -->
<script id="top100Counter" type="text/javascript" src="http://counter.rambler.ru/top100.jcn?29833"></script><noscript><img src="http://counter.rambler.ru/top100.cnt?29833" alt="" width="1" height="1" border="0"/></noscript>
<!-- end of Top100 code -->

</body></html>