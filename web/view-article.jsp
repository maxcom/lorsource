<%@ page pageEncoding="koi8-r" contentType="text/html; charset=utf-8"%>
<%@ page import="ru.org.linux.site.MissingParameterException,ru.org.linux.site.Template" errorPage="/error.jsp" %>
<% Template tmpl = new Template(request, config, response); %>
<%= tmpl.head() %>
<% String article=request.getParameter("article"); 
	if (article==null) throw new MissingParameterException("article");
%>
<title>Статья</title>
<%= tmpl.DocumentHeader() %>
<div class="text">
<%= tmpl.getObjectConfig().getStorage().readMessage("articles", article) %>
</div>
<%= tmpl.DocumentFooter() %>
