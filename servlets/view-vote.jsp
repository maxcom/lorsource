<%@ page contentType="text/html; charset=koi8-r"%>
<%@ page import="java.net.URLEncoder,java.sql.Connection,java.sql.ResultSet,java.sql.Statement,java.util.Date" errorPage="error.jsp" buffer="200kb"%>
<%@ page import="java.util.Iterator"%>
<%@ page import="java.util.List"%>
<%@ page import="ru.org.linux.site.*"%>
<%@ page import="ru.org.linux.util.HTMLFormatter"%>
<%@ page import="ru.org.linux.util.ImageInfo"%>
<% Template tmpl = new Template(request, config, response); %>
<%= tmpl.head() %>
<%
  Connection db=null;
  try {
    response.setDateHeader("Expires", new Date(new Date().getTime()-20*3600*1000).getTime());
    response.setDateHeader("Last-Modified", new Date(new Date().getTime()-120*1000).getTime());

    if (request.getParameter("vote")==null)
      throw new MissingParameterException("vote");

    int voteid = Integer.parseInt(request.getParameter("vote"));

    boolean showDeleted =request.getParameter("deleted")!=null;
    if (showDeleted && !"POST".equals(request.getMethod())) {
      response.setHeader("Location", tmpl.getRedirectUrl() + "view-vote.jsp?vote="+voteid);
      response.setStatus(HttpServletResponse.SC_MOVED_TEMPORARILY);

      showDeleted = false;
    }

    if (showDeleted) {
      if (!tmpl.isSessionAuthorized(session)) {
        throw new BadInputException("Вы уже вышли из системы");
      }
    }

    db = tmpl.getConnection("view-vote");

    Poll poll = new Poll(db, voteid);

    String title=poll.getTitle();
    int topic=poll.getTopicId();
    out.print("<title>Результаты опроса: "+title+"</title>");
%>
<%= tmpl.DocumentHeader() %>

<div class=messages>

<h1><%= title %></h1>
<%
  Statement st = db.createStatement();

  int max = poll.getMaxVote(db);

  List vars = poll.getPollVariants(db, Poll.ORDER_VOTES);

  out.print("<table width=\"100%\" cellspacing=0 cellpadding=0 border=0>");
  out.print("<tr class=body><td>");
  out.print("<div class=msg>");

  out.print("<h2>" + title + "</h2>");

  out.print("<table>");

  ImageInfo info = new ImageInfo(tmpl.getObjectConfig().getHTMLPathPrefix() + tmpl.getStyle() + "/img/votes.gif");

  int highlight = 0;
  if (request.getParameter("highlight") != null)
    highlight = Integer.parseInt(request.getParameter("highlight"));

  for (Iterator iter = vars.iterator(); iter.hasNext();) {
    PollVariant var = (PollVariant) iter.next();

    out.print("<tr><td>");
    int id = var.getId();
    int votes = var.getVotes();

    if (id == highlight) {
      out.print("<b>");
    }

    out.print(HTMLFormatter.htmlSpecialChars(var.getLabel()));

    if (id == highlight) {
      out.print("</b>");
    }

    out.print("</td><td>" + votes + "</td><td>");

    for (int i = 0; i < 20 * votes / max; i++) {
      out.print("<img src=\"" + tmpl.getStyle() + "/img/votes.gif\" alt=\"*\" " + info.getCode() + '>');
    }

    out.print("</td></tr>");
  }

  out.print("</table>");

  out.print("<p>");

  User poster = new User(db, poll.getUserid());

  out.print(User.getUserInfoLine(tmpl, poster, poll.getPostdate()));

  int commitby = poll.getCommitby();

  if (commitby != 0) {
    User commiter = new User(db, commitby);

    out.print("<br>");
    out.print(commiter.getCommitInfoLine(poll.getPostdate(), poll.getCommitDate()));
  }

  out.print("</div></td></tr>");
  out.print("</table><p>");

// comments

  String returnurl = "view-vote.jsp?vote=" + voteid;

  boolean expired = Poll.getCurrentPollId(db) != voteid;

  out.print("<div class=comment>");

  if (tmpl.getProf().getBoolean("sortwarning")) {
    out.print("<div class=nav><div class=color1><table width=\"100%\" cellspacing=1 cellpadding=0 border=0><tr class=body><td align=\"center\">");

    if (tmpl.getProf().getBoolean("newfirst")) {
      out.print("сообщения отсортированы в порядке убывания даты их написания");
    } else {
      out.print("сообщения отсортированы в порядке возрастания даты их написания");
    }

    out.print("</td></tr></table></div></div>");
  }

  String order = tmpl.getProf().getBoolean("newfirst") ? "DESC" : "ASC";

  String delq = showDeleted ? "" : " AND NOT deleted ";

  Statement scm = db.createStatement();
  ResultSet cm = scm.executeQuery("SELECT comments.title, topic, postdate, nick, score, max_score, comments.id as msgid, replyto, photo, " + (expired ? "'t'" : "'f'") + " as expired, deleted, message FROM comments, users, msgbase WHERE msgbase.id=comments.id AND comments.userid=users.id AND comments.topic=" + topic + ' ' + delq + " ORDER BY msgid " + order);

  String urladd = "&return=" + URLEncoder.encode(returnurl);

  CommentViewer cv = new CommentViewer(tmpl, cm, db, urladd);
  out.print(cv.showAll());

  out.print("</div>");
%>
</div>
</div>

<p>
<% if (tmpl.isSessionAuthorized(session) && !expired && !tmpl.isSearchMode() && !showDeleted) { %>
<hr>
<form action="view-vote.jsp" method=POST>
<input type=hidden name=vote value=<%= voteid %>>
<input type=hidden name=deleted value=1>
<input type=submit value="Показать удаленные комментарии">
</form>
<hr>
<% } %>

<% if (!expired) { %>

<h2><a name=rep>Добавить сообщение:</a></h2>

<% if (tmpl.getProf().getBoolean("showinfo") && !tmpl.isSessionAuthorized(session)) { %>
  <font size=2>Чтобы просто поместить сообщение, используйте login `anonymous',
  без пароля. Если вы собираетесь активно участвовать в форуме,
  помещать новости на главную страницу,
  <a href="register.jsp">зарегистрируйтесь</a></font>.
  <p>

<% } %>


<font size=2><strong>Внимание!</strong> Перед написанием комментария ознакомьтесь с
<a href="rules.jsp">правилами</a> сайта.</font><p>


<form method=POST action="add_comment.jsp">
<input type="hidden" name="session" value="<%= HTMLFormatter.htmlSpecialChars(session.getId()) %>">

<% if (session==null || session.getAttribute("login")==null || !((Boolean) session.getAttribute("login")).booleanValue()) { %>
Имя:
<input type=text name=nick value="<%= tmpl.getCookie("NickCookie","anonymous") %>" size=40><br>
Пароль:
<input type=password name=password size=40><br>
<% } %>
<% out.print("<input type=hidden name=voteid value="+voteid+ '>'); %>
<input type=hidden name=return value="<%= returnurl %>">
<input type=hidden name=topic value=<%= topic %>>
Заглавие:
<input type=text name=title size=40 value="Re: <%= title %>"><br>
Сообщение:<br>
<textarea name=msg cols=70 rows=20></textarea><br>

<select name=mode>
<option value=tex>TeX paragraphs
<option value=html>Ignore line breaks
<option value=ntobr>User line breaks
<option value=pre>Preformatted text
</select>

<select name=autourl>
<option value=1>Auto URL
<option value=0>No Auto URL
</select>

<select name=texttype>
<option value=0>Plain text
<option value=1>HTML (limited)
</select><br>
  
<%
  if (!Template.isSessionAuthorized(session)) {
    out.print("<p><img src=\"/jcaptcha.jsp\"><input type='text' name='j_captcha_response' value=''>");
  }
%>

<input type=submit value="Post/Отправить">
</form>

<% } %>

<%
   st.close();
  } finally {
    if (db!=null)  db.close();
  }
%>
<%= tmpl.DocumentFooter() %>
