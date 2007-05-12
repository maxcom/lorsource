<%@ page contentType="text/html; charset=koi8-r"%>
<%@ page import="ru.org.linux.site.ScriptErrorException,ru.org.linux.site.Template" isErrorPage="true" %>
<%@ page import="ru.org.linux.site.UserErrorException"%>
<%@ page import="ru.org.linux.util.HTMLFormatter"%>
<%@ page import="ru.org.linux.util.ServletParameterException"%>
<%@ page import="ru.org.linux.util.StringUtil"%>
<% Template tmpl = new Template(request, config, response, true); %>
<%= tmpl.head() %>
<title>Ошибка: <%= HTMLFormatter.htmlSpecialChars(exception.getClass().getName()) %></title>
<%= tmpl.DocumentHeader() %>

<h1><%=exception.getMessage()==null?HTMLFormatter.htmlSpecialChars(exception.getClass().getName()):HTMLFormatter.htmlSpecialChars(exception.getMessage()) %></h1>

<% if (exception instanceof UserErrorException) { %>
<% } else if (exception instanceof ScriptErrorException || exception instanceof ServletParameterException) { %>
Скрипту, генерирующему страничку были переданы некорректные
параметры. Если на эту страничку вас привела одна из
страниц нашего сайта, пожалуйста
<a href="mailto:bugs@linux.org.ru">сообщите</a> нам адреса
текущей и ссылающейся страниц.
<% } else { %>

К сожалению, произошла исключительная ситуация при генерации страницы. Если
вы считаете, что она возникла по причине нашей ошибки, пожалуйста <a href="mailto:bugs@linux.org.ru">сообщите</a> нам о ошибке и условиях ее возникновения. Не забудьте
также указать полный URL странички, вызвавшей исключение.

<pre>
<%= HTMLFormatter.htmlSpecialChars(StringUtil.getStackTrace(exception)) %>
</pre>
<%
  tmpl.getLogger().error("exception", exception.toString()+": "+StringUtil.getStackTrace(exception));
%>
<% } %>

<%= tmpl.DocumentFooter() %>
