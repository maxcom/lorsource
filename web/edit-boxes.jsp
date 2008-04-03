<%@ page pageEncoding="koi8-r" contentType="text/html; charset=utf-8"%>
<%@ page import="java.net.URLEncoder,java.sql.Connection,java.sql.ResultSet,java.sql.Statement, java.util.Date, java.util.List, javax.servlet.http.Cookie, javax.servlet.http.HttpServletResponse"  %>
<%@ page import="ru.org.linux.boxlet.BoxletVectorRunner" %>
<%@ page import="ru.org.linux.site.*" %>
<%@ page import="ru.org.linux.util.ProfileHashtable" %>
<%@ page import="ru.org.linux.util.ServletParameterParser" %>
<%@ page import="ru.org.linux.util.StringUtil" %>
<% Template tmpl = Template.getTemplate(request);%>
<jsp:include page="WEB-INF/jsp/head.jsp"/>

        <title>Конструктор страницы</title>
<jsp:include page="WEB-INF/jsp/header.jsp"/>

<h1>Конструктор страницы</h1>
<% if (tmpl.isUsingDefaultProfile() || tmpl.getProfileName().charAt(0) == '_') {
  throw new AccessViolationException("нельзя изменить системный профиль; создайте сначала свой");
}
%>
<% if (request.getParameter("mode")==null) { %>
При помощи этого инструмента вы можете составить для себя свою собственную
страничку, содержащую только необходимую вам информацию.
<% } else { %>
[<a href="edit-boxes.jsp">В&nbsp;начало</a>] [<a href="edit-profile.jsp">Настройки&nbsp;профиля</a>] [<a href="/">На&nbsp;главную&nbsp;страницу</a>]
<% } %>
<%
  List main2 = (List) tmpl.getProf().getObject("main2");
  List main31 = (List) tmpl.getProf().getObject("main3-1");
  List main32 = (List) tmpl.getProf().getObject("main3-2");
  List current = null;
  String cname = null;
  String tag = request.getParameter("tag");
  if (tag != null) {
    if ("31".equals(tag)) {
      current = main31;
      cname = "main3-1";
    } else if ("32".equals(tag)) {
      current = main32;
      cname = "main3-2";
    } else if ("2".equals(tag)) {
      current = main2;
      cname = "main2";
    }
  }

%>
<%
  boolean showlist = true;
  boolean save = false;
  if (request.getParameter("mode") != null && "remove".equals(request.getParameter("mode"))) {
    showlist = false;

    out.print("<form method=POST action=\"edit-boxes.jsp\">");
    out.print("<input type=hidden name=mode value=remove2>");
    out.print("<input type=hidden name=tag value=" + tag + '>');
    int id = Integer.parseInt(request.getParameter("id"));
    out.print("<input type=hidden name=id value=" + id + '>');
    out.print("Пароль <input type=password name=password><br>");
    out.print("<input type=submit value=\"Remove/Удалить\">");
    out.print("</form>");
  } else
  if (request.getParameter("mode") != null && "remove2".equals(request.getParameter("mode"))) {
    Connection db = null;
    try {
      db = LorDataSource.getConnection();
      User user = User.getUser(db, tmpl.getProfileName());
      user.checkAnonymous();
      user.checkPassword(request.getParameter("password"));
    } finally {
      if (db != null) {
        db.close();
      }
    }


    int id = Integer.parseInt(request.getParameter("id"));
    current.remove(id);
    save = true;
  } else if (request.getParameter("mode") != null && "add".equals(request.getParameter("mode"))) {
    showlist = false;
    List boxlist = (List) tmpl.getProf().getObject("boxlist");

    out.print("<form method=POST action=\"edit-boxes.jsp\">");
    out.print("<input type=hidden name=mode value=add2>");
    out.print("<input type=hidden name=tag value=" + tag + '>');
    if (request.getParameter("id") != null) {
      int id = Integer.parseInt(request.getParameter("id"));
      out.print("<input type=hidden name=id value=" + id + '>');
    }
    for (Object aBoxlist : boxlist) {
      out.print("<input type=radio name=box value=\"" + URLEncoder.encode((String) aBoxlist) + "\">" + aBoxlist + "<br>");
    }
    out.print("Пароль <input type=password name=password><br>");
    out.print("<input type=submit value=\"Add/Добавить\">");
    out.print("</form>");
  } else if (request.getParameter("mode") != null && "add2".equals(request.getParameter("mode"))) {
    Connection db = null;
    try {
      db = LorDataSource.getConnection();
      User user = User.getUser(db, tmpl.getProfileName());
      user.checkAnonymous();
      user.checkPassword(request.getParameter("password"));
    } finally {
      if (db != null) {
        db.close();
      }
    }

    if (request.getParameter("box") == null) {
      throw new MissingParameterException("box");
    }

    if (request.getParameter("id") != null) {
      int id = Integer.parseInt(request.getParameter("id"));
      current.add(id, request.getParameter("box"));
    } else {
      current.add(request.getParameter("box"));
    }
    save = true;
  }
%>

<% // save
  if (save) {
    tmpl.getProf().setObject(cname, current);
    tmpl.writeProfile(tmpl.getProfileName());
  }

%>

<% if (showlist) { %>
<table><tr><td valign=top>
<%
  if (tmpl.getProf().getBoolean("main.3columns")) {
    out.print("<h3>Левая колонка</h3>");
    out.print("<div class=column>");
    out.print(new BoxletVectorRunner(main31).getEditContent(tmpl.getObjectConfig(), tmpl.getProf(), "31"));
    out.print("</div>");
  } else {
    out.print("<h3>Левая колонка</h3>");
    out.print("<div class=column>");
    out.print(new BoxletVectorRunner(main2).getEditContent(tmpl.getObjectConfig(), tmpl.getProf(), "2"));
    out.print("</div>");
  }
%>
</td><td valign=top>
<h1>Редактирование</h1>
Чтобы добавить или удалить Boxlet, выберете соответствующий пункт в меню
редактирования внизу каждой коробочки.
</td>
<%
  if (tmpl.getProf().getBoolean("main.3columns")) {
    out.print("<td valign=top>");
    out.print("<h3>Правая колонка</h3>");
    out.print("<div class=column>");
    out.print(new BoxletVectorRunner(main32).getEditContent(tmpl.getObjectConfig(), tmpl.getProf(), "32"));
    out.print("</div>");
    out.print("</td>");
  }

  //tmpl.getObjectConfig().SQLclose();
%>
</tr></table>
<% } %>


<jsp:include page="WEB-INF/jsp/footer.jsp"/>
