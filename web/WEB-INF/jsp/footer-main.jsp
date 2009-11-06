<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ page contentType="text/html; charset=utf-8"%>

<%--
  ~ Copyright 1998-2009 Linux.org.ru
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

</div>
<div id="ft">
  <div align=center>
  <p>
  Разработка и&nbsp;поддержка&nbsp;&#8212; <a href="whois.jsp?nick=maxcom">Максим Валянский</a> 1998&ndash;2009<br>
  Размещение сервера и&nbsp;подключение его к&nbsp;сети Интернет осуществляется компанией
  ООО &laquo;<a href="http://www.ratel.ru">НИИР-РадиоНет</a>&raquo;
  </p>
  </div>

<div align=center style="margin-top: 1em">
<!-- begin of Top100 logo -->
<a href="http://top100.rambler.ru/home?id=29833" target="_blank"><img src="http://top100-images.rambler.ru/top100/banner-88x31-rambler-black2.gif" alt="Rambler's Top100" width="88" height="31" border="0" /></a>
<!-- end of Top100 logo -->
  <!--TopList LOGO-->
  <a target="_top" href="http://top.list.ru/jump?from=71642"><img src="http://top.list.ru/counter?id=71642;t=11;l=1" border=0 height=31 width=88 alt="TopList"></a>
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
pageTracker._setCustomVar(3, "Mobile View", "${template.mobile}");

pageTracker._trackPageview();
} catch(err) {}</script>

</div>

<c:if test="${template.style == 'tango'}">
  </div>
</c:if>

<!-- begin of Top100 code -->
<script id="top100Counter" type="text/javascript" src="http://counter.rambler.ru/top100.jcn?29833"></script><noscript><img src="http://counter.rambler.ru/top100.cnt?29833" alt="" width="1" height="1" border="0"/></noscript>
<!-- end of Top100 code -->

</body>
</html>
