<%@ page contentType="text/html; charset=koi8-r"%>
<%@ page import="ru.org.linux.site.Template" errorPage="error.jsp"%>
<% Template tmpl = new Template(request, config, response); %>
<%= tmpl.head() %>
<title>О Сервере</title>
<%= tmpl.DocumentHeader() %>
<div class=text>

<h1>О Проекте</h1>
Некоммерческий проект <i>LINUX.ORG.RU: Русская информация об ОС Linux</i> был 
основан в октябре 
1998 года. Нашей целью является создание основного информационного ресурса о 
операционной системе Linux в России. Мы стараемся обеспечить возможность
обмена различной Linux-ориентированной информацией, последними новостями,
ссылками, документацией и другими ресурсами.

<h1>Наша кнопочка</h1>
Вы можете использовать эту кнопку для ссылки на наш сайт:<p>
<img width=88 height=31 src="/img/button.gif">

<h1>Хостинг</h1>
Размещение сервера и подключение к сети Интернет осуществляется компанией 
ООО "<a href="http://www.ratel.ru">НИИР-РадиоНет</a>".
<p>
	Статистику сервера можно посмотреть тут: <a href="http://linuxhacker.ru/stats">статистика</a>.

<h1>Софт</h1>
Мы работаем на Fedora Core 4 Linux, СУБД PostgreSQL 8.0, Apache2,
Sun Java SDK 1.4, Resin. Спасибо Олегу Дрокину (<b>green</b>) за
администрирование и hardware.<p>

<H1>Обратная связь</H1>
Комментарии просьба направлять по адресу <a href="mailto:webmaster@linux.org.ru">webmaster@linux.org.ru</a>. К сожалению, из-за большого
потока писем, у нас нет возможности отвечать на общие вопросы о Linux.
Задайте свой вопрос в <a href="view-section.jsp?section=2">форуме</a>.

<h1>Наша команда</h1>
Проект реализован и развивается исключительно в свободное время авторов. 
<ul>
<li><a href="whois.jsp?nick=maxcom">Максим Валянский</a> - <i>координатор
проекта</i> -
разработка, поддержка, дизайн, новости, информационное наполнение. 
<li><a href="whois.jsp?nick=ott">Алексей Отт</a> - раздел документации.
<li><a href="whois.jsp?nick=Tima_">Артем Веремей</a> - новости.
<li><a href="whois.jsp?nick=green">Олег Дрокин</a> - новости, администратор.
<li><a href="whois.jsp?nick=ivlad">Владимир Иванов</a> - новости, раздел Security форума
</ul>

</div>
<%= tmpl.DocumentFooter() %>
