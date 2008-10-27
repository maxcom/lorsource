<%@ page contentType="text/html; charset=utf-8"%>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>


<LINK REL=STYLESHEET TYPE="text/css" HREF="/black/style.css" TITLE="Normal">
<LINK REL="shortcut icon" HREF="/favicon.ico" TYPE="image/x-icon">
</head>
<body>
<table border="0" cellspacing="0" cellpadding="0" width="100%">
<tr>
        <td rowspan="2" align=left><a href="/"><img src="/black/lor-new.png" width=282 height=60 border=0 alt="Linux.org.ru"></a></td>
        <td align="right">
          <c:if test="${template.sessionAuthorized}">
            <c:url var="userUrl" value="/whois.jsp">
              <c:param name="nick" value="${template.nick}"/>
            </c:url>
            добро пожаловать, <a style="text-decoration: none" href="${userUrl}">${template.nick}</a>
          </c:if>

          <c:if test="${not template.sessionAuthorized}">
            <div id="regmenu">
              <a style="text-decoration: none" href="/register.jsp">Регистрация</a> -
              <a style="text-decoration: none" href="/" onclick="showLoginForm(); return false;">Вход</a>
            </div>

            <form method=POST action="login.jsp" style="display: none" id="regform">
              Имя: <input type=text name=nick size=15>
              Пароль: <input type=password name=passwd size=15>
              <input type=submit value="Вход">
              <input type="button" value="Отмена" onclick="hideLoginForm(); return false">
            </form>
          </c:if>

        </td>
  </tr>
  <tr>
        <td align=right valign=bottom>
                <a style="text-decoration: none" href="/">Новости</a> -
                <a style="text-decoration: none" href="view-news.jsp?section=3">Галерея</a> -
                <a style="text-decoration: none" href="view-section.jsp?section=2">Форум</a> -
                <a style="text-decoration: none" href="/books">Документация</a> -
                <a style="text-decoration: none" href="/wiki">Wiki</a> -
                <a style="text-decoration: none" href="search.jsp">Поиск</a>
        </td>
</tr>
</table>

