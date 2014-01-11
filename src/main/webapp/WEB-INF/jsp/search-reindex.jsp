<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib tagdir="/WEB-INF/tags" prefix="lor" %>
<%--
  ~ Copyright 1998-2013 Linux.org.ru
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

<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<jsp:include page="/WEB-INF/jsp/head.jsp"/>
<title>Search Reindex</title>
<jsp:include page="/WEB-INF/jsp/header.jsp"/>
<h1>Reindex</h1>
<form action="/admin/search-reindex" method="POST">
  <lor:csrf/>
  <select name="action">
    <option value="all">all</option>
    <option value="current">current</option>
  </select>
  <button type="submit" class="btn btn-primary">Reindex</button>
</form>

<jsp:include page="/WEB-INF/jsp/footer.jsp"/>
