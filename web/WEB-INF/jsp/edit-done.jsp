<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>

<jsp:include page="/WEB-INF/jsp/head.jsp"/>

<title>Редактирование сообщения прошло успешно</title>
<jsp:include page="/WEB-INF/jsp/header.jsp"/>

<c:if test="${modifiedTags}">
  tags updated
</c:if>

<c:if test="${modified}">
  <br><a href="view-message.jsp?msgid=${message.id}">Сообщение исправлено</a>.<br>
</c:if>

<jsp:include page="/WEB-INF/jsp/footer.jsp"/>

