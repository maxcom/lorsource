<%@ page import="java.sql.Connection" %>
<%@ page import="java.sql.ResultSet" %>
<%@ page import="java.sql.Statement" %>
<%@ page import="java.util.Date" %>
<%@ page import="java.util.List" %>
<%@ page import="ru.org.linux.boxlet.BoxletVectorRunner" %>
<%@ page import="ru.org.linux.site.NewsViewer" %>
<%@ page import="ru.org.linux.site.Template" %>
<%@ page import="ru.org.linux.site.User" %>
<%@ page import="ru.org.linux.site.ViewerCacher" %>
<%@ page import="ru.org.linux.util.ServletParameterParser" %>

<%@ taglib uri="http://java.sun.com/jsp/jstl/functions" prefix="fn" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>

<LINK REL=STYLESHEET TYPE="text/css" HREF="/common.css" TITLE="Normal">
<link rel="search" title="Search L.O.R." href="/search.jsp">
<link rel="top" title="Linux.org.ru" href="/">
<script src="/js/lor.js" type="text/javascript">;</script>

<base href="${fn:escapeXml(template.mainUrl)}">

<jsp:include page="${template.style}/head-main.jsp"/>

<img src="http://counter.rambler.ru/top100.cnt?29833" alt="Rambler's Top100" width=1 height=1 border=0>
<!--TopList COUNTER-->
<img height=1 width=1 src="http://top.list.ru/counter?id=71642" alt="">
<!--TopList COUNTER-->
