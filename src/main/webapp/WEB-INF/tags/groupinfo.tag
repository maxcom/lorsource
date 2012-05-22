<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ tag import="ru.org.linux.site.Template" %>
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
<%@ tag pageEncoding="utf-8" %>
<%@ attribute name="group" required="true" type="ru.org.linux.group.PreparedGroupInfo" %>
<% Template tmpl = Template.getTemplate(request); %>
<c:if test="${group.info != null}">
  <p style="margin-top: 0"><em>${group.info}</em></p>
</c:if>

<c:if test="${group.longInfo != null}">
  <div class="infoblock">
  ${group.longInfo}
    <% if (tmpl.isModeratorSession()) { %>
    <p>[<a href="groupmod.jsp?group=${group.id}">править</a>]</p>
    <% } %>
  </div>
</c:if>

