<%@ page import="java.net.URLEncoder" %>
<%@ page import="java.sql.Connection" %>
<%@ page import="java.sql.Statement" %>
<%@ page import="java.util.Random" %>
<%@ page import="java.util.logging.Logger" %>
<%@ page import="ru.org.linux.site.*" %>
<%@ page import="ru.org.linux.util.HTMLFormatter" %>
<%@ page import="ru.org.linux.util.ServletParameterParser" %>
<%@ page pageEncoding="koi8-r" contentType="text/html;charset=utf-8" language="java"   %>
<% Template tmpl = new Template(request, config.getServletContext(), response);
  Logger logger = Logger.getLogger("ru.org.linux");
%>
<%= tmpl.getHead() %>
<title>usermod</title>
<jsp:include page="WEB-INF/jsp/header.jsp"/>

<%
  if (!tmpl.isModeratorSession()) {
    throw new IllegalAccessException("Not authorized");
  }

  String action = new ServletParameterParser(request).getString("action");
  int id = new ServletParameterParser(request).getInt("id");

  if (!request.getMethod().equals("POST")) {
    throw new IllegalAccessException("Invalid method");
  }

  Connection db = null;

  try {
    db = LorDataSource.getConnection();
    db.setAutoCommit(false);

    Statement st = db.createStatement();

    User user = User.getUser(db, id);

    User moderator = User.getUser(db, (String) session.getValue("nick"));

    boolean redirect = true;

    if (action.equals("block") || action.equals("block-n-delete-comments")) {
      if (!user.isBlockable()) {
        throw new AccessViolationException("Пользователя " + user.getNick() + " нельзя заблокировать");
      }

      user.block(db);
      st.executeUpdate("UPDATE users SET blocked='t' WHERE id=" + id);
      logger.info("User " + user.getNick() + " blocked by " + session.getValue("nick"));

      if (action.equals("block-n-delete-comments")) {
        out.print(user.deleteAllComments(db, moderator));
        redirect = false;
      }
    } else if (action.equals("toggle_corrector")) {
      if (user.getScore()<300) {
        throw new AccessViolationException("Пользователя " + user.getNick() + " нельзя сделать корректором (score<300)");
      }

      if (user.canCorrect()) {
        st.executeUpdate("UPDATE users SET corrector='f' WHERE id=" + id);
      } else {
        st.executeUpdate("UPDATE users SET corrector='t' WHERE id=" + id);
      }
    } else if (action.equals("unblock")) {
      if (!user.isBlockable()) {
        throw new AccessViolationException("Пользователя " + user.getNick() + " нельзя разблокировать");
      }

      st.executeUpdate("UPDATE users SET blocked='f' WHERE id=" + id);
      logger.info("User " + user.getNick() + " unblocked by " + session.getValue("nick"));
    } else if (action.equals("remove_userpic")) {
      if (user.canModerate()) {
        throw new AccessViolationException("Пользователю " + user.getNick() + " нельзя удалить картинку");
      }

      if (user.getPhoto() == null) {
        throw new AccessViolationException("Пользователь " + user.getNick() + " картинки не имеет");
      }

      st.executeUpdate("UPDATE users SET photo=null WHERE id=" + id);
      st.executeUpdate("UPDATE users SET score=score-10 WHERE id=" + id);
      logger.info("Clearing " + user.getNick() + " userpic by " + session.getValue("nick"));
    } else if (action.equals("remove_userinfo")) {
      if (user.canModerate()) {
        throw new AccessViolationException("Пользователю " + user.getNick() + " нельзя удалить сведения");
      }

      tmpl.getObjectConfig().getStorage().updateMessage("userinfo", String.valueOf(id), "");

      st.executeUpdate("UPDATE users SET score=score-10 WHERE id=" + id);
      logger.info("Clearing " + user.getNick() + " userinfo");
    } else {
      throw new UserErrorException("Invalid action=" + HTMLFormatter.htmlSpecialChars(action));
    }

    if (redirect) {
      Random random = new Random();

      response.setHeader("Location", tmpl.getMainUrl() + "whois.jsp?nick=" + URLEncoder.encode(user.getNick()) + "&nocache=" + random.nextInt());
      response.setStatus(HttpServletResponse.SC_MOVED_TEMPORARILY);
    }

    db.commit();
  } finally {
    if (db != null) {
      db.close();
    }
  }

%>
<jsp:include page="WEB-INF/jsp/footer.jsp"/>

