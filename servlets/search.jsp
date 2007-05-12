<%@ page contentType="text/html; charset=koi8-r"%>
<%@ page import="ru.org.linux.site.Template" errorPage="error.jsp"%>
<% Template tmpl = new Template(request, config, response);%>
<%= tmpl.head() %>
<title>Поиск по сайту</title>
<%= tmpl.DocumentHeader() %>

<H1>Поиск по сайту</h1>

<br>
<p>
<form method="post" action="/cgi-bin/htsearch">
<font size=-1>
Совпадения: <select name=method>
<option value=and>Все/All
<option value=or>Любые/Any
</select>
Формат: <select name=format>
<option value=builtin-long>Длинный/Long
<option value=builtin-short>Короткий/Short
</select>
</font>
<input type=hidden name=config value="linux.<%= tmpl.getStyle() %>">
<input type=hidden name=restrict value="">
<input type=hidden name=exclude value="">
<br>
Поиск:
<input type="text" size="30" name="words" value="">
<input type="submit" value="Искать/Search">
</form>

<a href="http://www.htdig.org"><IMG SRC="/htdig/htdig.gif" align=bottom alt="ht://Dig" border=0></a> 
<%= tmpl.DocumentFooter() %>
