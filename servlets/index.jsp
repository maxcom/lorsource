<%@ page contentType="text/html; charset=koi8-r"%>
<%@ page import="java.sql.Connection,java.sql.ResultSet,java.sql.Statement,java.util.Date,java.util.List,ru.org.linux.boxlet.BoxletRunner, ru.org.linux.boxlet.BoxletVectorRunner" errorPage="error.jsp" buffer="60kb"%>
<%@ page import="ru.org.linux.site.Template"%>
<%@ page import="ru.org.linux.site.User"%>
<% Template tmpl = new Template(request, config, response);
   tmpl.setMainPage(); %>
<%=   tmpl.head() %>
<title>LINUX.ORG.RU - Русская информация об ОС Linux</title>
<META NAME="Keywords" CONTENT="linux линукс операционная система документация gnu бесплатное свободное програмное обеспечение софт unix юникс software free documentation operating system новости news">
<META NAME="Description" CONTENT="Все о Linux на русском языке">
<LINK REL="alternate" TITLE="L.O.R RSS" HREF="http://linux.org.ru/rss.jsp" TYPE="application/rss+xml">

<%
   boolean redirect=false;
/*
   if (tmpl.getCookie("profile")!=null && !tmpl.getCookie("profile").equals("") && tmpl.getProfileName()==null) {
	response.setHeader("Location", tmpl.getRedirectUrl(tmpl.getCookie("profile")));
	response.setStatus(HttpServletResponse.SC_MOVED_TEMPORARILY);
	redirect=true;
   }

   if (tmpl.getProfile(request)!=null && tmpl.isUsingDefaultProfile()) {
   	response.setHeader("Location", tmpl.getMainUrl());
   	response.setStatus(HttpServletResponse.SC_MOVED_TEMPORARILY);
	redirect=true;
   }
*/
   if (!redirect) {
   	response.setDateHeader("Expires", new Date(new Date().getTime()-20*3600*1000).getTime());
   	response.setDateHeader("Last-Modified", new Date(new Date().getTime()-2*1000).getTime());
   }

%>
<%= tmpl.DocumentHeader() %>
<%
  boolean columns3 = tmpl.getProf().getBoolean("main.3columns");
%>

<div style="clear: both"></div>

<div class=column>
<div class=boxlet>
<h2>Вход на сайт</h2>
<% if (session==null || session.getAttribute("login")==null || !((Boolean) session.getAttribute("login")).booleanValue()) { %>
<form method=POST action="login.jsp">
Имя:<br><input type=text name=nick size=15 style="width: 90%"><br>
Пароль:<br><input type=password name=passwd size=15 style="width: 90%"><br>
<input type=submit value="Вход">
</form>
* <a href="lostpwd.jsp">Забыли пароль?</a><br>
* <a href="rules.jsp">Правила</a><br>
* <a href="register.jsp">Регистрация</a>
<% } else { %>
<form method=POST action="logout.jsp">
Вы вошли как <b><%= session.getAttribute("nick") %></b>
<%
  Connection db = tmpl.getConnection("index");
  User user = new User(db, (String) session.getAttribute("nick"));

  out.print(" (статус: "+user.getStatus()+ ')');
%><br>
<input type=submit value="Выход"><p>
</form>
* <a href="rules.jsp">Правила</a><br>
* <a href="edit-profile.jsp">Настройки</a><br>&nbsp;<br>
* <a href="show-topics.jsp?nick=<%= user.getNick() %>">Мои темы</a><br>
* <a href="show-comments.jsp?nick=<%= user.getNick() %>">Мои комментарии</a><br>
<% } %>

</div>

<!-- IBM developerWorks -->
<div class="boxlet" id="ibmdw">
<h2>Новые материалы на IBM developerWorks</h2>
  <iframe src="dw.jsp?height=400&amp;width=155&amp;main=1" width="158" height="400" scrolling="no" frameborder="0"></iframe>
  <br>&nbsp;<br>

  Профессиональный ресурс от IBM для специалистов в области разработки ПО. Рассылка выходит 1 раз в неделю.
  <form id="data1" method="post" enctype="multipart/form-data" action="http://www-931.ibm.com/bin/subscriptions/esi/subscribe/RURU/10209/">
                       e-mail:<br />
  <input type="text" size="15" name="email" style="width: 90%" value="" />
  <br />
  <input alt="subscribe" type="image" name="butSubmit1" value="Subscribe" src="http://www.ibm.com/i/v14/buttons/ru/ru/subscribe.gif">
                      </form>
</div>

<!-- boxes -->
<%
  BoxletVectorRunner boxes = null;

  if (tmpl.getProf().getBoolean("main.3columns"))
    boxes = new BoxletVectorRunner((List) tmpl.getProf().getObject("main3-1"), tmpl.getCache());
  else
    boxes = new BoxletVectorRunner((List) tmpl.getProf().getObject("main2"), tmpl.getCache());

  if (request.getParameter("nocache") != null)
    boxes.setCacheMode(true);

  out.print(boxes.getContent(tmpl.getObjectConfig(), tmpl.getProf()));

%>
</div>

<% if (columns3) { %>
<div class=column2>
<%
  boxes = new BoxletVectorRunner((List) tmpl.getProf().getObject("main3-2"), tmpl.getCache());
  if (request.getParameter("nocache") != null) boxes.setCacheMode(true);

  out.print(boxes.getContent(tmpl.getObjectConfig(), tmpl.getProf()));
%>
</div>
<% } %>

<div class="<%= columns3?"newsblog2":"newsblog"%>">
  
<h1><a href="view-section.jsp?section=1">Новости</a></h1>
<%
        if (tmpl.isSessionAuthorized(session) && ((Boolean) session.getValue("moderator")).booleanValue()) {
          out.print("<hr><div align=\"center\">");

          Connection db = tmpl.getConnection("index");

	  Statement st = db.createStatement();
	  ResultSet rs = st.executeQuery("select count(*) from topics,groups where section=1 and topics.groupid=groups.id and not deleted and not moderate AND postdate>(CURRENT_TIMESTAMP-'1 month'::interval)");

          if (rs.next()) {
	    int count = rs.getInt("count");

	    out.print("[<a style=\"text-decoration: none\" href=\"view-all.jsp\">Неподтвержденных новостей</a>: "+count+"]");
	  }

	  rs.close();

          rs = st.executeQuery("select count(*) from votenames where not deleted and not moderate");

          if (rs.next()) {
            int count = rs.getInt("count");

            out.print(" [<a style=\"text-decoration: none\" href=\"votes.jsp\">Неподтвержденных опросов</a>: "+count+"]");
          }

          rs.close();
          st.close();

          out.print("</div>");
        }

	BoxletRunner main=new BoxletRunner("fullnews", tmpl.getCache());
	if (request.getParameter("nocache")!=null) main.setCacheMode(true);
	out.print(main.getContent(tmpl.getObjectConfig(), tmpl.getProf()));
%>
<hr>
<div align=center>[<a href="add-section.jsp?section=1" style="text-decoration: none">добавить новость</a>]</div>
<hr>
</div>

<div style="clear: both"></div>

<%
	tmpl.getObjectConfig().SQLclose();
%>
<%=	tmpl.DocumentFooter(false) %>

<div align=center>
<p>
Разработка и поддержка - <a href="whois.jsp?nick=maxcom">Максим Валянский</a> 1998-2007<br>
Размещение сервера и подключение его к сети Интернет осуществляется компанией
ООО "<a href="http://www.ratel.ru">НИИР-РадиоНет</a>"<br>
</p>
</div>
</body>
</html>
