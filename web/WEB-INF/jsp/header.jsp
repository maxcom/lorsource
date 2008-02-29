<%@ page import="java.io.File" %>
<%@ page import="java.io.IOException" %>
<%@ page import="java.io.PrintWriter" %>
<%@ page import="java.io.StringWriter" %>
<%@ page import="java.net.URLEncoder" %>
<%@ page import="java.sql.*" %>
<%@ page import="java.text.DateFormat" %>
<%@ page import="java.text.SimpleDateFormat" %>
<%@ page import="java.util.*" %>
<%@ page import="java.util.Date" %>
<%@ page import="java.util.logging.Logger" %>
<%@ page import="javax.mail.Session" %>
<%@ page import="javax.mail.Transport" %>
<%@ page import="javax.mail.internet.InternetAddress" %>
<%@ page import="javax.mail.internet.MimeMessage" %>
<%@ page import="javax.servlet.http.Cookie" %>
<%@ page import="javax.servlet.http.HttpServletResponse" %>
<%@ page import="com.danga.MemCached.MemCachedClient" %>
<%@ page import="org.apache.commons.fileupload.FileItem" %>
<%@ page import="org.apache.commons.fileupload.disk.DiskFileItemFactory" %>
<%@ page import="org.apache.commons.fileupload.servlet.ServletFileUpload" %>
<%@ page import="org.apache.commons.lang.StringUtils" %>
<%@ page import="ru.org.linux.boxlet.BoxletVectorRunner" %>
<%@ page import="ru.org.linux.site.*" %>
<%@ page import="ru.org.linux.storage.StorageNotFoundException" %>
<%@ page import="ru.org.linux.util.*" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/functions" prefix="fn" %>

<LINK REL=STYLESHEET TYPE="text/css" HREF="/common.css" TITLE="Normal">
<link rel="search" title="Search L.O.R." href="/search.jsp">
<link rel="top" title="Linux.org.ru" href="/">
<script src="/js/lor.js" type="text/javascript">;</script>

<c:if test="${template.hover}">
  <LINK REL=STYLESHEET TYPE="text/css" HREF="/${template.style}/hover.css" TITLE="Normal">
</c:if>

<base href="${fn:escapeXml(template.mainUrl)}">

<jsp:include page="${template.style}/head.jsp"/>

<!--TopList COUNTER-->
<img height=1 width=1 src="http://top.list.ru/counter?id=71642" alt="">
<!--TopList COUNTER-->
