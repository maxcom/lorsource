<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<jsp:include page="/WEB-INF/jsp/head.jsp"/>
<title>Login</title>
<jsp:include page="/WEB-INF/jsp/header.jsp"/>

<h1>Вход</h1>

<c:if test="${error!=null}">
    <strong>Ошибка: ${error}</strong>
</c:if>

<form method=POST action="login.jsp">
Имя:<br><input type=text name=nick size=15><br>
Пароль:<br><input type=password name=passwd size=15><br>
<input type=submit value="Вход">
</form>

<h2>Ссылки</h2>
<ul>
    <li><a href="register.jsp">Регистрация</a></li>
    <li><a href="lostpwd.jsp">Получить забытый пароль</a></li>
</ul>

<jsp:include page="/WEB-INF/jsp/footer.jsp"/>