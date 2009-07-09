<%@ page contentType="text/html; charset=utf-8"%>
<%@ page import="java.sql.Connection,java.sql.Statement,java.util.Random,java.util.logging.Logger,javax.servlet.http.HttpServletResponse,ru.org.linux.site.*"  %>
<%@ page import="ru.org.linux.util.BadURLException"%>
<%@ page import="ru.org.linux.util.HTMLFormatter" %>
<%@ page import="ru.org.linux.util.ServletParameterParser" %>
<%@ taglib tagdir="/WEB-INF/tags" prefix="lor" %>
<%--
  ~ Copyright 1998-2009 Linux.org.ru
  ~    Licensed under the Apache License, Version 2.0 (the "License");
  ~    you may not use this file except in compliance with the License.
  ~    You may obtain a copy of the License at
  ~
  ~        http://www.apache.org/licenses/LICENSE-2.0
  ~
  ~    Unless required by applicable law or agreed to in writing, software
  ~    distributed under the License is distributed on an "AS IS" BASIS,
  ~    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
  ~    See the License for the specific language governing permissions and
  ~    limitations under the License.
  --%>

<% Template tmpl = Template.getTemplate(request);%>
<%
  Logger logger = Logger.getLogger("ru.org.linux");

  int topicId = new ServletParameterParser(request).getInt("topic");
  boolean showform = request.getParameter("msg") == null;
  boolean preview = request.getParameter("preview") != null;

  if (!"POST".equals(request.getMethod()) || preview) {
    showform = true;
  }
%>
<jsp:include page="WEB-INF/jsp/head.jsp"/>

