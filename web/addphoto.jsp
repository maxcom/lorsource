<%@ page contentType="text/html; charset=koi8-r"%>
<%@ page import="java.io.File,java.io.IOException, java.net.URLEncoder, java.sql.Connection" errorPage="/error.jsp"%>
<%@ page import="java.sql.PreparedStatement" %>
<%@ page import="java.util.Random" %>
<%@ page import="java.util.logging.Logger" %>
<%@ page import="ru.org.linux.site.*" %>
<%@ page import="ru.org.linux.util.BadImageException" %>
<%@ page import="ru.org.linux.util.ImageInfo" %>
<% Template tmpl = new Template(request, config, response);
  Logger logger = Logger.getLogger("ru.org.linux");
%>
<%= tmpl.head() %>
<title>Добавление/Изменение фотографии</title>
<%= tmpl.DocumentHeader() %>

<div class=messages>
<div class=nav>

<div class="color1">
  <table width="100%" cellspacing=1 cellpadding=1 border=0><tr class=body>
    <td align=left valign=middle>
      Добавление/Изменение фотографии
    </td>

    <td align=right valign=middle>
      [<a style="text-decoration: none" href="register.jsp?mode=change">Изменение регистрации</a>]
      [<a style="text-decoration: none" href="rules.jsp">Правила форума</a>]
     </td>
    </tr>
 </table>
</div>

</div>
</div>

<h1>Добавление/Изменение фотографии</h1>

<%
  boolean showForm = request.getMethod().equals("GET");
  Exception error = null;

  if (!Template.isSessionAuthorized(session)) {
    throw new AccessViolationException("Not autorized");
  }

  if (request.getMethod().equals("POST")) {
    Connection db = null;

    try {
      String filename = request.getParameter("file");
      Userpic.checkUserpic(filename);

      db = tmpl.getConnection("addphoto");
      User user = new User(db, (String) session.getAttribute("nick"));
      user.checkAnonymous();

      File file = new File(filename);
      String extension = ImageInfo.detectImageType(filename);

      Random random = new Random();

      String photoname;
      File photofile;

      do {
        photoname = Integer.toString(user.getId())+":" +random.nextInt() + "." + extension;
        photofile = new File(tmpl.getObjectConfig().getHTMLPathPrefix() + "/photos", photoname);
      } while (photofile.exists());

      file.renameTo(photofile);

      PreparedStatement pst = db.prepareStatement("UPDATE users SET photo=? WHERE id=?");
      pst.setString(1, photoname);
      pst.setInt(2, user.getId());
      pst.executeUpdate();

      logger.info("Установлена фотография пользователем " + user.getNick());

      response.sendRedirect(tmpl.getRedirectUrl() + "whois.jsp?nick=" + URLEncoder.encode(user.getNick())+"&nocache="+random.nextInt());
    } catch (IOException ex) {
      showForm = true;
      error = ex;
    } catch (BadImageException ex) {
      showForm = true;
      error = ex;
    } catch (UserErrorException ex) {
      showForm = true;
      error = ex;
    } finally {
      if (db != null) {
        db.close();
      }
    }
  }

  if (showForm) {
%>
<p>
Загрузите вашу фотографию в форум. Изображение должно соответствовать <a href="rules.jsp">правилам</a> сайта.
</p>
<p>
  Технические требования к изображению:
  <ul>
    <li>Ширина x Высота: от 50x50 до 150x150 пискелей</li>
    <li>Тип: jpeg, gif, png</li>
    <li>Размер не более 20 Kb</li>
  </ul>
</p>

<form action="addphoto.jsp" method="POST" enctype="multipart/form-data">
<% if (error != null) {
  out.print("<strong>Ошибка! " + error.getMessage() + "</strong><br>");
  //error.printStackTrace(new PrintWriter(out));
}
%>
  <input type="file" name="file"><br>
  <input type="submit" value="Отправить">
</form>
<%
  }
%>

<%=	tmpl.DocumentFooter() %>
