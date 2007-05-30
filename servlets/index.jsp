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

<LINK REL=STYLESHEET TYPE="text/css" HREF="/<%= tmpl.getStyle() %>/dw-main.css">
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
<SCRIPT LANGUAGE="JavaScript" type="text/javascript">
   function addSidebar() {
      if ((typeof window.sidebar == "object") && (typeof window.sidebar.addPanel == "function")) {
         window.sidebar.addPanel ("Linux.Org.Ru News", "http://www.linux.org.ru/sidebar.jsp", "");
      } else {
         alert ("Кнопка добавления SideBar предназначена для броузера Mozilla/Netscape6/7");
      }
   }
//-->
</SCRIPT>
<%= tmpl.DocumentHeader() %>
<table border=0 width="100%">
<tr>
<td valign=top width=160>


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
<div class=boxlet>
<h2>Новые материалы на IBM developerWorks</h2>
  <table border="0" cellspacing="0" cellpadding="0">
        <tr>
          <td><marquee behavior="scroll" direction="up" height="400" width="160" ScrollAmount="1" ScrollDelay="100" onMouseOver="this.stop()" onMouseOut="this.start()">
            <script type="text/javascript" language="Javascript">

        var site_id = 40;
        var dw_rss_feed = 'http://www-128.ibm.com/developerworks/ru/views/rss'
        +'/customrssatom.jsp?feed_by=rss&zone_by=IBM+Systems'
        +',Java+technology,Web+services,Linux,XML,Open+sourc'
        +'e&type_by=Articles,Tutorials&search_by=&pubdate=01'
        +'/01/2007&max_entries=10&encoding=UTF-8';

        var num_of_articles = 10;
        var enc = 'UTF-8';

      </script>
            <script type="text/javascript"
        src="http://www-128.ibm.com/developerworks/everywhere/ew.js" language="Javascript">
      </script>
            </marquee>
          </td>
        </tr>
      </table>

  <p style="font-size: 10pt">
  Профессиональный ресурс от IBM для специалистов в области разработки ПО. Рассылка выходит 1 раз в неделю.
e-mail:
  <form name="data1" id="data1" method="post" enctype="multipart/form-data" action="http://www-931.ibm.com/bin/subscriptions/esi/subscribe/RURU/10209/">
                       e-mail:&nbsp;&nbsp;
  <input type="text" size="18" maxlength="55" name="email" value="" />
  <br />
  <input alt="subscribe" type="image" name="butSubmit1" value="Subscribe" src="http://www.ibm.com/i/v14/buttons/ru/ru/subscribe.gif">
                      </form>
  </p>
</div>

<!-- boxes -->
<%
	BoxletVectorRunner boxes=null;

	if (tmpl.getProf().getBooleanProperty("main.3columns"))
		boxes=new BoxletVectorRunner((List) tmpl.getProf().getObjectProperty("main3-1"), tmpl.getCache());
	else
		boxes=new BoxletVectorRunner((List) tmpl.getProf().getObjectProperty("main2"), tmpl.getCache());

	if (request.getParameter("nocache")!=null)
		boxes.setCacheMode(true);

	out.print(boxes.getContent(tmpl.getObjectConfig(), tmpl.getProf()));

%>
<p>
<div class=column style="font-size: smaller">
[<a href="javascript:addSidebar();">Добавить<br>Mozilla SideBar</a>]
</div>
</div>
</td>
<td valign=top>
<%
if (!"black".equals(tmpl.getStyle())) {
//	out.print("<div align=center>");
//	out.print("<a href=\"http://www.centerpress.ru/shop/computer_press/linuxformat/lxf-2007/ref_102196\"><img src=\"http://www.linux.org.ru/adv/linuxformat/lxf2007.gif\"></a>");
        // banners
//        out.print("</div>");
} else {%>
<h1><a href="view-section.jsp?section=1">Новости</a></h1>
<% } %>
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
</td>
<% if (tmpl.getProf().getBooleanProperty("main.3columns")) { %>
<td valign=top width=160>
<div class=column>
<%
boxes=new BoxletVectorRunner((List) tmpl.getProf().getObjectProperty("main3-2"), tmpl.getCache());
if (request.getParameter("nocache")!=null) boxes.setCacheMode(true);

out.print(boxes.getContent(tmpl.getObjectConfig(), tmpl.getProf()));
%>
</div>
</td>
<% } %>
</tr></table>

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
