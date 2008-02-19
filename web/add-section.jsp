<%@ page pageEncoding="koi8-r" contentType="text/html; charset=utf-8"%>
<%@ page import="java.sql.Connection,java.sql.ResultSet,java.sql.Statement,java.util.Date" errorPage="/error.jsp"%>
<%@ page import="java.util.List" %>
<%@ page import="ru.org.linux.boxlet.BoxletVectorRunner" %>
<%@ page import="ru.org.linux.site.*" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<% Template tmpl = new Template(request, config, response); %>
<%= tmpl.head() %>
<%
  Connection db = null;
  try {

    if (request.getParameter("section") == null) {
      throw new MissingParameterException("section");
    }

    int sectionid = Integer.parseInt(request.getParameter("section"));

    db = tmpl.getConnection();
    Statement st = db.createStatement();

    Section section = new Section(db, sectionid);

    if (!section.isBrowsable()) {
      throw new BadSectionException(sectionid);
    }

%>
<c:set var="section" value="<%= section %>"/>

<title>${section.title}: добавление</title>
<%= tmpl.DocumentHeader() %>

<c:set var="info" value="<%= tmpl.getObjectConfig().getStorage().readMessageNull("addportal", String.valueOf(sectionid)) %>"/>

<c:if test="${info!=null}">
  ${info}
  <h2>Выберите группу</h2>
</c:if>

<c:if test="${info==null}">
  <h1>${section.title}: добавление</h1>
</c:if>

Доступные группы:
<ul>
<c:forEach var="group"
           items="<%= Group.getGroups(db, section) %>">
  <li>
    <a href="add.jsp?group=${group.id}&amp;noinfo=1">${group.title}</a> (<a href="group.jsp?group=${group.id}">просмотр...</a>)

    <c:if test="${group.info != null}">
      - <em><c:out value="${group.info}" escapeXml="false"/></em>
    </c:if>
  </li>
</c:forEach>
</ul>

<%
   st.close();
  } finally {
    if (db!=null) {
      db.close();
    }
  }
%>
<%=	tmpl.DocumentFooter() %>
