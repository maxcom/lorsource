<%@ page pageEncoding="koi8-r" contentType="text/html; charset=utf-8"%>
<%@ page import="java.net.URLEncoder"  %>
<%@ page import="java.sql.Connection" %>
<%@ page import="java.sql.ResultSet" %>
<%@ page import="java.sql.Statement" %>
<%@ page import="ru.org.linux.site.LorDataSource" %>
<jsp:include page="WEB-INF/jsp/head.jsp"/>

<title>О Сервере</title>
<jsp:include page="WEB-INF/jsp/header.jsp"/>
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
Мы работаем на:
    <ul>
      <li>Fedora 7</li>
      <li>СУБД PostgreSQL 8.2</li>
      <li>Apache2 2.2</li>
      <li>Sun Java SDK 1.5</li>
      <li>Apache Tomcat 5.5</li>
      <li>memcached 1.2</li>
    </ul>
      Спасибо Олегу Дрокину (<b>green</b>) за
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
    db = LorDataSource.getConnection();

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

<h1>Связанные проекты</h1>
<ul>
  <li><a href="http://www.lorquotes.ru/">LorQuotes</a> - избранные цитаты</li>
  <li><a href="http://www.lastfm.ru/group/Linux-org-ru">Группа linux.org.ru на last.fm</a></li>
  <li><a href="http://community.livejournal.com/l_o_r/">Филиал l.o.r. в ЖЖ</a></li>
</ul>

<jsp:include page="WEB-INF/jsp/footer.jsp"/>
