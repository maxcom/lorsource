<%@ page pageEncoding="koi8-r" contentType="text/html; charset=utf-8"%>
<%@ page import="java.sql.Connection,java.sql.ResultSet,java.sql.Statement,java.util.Date,java.util.List, ru.org.linux.boxlet.BoxletVectorRunner"   buffer="60kb"%>
<%@ page import="ru.org.linux.site.*" %>
<%@ page import="ru.org.linux.util.ServletParameterParser" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<% Template tmpl = Template.getTemplate(request); %>
<jsp:include page="WEB-INF/jsp/head.jsp"/>

<title>LINUX.ORG.RU - Русская информация об ОС Linux</title>
<META NAME="Keywords" CONTENT="linux линукс операционная система документация gnu бесплатное свободное програмное обеспечение софт unix юникс software free documentation operating system новости news">
<META NAME="Description" CONTENT="Все о Linux на русском языке">
<LINK REL="alternate" TITLE="L.O.R RSS" HREF="section-rss.jsp?section=1" TYPE="application/rss+xml">

<%
  response.setDateHeader("Expires", new Date(new Date().getTime() - 20 * 3600 * 1000).getTime());
  response.setDateHeader("Last-Modified", new Date(new Date().getTime() - 2 * 1000).getTime());

%>
<jsp:include page="WEB-INF/jsp/header-main.jsp"/>
<%
  boolean columns3 = tmpl.getProf().getBoolean("main.3columns");

  Connection db = null;
  try {
%>

<div style="clear: both"></div>
<div class="<%= columns3?"newsblog2":"newsblog"%>">
  <div class="<%= columns3?"newsblog-in2":"newsblog-in"%>">

<h1><a href="view-news.jsp?section=1">Новости</a></h1>
<%
  if (tmpl.isModeratorSession()) {
    out.print("<div class=\"nav\"  style=\"border-bottom: none\">");

    if (db==null) {
      db = LorDataSource.getConnection();
    }

    Statement st = db.createStatement();
    ResultSet rs = st.executeQuery("select count(*) from topics,groups,sections where section=sections.id AND sections.moderate and topics.groupid=groups.id and not deleted and not topics.moderate AND postdate>(CURRENT_TIMESTAMP-'1 month'::interval)");

    if (rs.next()) {
      int count = rs.getInt("count");

      out.print("[<a style=\"text-decoration: none\" href=\"view-all.jsp\">Неподтвержденных: " + count + ", ");
    }

    rs.close();

    rs = st.executeQuery("select count(*) from topics,groups where section=1 AND topics.groupid=groups.id and not deleted and not topics.moderate AND postdate>(CURRENT_TIMESTAMP-'1 month'::interval)");

    if (rs.next()) {
      int count = rs.getInt("count");

      out.print(" в том числе новостей: " + count + "</a>]");
    }

    rs.close();

    st.close();

    out.print("</div>");

    db.close(); db=null;
  }

  int offset = 0;
  if (request.getParameter("offset")!=null) {
    offset = new ServletParameterParser(request).getInt("offset");

    if (offset<0) {
	offset = 0;
    }

    if (offset>200) {
      offset=200;
    }
  }

  NewsViewer nv = NewsViewer.getMainpage(tmpl.getConfig(), tmpl.getProf(), offset);

  out.print(ViewerCacher.getViewer(nv, tmpl, false));
%>
<div class="nav">
  <% if (offset<200) { %>
  [<a href="index.jsp?offset=<%=offset+20%>">предыдущие 20</a>]
  <% } %>
  [<a href="add-section.jsp?section=1">добавить новость</a>]
  [<a href="section-rss.jsp?section=1">RSS</a>]
  <% if (offset>20) { %>
    [<a href="index.jsp?offset=<%= (offset-20) %>">следующие 20</a>]
  <% } else if (offset==20) { %>
  [<a href="index.jsp">cледующие 20</a>]
  <% } %>
</div>
</div>
</div>

<%
  BoxletVectorRunner boxes;

  if (tmpl.getProf().getBoolean("main.3columns")) {
    boxes = new BoxletVectorRunner((List) tmpl.getProf().getObject("main3-1"));
  }
  else {
    boxes = new BoxletVectorRunner((List) tmpl.getProf().getObject("main2"));
  }
%>

<div class=column>
<div class=boxlet>
<h2>Вход на сайт</h2>
<% if (!Template.isSessionAuthorized(session)) { %>
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
  if (db==null) {
    db = LorDataSource.getConnection();
  }
  
  User user = User.getUser(db, (String) session.getAttribute("nick"));

  out.print("<br>(статус: " + user.getStatus() + ')');
%><br>
<input type=submit value="Выход"><p>
</form>
* <a href="rules.jsp">Правила</a><br>
* <a href="edit-profile.jsp">Настройки</a><br>&nbsp;<br>
* <a href="show-topics.jsp?nick=<%= user.getNick() %>">Мои темы</a><br>
* <a href="show-comments.jsp?nick=<%= user.getNick() %>">Мои комментарии</a><br>
<% } %>

</div>

<!-- boxes -->
<%

  out.print(boxes.getContent(tmpl.getObjectConfig(), tmpl.getProf()));

%>
</div>
<% if (columns3) { %>
<div class=column2>
<%
  boxes = new BoxletVectorRunner((List) tmpl.getProf().getObject("main3-2"));

  out.print(boxes.getContent(tmpl.getObjectConfig(), tmpl.getProf()));
%>
</div>
<% } %>

<div style="clear: both"></div>

<% } finally {
    if (db!=null) {
      db.close();
    }
  }
%>
<jsp:include page="WEB-INF/jsp/footer-main.jsp"/>
