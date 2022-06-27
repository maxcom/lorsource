<%@ page contentType="text/html; charset=utf-8"%>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%--
  ~ Copyright 1998-2022 Linux.org.ru
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
<%--@elvariable id="configuration" type="ru.org.linux.spring.SiteConfig"--%>
</div>
<footer id="ft">

<p id="ft-info">
  <a href="/about">О Сервере</a> -
  <a href="/help/rules.md">Правила форума</a> -
  <a href="/help/lorcode.md">Правила разметки (LORCODE)</a> -
  <a href="/help/markdown.md">Правила разметки (Markdown)</a><br>
  <a href="https://github.com/maxcom/lorsource/issues">Сообщить об ошибке</a><br>
  <a href="${configuration.secureUrl}">${configuration.secureUrl}</a>
</p>

<script type="text/javascript">
  var _gaq = _gaq || [];
  _gaq.push(['_setAccount', 'UA-2184304-1']);
  _gaq.push(['_setCustomVar', 1, 'Authorized', '${template.sessionAuthorized}']);
  _gaq.push(['_setCustomVar', 2, 'Style', '${template.style}']);
  _gaq.push(['_setSiteSpeedSampleRate', 10]);
  _gaq.push(['_gat._forceSSL']);
  _gaq.push(['_trackPageview']);

  (function() {
    var ga = document.createElement('script'); ga.type = 'text/javascript'; ga.async = true;
    ga.src = 'https://ssl.google-analytics.com/ga.js';
    var s = document.getElementsByTagName('script')[0]; s.parentNode.insertBefore(ga, s);
  })();
</script>

<script type="text/javascript">
  <c:if test="${template.sessionAuthorized}">
    $script.ready('realtime', function() {
      RealtimeContext.start("${configuration.WSUrl}");
    });
  </c:if>
</script>


</footer>
</body></html>
