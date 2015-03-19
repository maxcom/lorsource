<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ page contentType="text/html; charset=utf-8"%>
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

</div>
<div id="ft">
  <p id="ft-info">
    <a href="/about">О Сервере</a> -
    <a href="rules.jsp">Правила форума</a><br>

  Разработка и&nbsp;поддержка&nbsp;&#8212; <a href="/people/maxcom/profile">Максим Валянский</a> 1998&ndash;2015<br>
  Размещение сервера и&nbsp;подключение его к&nbsp;сети Интернет осуществляется компанией
  ООО &laquo;<a href="http://www.ratel.ru">НИИР-РадиоНет</a>&raquo;
  </p>

<script type="text/javascript">
var gaJsHost = (("https:" == document.location.protocol) ? "https://ssl." : "http://www.");
document.write(unescape("%3Cscript src='" + gaJsHost + "google-analytics.com/ga.js' type='text/javascript'%3E%3C/script%3E"));
</script>
<script type="text/javascript">
try {
var pageTracker = _gat._getTracker("UA-2184304-1");
pageTracker._setCustomVar(1, "Authorized", "${template.sessionAuthorized}");
pageTracker._setCustomVar(2, "Style", "${template.style}");

pageTracker._setSiteSpeedSampleRate(10);
pageTracker._trackPageview();
} catch(err) {}</script>

</div>
</body>
</html>
