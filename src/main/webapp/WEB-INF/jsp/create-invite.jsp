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
<%@ page contentType="text/html; charset=utf-8" %>
<%--@elvariable id="template" type="ru.org.linux.site.Template"--%>
<%@ taglib tagdir="/WEB-INF/tags" prefix="lor" %>
<%@ taglib prefix="fn" uri="http://java.sun.com/jsp/jstl/functions" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>

<jsp:include page="/WEB-INF/jsp/head.jsp"/>

<title>Приглашение нового участника</title>
<jsp:include page="/WEB-INF/jsp/header.jsp"/>
<h1>Приглашение нового участника</h1>

  <form method=POST action="/create-invite" class="form-horizontal">
    <lor:csrf/>

    <div class="control-group">
      <label class="control-label" for="field_nick">Email</label>
      <div class="controls">
        <input type="email" name="email" required autofocus id="field_nick">
        <span class="help-block">
          Проверьте правильность написания email!
        </span>
      </div>
    </div>

    <div class="control-group">
      <div class="controls">
        <button type=submit class="btn btn-primary">Пригласить</button>
      </div>
    </div>
    <input type="hidden" name="action" value="new" />
  </form>


<jsp:include page="/WEB-INF/jsp/footer.jsp"/>
