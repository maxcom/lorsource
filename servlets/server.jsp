<%@ page contentType="text/html; charset=koi8-r"%>
<%@ page import="java.net.URLEncoder" errorPage="error.jsp"%>
<%@ page import="java.sql.Connection" %>
<%@ page import="java.sql.ResultSet" %>
<%@ page import="java.sql.Statement" %>
<%@ page import="ru.org.linux.site.Template" %>
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
Вы можете использовать эту кнопку для ссылки на наш сайт:<br>
<img width=88 height=31 src="/img/button.gif">
  <p>
  и чудо баннер:<br>
  <img src="/img/linux-banner5.gif" alt="www.linux.org.ru" width="468" height="60"> 
  </p>

<h1>Хостинг</h1>
Размещение сервера и подключение к сети Интернет осуществляется компанией 
ООО "<a href="http://www.ratel.ru">НИИР-РадиоНет</a>".
<p>
	Статистику сервера можно посмотреть тут: <a href="http://linuxhacker.ru/stats">статистика</a>.

<h1>Софт</h1>
  <p>
Мы работаем на Fedora 7, СУБД PostgreSQL 8.2, Apache2 2.0,
Sun Java SDK 1.5, Caucho Resin 2.1. Спасибо Олегу Дрокину (<b>green</b>) за
администрирование.
  </p>

<h1>Наша команда</h1>
Проект реализован и развивается исключительно в свободное время авторов. 
<ul>
<li><a href="whois.jsp?nick=maxcom">Максим Валянский</a> (maxcom) - <i>координатор
проекта</i> -
разработка, поддержка, дизайн, новости, информационное наполнение.

<li><a href="whois.jsp?nick=green">Олег Дрокин</a> (green) - администрирование сервера, железо

<%
  Connection db = null;

  try {
    db = tmpl.getConnection("server");

    Statement st = db.createStatement();
    ResultSet rs = st.executeQuery("SELECT nick, name FROM users WHERE canmod ORDER BY id");

    while (rs.next()) {
      String nick = rs.getString("nick");
      String name = rs.getString("name");

      if (nick.equals("maxcom")) {
        continue;    
      }

      out.print("<li><a href=\"whois.jsp?nick="+URLEncoder.encode(nick)+"\">"+name+"</a> ("+nick+")");  
    }
%>

</ul>

  <%
  } finally {
    if (db!=null) {
      db.close();
    }
  }
  %>
</div>

<h1>Реклама на сайте</h1>
Linux.org.ru некоммерческий проект, мы не занимаемся размещением рекламы на страничках сайта
сверх минимума, необходимого для работы сайта. Вы можете разместить рекламу через Google Adsense
на страничках нашего сайта по ссылке "<a href="https://adwords.google.com/select/OnsiteSignupLandingPage?client=ca-pub-6069094673001350&referringUrl=http://www.linux.org.ru/">размещение рекламы на этом сайте</a>".

<%= tmpl.DocumentFooter() %>