<%
  Connection db = null;
  try {
%>

<%
  Exception error = null;
  String mode = tmpl.getFormatMode();
  boolean autourl = true;
  Comment comment = null;
  if (!showform || preview) { // add2
    mode = new ServletParameterParser(request).getString("mode");
    autourl = new ServletParameterParser(request).getBoolean("autourl");
    String msg = request.getParameter("msg");

    int replyto = 0;

    if (request.getParameter("replyto") != null) {
      replyto = new ServletParameterParser(request).getInt("replyto");
    }

    if (!preview && !session.getId().equals(request.getParameter("session"))) {
      logger.info("Flood protection (session variable differs: " + session.getId() + ") " + request.getRemoteAddr());
      throw new BadInputException("сбой добавления");
    }

    String title = request.getParameter("title");

    if (title == null) {
      title = "";
    }

    title = HTMLFormatter.htmlSpecialChars(title);

    int maxlength = 80; // TODO: remove this hack
    HTMLFormatter form = new HTMLFormatter(msg);
    form.setMaxLength(maxlength);
    if ("pre".equals(mode)) {
      form.enablePreformatMode();
    }
    if (autourl) {
      form.enableUrlHighLightMode();
    }
    if ("ntobrq".equals(mode)) {
      form.enableNewLineMode();
      form.enableQuoting();      
    }
    if ("ntobr".equals(mode)) {
      form.enableNewLineMode();
    }
    if ("tex".equals(mode)) {
      form.enableTexNewLineMode();
    }
    if ("quot".equals(mode)) {
      form.enableTexNewLineMode();
      form.enableQuoting();
    }

    msg = form.process();

    comment = new Comment(replyto, title, msg, topicId, 0, request.getHeader("user-agent"), request.getRemoteAddr());

    try {
      // prechecks is over
      db = LorDataSource.getConnection();
      db.setAutoCommit(false);

      IPBlockInfo.checkBlockIP(db, request.getRemoteAddr());

      User user;

      if (!Template.isSessionAuthorized(session)) {
        if (request.getParameter("nick") == null) {
          throw new BadInputException("Вы уже вышли из системы");
        }
        user = User.getUser(db, request.getParameter("nick"));
        user.checkPassword(request.getParameter("password"));
      } else {
        user = User.getUser(db, (String) session.getAttribute("nick"));
      }

      user.checkBlocked();

      comment.setAuthor(user.getId());

      if ("".equals(title)) {
        throw new BadInputException("заголовок сообщения не может быть пустым");
      }

      if (title.length()>Comment.TITLE_LENGTH) {
        throw new BadInputException("заголовок превышает "+Comment.TITLE_LENGTH+" символов");
      }

      if ("".equals(msg)) {
        throw new BadInputException("комментарий не может быть пустым");
      }

      if (!preview && !Template.isSessionAuthorized(session)) {
        CaptchaSingleton.checkCaptcha(session, request);
      }

      if (user.isAnonymous()) {
        if (msg.length() > 4096) {
          throw new BadInputException("Слишком большое сообщение");
        }
      } else {
        if (msg.length() > 8192) {
          throw new BadInputException("Слишком большое сообщение");
        }
      }

      if (replyto != 0) {
        Comment reply = new Comment(db, replyto);
        if (reply.isDeleted()) {
          throw new AccessViolationException("Комментарий был удален");
        }

        if (reply.getTopic() != topicId) {
          throw new AccessViolationException("Некорректная тема?!");
        }
      }

      Statement st = db.createStatement();
      Message topic = new Message(db, topicId);

      topic.checkCommentsAllowed(db, user);

      if (!preview) {
        DupeProtector.getInstance().checkDuplication(request.getRemoteAddr(),user.getScore()>100);

        int msgid = comment.saveNewMessage(db, request.getRemoteAddr(), request.getHeader("user-agent"));

        String logmessage = "Написан комментарий " + msgid + " ip:" + request.getRemoteAddr();
        if (request.getHeader("X-Forwarded-For") != null) {
          logmessage = logmessage + " XFF:" + request.getHeader(("X-Forwarded-For"));
        }

        logger.info(logmessage);

        topic = new Message(db, topicId); // update lastmod

        CommentList commentList = CommentList.getCommentList(db, topic, false);
        Comment newComment = commentList.getNode(msgid).getComment();
        int pageNum = commentList.getCommentPage(newComment, tmpl);

        Random random = new Random();

        String returnUrl;

        if (pageNum > 0) {
          returnUrl = "view-message.jsp?msgid=" + topicId + "&page=" + pageNum + "&nocache=" + random.nextInt() + "#comment-" + msgid;
        } else {
          returnUrl = "view-message.jsp?msgid=" + topicId + "&nocache=" + random.nextInt() + "#comment-" + msgid;
        }

        response.setHeader("Location", tmpl.getMainUrl() + returnUrl);
        response.setStatus(HttpServletResponse.SC_MOVED_TEMPORARILY);

        db.commit();
        st.close();

%>
<title>Добавление сообщения прошло успешно</title>
<jsp:include page="WEB-INF/jsp/header.jsp"/>
<p>Сообщение помещено успешно

<p><a href="<%= returnUrl %>">Возврат</a>

<p><b>Пожалуйста, не нажимайте кнопку "ReLoad" вашего броузера на этой страничке и не возвращайтесь на нее по средством кнопки Back</b>
<%		
			} 
} catch (UserErrorException e) {
	error=e;
	showform=true;
	if (db!=null) {
		db.rollback();
		db.setAutoCommit(true);
	}
} catch (UserNotFoundException e) {
	error=e;
	showform=true;
	if (db!=null) {
		db.rollback();
		db.setAutoCommit(true);
	}
} catch (BadURLException e) {
	error=e;
	showform=true;
	if (db!=null) {
		db.rollback();
		db.setAutoCommit(true);
	}
}
%>

<% }

if (showform) { // show form
  if (db==null) {
    db = LorDataSource.getConnection();
  }

  Message topic = new Message(db, topicId);

  if (topic.isExpired()) {
    throw new AccessViolationException("нельзя добавлять в устаревшие темы");
  }

  if (topic.isDeleted()) {
    throw new AccessViolationException("нельзя добавлять в удаленные темы");
  }

  int postscore = topic.getPostScore();
%>

<title>Добавить сообщение</title>
<jsp:include page="WEB-INF/jsp/header.jsp"/>

<% if (error==null) { %>
<h1>Добавить комментарий</h1>
<% } else { out.println("<h1>Ошибка: "+error.getMessage()+"</h1>"); } %>

<%--<% if (tmpl.getProf().getBoolean("showinfo") && !Template.isSessionAuthorized(session)) { %>--%>
<%--<font size=2>Чтобы просто поместить сообщение, используйте login `anonymous',--%>
<%--без пароля. Если вы собираетесь активно участвовать в форуме,--%>
<%--помещать новости на главную страницу,--%>
<%--<a href="register.jsp">зарегистрируйтесь</a></font>.--%>
<%--<p>--%>

<%--<% } %>--%>
<font size=2><strong>Внимание!</strong> Перед написанием комментария ознакомьтесь с
<a href="rules.jsp">правилами</a> сайта.</font><p>

<%
  out.print(Message.getPostScoreInfo(postscore));
%>

<form method=POST action="add_comment.jsp">
  <input type="hidden" name="session" value="<%= HTMLFormatter.htmlSpecialChars(session.getId()) %>">
<% if (!Template.isSessionAuthorized(session)) { %>
Имя:
<% if (request.getParameter("nick") != null) { %>
       <input type='text' name='nick' value="<%= request.getParameter("nick") %>" size=40><br><%
       } else { %>
       <input type='text' name='nick' value="<%= "anonymous" %>" size=40><br>
<% } %>
Пароль:
<input type=password name=password size=40><br>
<% } %>
<input type=hidden name=topic value="<%= topicId %>">

<% if (request.getParameter("return")!=null) { %>
<input type=hidden name=return value="<%= HTMLFormatter.htmlSpecialChars(request.getParameter("return")) %>">
<% } %>
<%
  String title = "";

  if (request.getParameter("replyto")!=null) {
    int replyto=Integer.parseInt(request.getParameter("replyto"));
%>
<input type=hidden name=replyto value="<%= replyto %>">
<%
    Comment onComment = new Comment(db, replyto);

    if (onComment.isDeleted()) {
      throw new MessageNotFoundException(replyto);
    }

    title = onComment.getTitle();
    if (!title.startsWith("Re:")) {
      title = "Re: " + title;
    }

    out.print("<div class=messages>");
    CommentView view = new CommentView();
    out.print(view.printMessage(onComment, tmpl, db, null, false, tmpl.isModeratorSession(), Template.getNick(session), false));
    out.print("</div>");
  }

  if (request.getParameter("title") != null) {
    title = HTMLFormatter.htmlSpecialChars(request.getParameter("title"));
  }

  if (preview && comment!=null) {
    out.print("<p><b>Ваше сообщение</b></p>");
    out.print("<div class=messages>");
    CommentView view = new CommentView();
    out.print(view.printMessage(comment, tmpl, db, null, false, tmpl.isModeratorSession(), Template.getNick(session), false));
    out.print("</div>");
  }
%>
Заглавие:
<input type=text name=title size=40 value="<%= title %>"><br>

Сообщение:<br>
<font size=2>(В режиме <i>Tex paragraphs</i> игнорируются переносы строк.<br> Пустая строка (два раза Enter) начинает новый абзац.<br> Знак '&gt;' в начале абзаца выделяет абзац курсивом цитирования)</font><br>
<textarea name="msg" cols="70" rows="20"><%= request.getParameter("msg")==null?"":HTMLFormatter.htmlSpecialChars(request.getParameter("msg")) %></textarea><br>

<select name=mode>
<option value=ntobrq <%= (mode!=null && "ntobrq".equals(mode))?"selected":""%> >User line breaks w/quoting
<option value=quot <%= (mode!=null && "quot".equals(mode))?"selected":""%> >TeX paragraphs w/quoting
<option value=tex <%= (mode!=null && "tex".equals(mode))?"selected":""%> >TeX paragraphs w/o quoting
<option value=ntobr <%= (mode!=null && "ntobr".equals(mode))?"selected":""%> >User line break w/o quoting
<option value=html <%= (mode!=null && "html".equals(mode))?"selected":""%> >Ignore line breaks
<option value=pre <%= (mode!=null && "pre".equals(mode))?"selected":""%> >Preformatted text
</select>

<select name=autourl>
<option value=1 <%= (preview && autourl)?"selected":""%> >Auto URL
<option value=0 <%= (preview && !autourl)?"selected":""%> >No Auto URL
</select>

<input type=hidden value=0 name=texttype>

<br>

<%
  out.print(Message.getPostScoreInfo(postscore));
%>

<br>
  <lor:captcha/>
<input type=submit value="Поместить">
<input type=submit name=preview value="Предпросмотр">

</form>
<%
   }
} finally {
  if (db!=null) {
    db.close();
  }
}
%>
<jsp:include page="WEB-INF/jsp/footer.jsp"/>
