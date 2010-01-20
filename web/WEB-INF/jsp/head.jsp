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

<!DOCTYPE html PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN" "http://www.w3.org/TR/html4/loose.dtd">

<html lang=ru>
<head>
<LINK REL=STYLESHEET TYPE="text/css" HREF="/common.css" TITLE="Normal">
  <c:if test="${template.mobile}">
    <LINK REL=STYLESHEET TYPE="text/css" HREF="/common-mobile.css" TITLE="Normal">
  </c:if>

  <c:if test="${template.hover}">
    <LINK REL=STYLESHEET TYPE="text/css" HREF="/${template.style}/hover.css" TITLE="Normal">
  </c:if>

<script src="http://ajax.googleapis.com/ajax/libs/jquery/1.2.6/jquery.min.js" type="text/javascript"></script>
<script src="/js/head.js" type="text/javascript"></script>