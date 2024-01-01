<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ page contentType="text/html; charset=utf-8"%>
<%--
  ~ Copyright 1998-2024 Linux.org.ru
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
<div id="ft">
  <p id="ft-info">
    <a href="/about">О Сервере</a> -
    <a href="/help/rules.md">Правила форума</a><br>

  Разработка и&nbsp;поддержка&nbsp;&#8212; <a href="/people/maxcom/profile">Максим Валянский</a> 1998&ndash;2024 <br>
  Сервер для сайта предоставлен &laquo;<a href="http://www.ittelo.ru/" target="_blank">ITTelo</a>&raquo;<br>
  Размещение сервера и&nbsp;подключение к&nbsp;сети Интернет осуществляется компанией
    &laquo;<a href="https://selectel.ru/?ref_code=3dce4333ba" target="_blank">Selectel</a>&raquo;.
  </p>

<script type="text/javascript">
  <c:if test="${template.sessionAuthorized}">
    $script.ready('realtime', function() {
      RealtimeContext.start("${configuration.WSUrl}");
    });
  </c:if>
</script>

</div>
</body>
</html>
