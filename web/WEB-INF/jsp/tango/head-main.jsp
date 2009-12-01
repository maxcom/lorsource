<%@ page contentType="text/html; charset=utf-8"%>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
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

<!-- head-main -->
<link href="/js/syntax/styles/shCore.css" rel="stylesheet" type="text/css" />
<link href="/js/syntax/styles/tango.css" rel="stylesheet" type="text/css" />

<c:if test="${not template.mobile}">
<LINK REL=STYLESHEET TYPE="text/css" HREF="/tango/reset-fonts-grids.css" TITLE="Normal">
<LINK REL=STYLESHEET TYPE="text/css" HREF="/tango/style.css" TITLE="Normal">
</c:if>
<c:if test="${template.mobile}">
<LINK REL=STYLESHEET TYPE="text/css" HREF="/tango/style-mobile.css" TITLE="Normal">
</c:if>
<LINK REL=STYLESHEET TYPE="text/css" HREF="/tango/tango-dark.css" TITLE="Normal">
<LINK REL="shortcut icon" HREF="/favicon.ico" TYPE="image/x-icon">
<script src="/js/jquery.corner.js" type="text/javascript"></script>
<script type="text/javascript">
  $(document).ready(function() {
    if ($.browser.msie || $.browser.opera) {
      $('.msg').corner();
      $('.column .boxlet h2').corner();
    }

    SyntaxHighlighter.all();
  });
</script>

<script type="text/javascript" src="/js/syntax/scripts/shCore.js"></script>
<script type="text/javascript" src="/js/syntax/scripts/shBrushPlain.js"></script>

</head>
<body>
<div id="doc3" class="yui-t5">
  	<div id="hd">
        <h1><a id="sitetitle" href="/">LINUX.ORG.RU&nbsp;&#8212; Русская информация об&nbsp;ОС&nbsp;Linux</a></h1>

        <div id="loginGreating" class="head">
        <c:if test="${template.sessionAuthorized}">
          <c:url var="userUrl" value="/whois.jsp">
            <c:param name="nick" value="${template.nick}"/>
          </c:url>
          добро пожаловать, <a style="text-decoration: none" href="${userUrl}">${template.nick}</a>
          [<a href="logout.jsp" title="Выйти">x</a>]
        </c:if>

        <c:if test="${not template.sessionAuthorized}">
          <div id="regmenu" class="head">
            <a href="/register.jsp">Регистрация</a> -
            <a id="loginbutton" href="/login.jsp">Вход</a>
          </div>

          <form method=POST action="login.jsp" style="display: none" id="regform">
            Имя: <input type=text name=nick size=15><br>
            Пароль: <input type=password name=passwd size=15><br>
            <input type=submit value="Вход">
            <input id="hide_loginbutton" type="button" value="Отмена">
          </form>
        </c:if>
        </div>

        <div class="menu">
            <ul class="primary">
                <li class="first"><a href="/">Новости</a></li>
                <li><a href="/view-news.jsp?section=3">Галерея</a></li>

                <li><a href="/view-section.jsp?section=2">Форум</a></li>
                <li><a href="/tracker.jsp">Трекер</a></li>
                <li><a href="/wiki/">Wiki</a></li>
                <li class="last"><a href="/search.jsp">Поиск</a></li>
            </ul>
            <ul class="secondary">
                <li class="first"><a href="/wiki/en/Linux">O linux</a></li>
                <li><a href="/wiki/en/%D0%94%D0%B8%D1%81%D1%82%D1%80%D0%B8%D0%B1%D1%83%D1%82%D0%B8%D0%B2%D1%8B">Дистрибутивы</a></li>

                <li class="last"><a href="/server.jsp">О сервере</a></li>
            </ul>
        </div>
    </div>



<div style="clear: both"></div>
