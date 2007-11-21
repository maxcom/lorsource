<%@ page pageEncoding="koi8-r" contentType="text/html; charset=utf-8"%>
<%@ page import="ru.org.linux.site.Template" errorPage="/error.jsp"%>
<% Template tmpl = new Template(request, config, response); %><%= tmpl.head() %>
<title>Ответы на часто задаваемые вопросы (FAQ)</title>
<%= tmpl.DocumentHeader() %>
<div class=text>
<h1>Ответы на часто задаваемые вопросы (FAQ)</h1>

<ul>
<li><a href="http://www.linux.org.ru/books/lor-faq/">General/Desktop FAQ</a>
<li><a href="http://ivlad.unixgods.net/lor-faq/">Security FAQ</a>
</ul>

</div>
<%= tmpl.DocumentFooter() %>
