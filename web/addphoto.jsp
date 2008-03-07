<%@ page pageEncoding="koi8-r" contentType="text/html; charset=utf-8"%>
<%@ page import="java.io.File, java.io.IOException, java.net.URLEncoder, java.sql.Connection, java.sql.PreparedStatement"  %>
<%@ page import="java.sql.ResultSet" %>
<%@ page import="java.sql.Statement" %>
<%@ page import="java.util.Date" %>
<%@ page import="java.util.List" %>
<%@ page import="java.util.Properties" %>
<%@ page import="java.util.Random" %>
<%@ page import="java.util.logging.Logger, javax.mail.Session" %>
<%@ page import="javax.mail.Transport" %>
<%@ page import="javax.mail.internet.InternetAddress" %>
<%@ page import="javax.mail.internet.MimeMessage" %>
<%@ page import="javax.servlet.http.Cookie" %>
<%@ page import="javax.servlet.http.HttpServletResponse" %>
<%@ page import="org.apache.commons.fileupload.FileItem" %>
<%@ page import="org.apache.commons.fileupload.disk.DiskFileItemFactory" %>
<%@ page import="org.apache.commons.fileupload.servlet.ServletFileUpload" %>
<%@ page import="org.apache.commons.lang.StringUtils" %>
<%@ page import="ru.org.linux.boxlet.BoxletVectorRunner" %>
<%@ page import="ru.org.linux.site.*" %>
<%@ page import="ru.org.linux.storage.StorageNotFoundException" %>
<%@ page import="ru.org.linux.util.*" %>
<% Template tmpl = new Template(request, config.getServletContext(), response);
  Logger logger = Logger.getLogger("ru.org.linux");
%>
<%= tmpl.getHead() %>
<title>Добавление/Изменение фотографии</title>
<jsp:include page="WEB-INF/jsp/header.jsp"/>


  <table class=nav><tr>
    <td align=left valign=middle>
      Добавление/Изменение фотографии
    </td>

    <td align=right valign=middle>
      [<a style="text-decoration: none" href="register.jsp?mode=change">Изменение регистрации</a>]
      [<a style="text-decoration: none" href="rules.jsp">Правила форума</a>]
     </td>
    </tr>
 </table>

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
      String filename = "";
      if (!ServletFileUpload.isMultipartContent(request) || request.getParameter("file") != null) {
        filename = request.getParameter("file");
      } else {
        // Load file from multipart request
        File rep = new File(tmpl.getObjectConfig().getPathPrefix() + "/linux-storage/tmp/");
        // Create a factory for disk-based file items
        DiskFileItemFactory factory = new DiskFileItemFactory();
        // Set factory constraints
        factory.setSizeThreshold(500000);
        factory.setRepository(rep);
        // Create a new file upload handler
        ServletFileUpload upload = new ServletFileUpload(factory);
        // Set overall request size constraint
        upload.setSizeMax(60000);
        // Parse the request
        List items = upload.parseRequest(request);
        // Process the uploaded items
        for (Object item1 : items) {
          FileItem item = (FileItem) item1;
          if (!item.isFormField()) {
            String fieldName = item.getFieldName();
            String fileName = item.getName();
            if (fieldName.compareToIgnoreCase("file") == 0 && fileName != null && !"".equals(fileName)) {
              filename = tmpl.getObjectConfig().getPathPrefix() + "/linux-storage/tmp/" + fileName;
              File uploadedFile = new File(filename);
              if (uploadedFile != null && (uploadedFile.canWrite() || uploadedFile.createNewFile())) {
                item.write(uploadedFile);
              } else {
                throw new BadInputException("Ошибка сохранения");
              }
            } else {
              throw new BadInputException("Ошибка загрузки");
            }
          } else {
            // Form field
          }
        } // while
      }
      Userpic.checkUserpic(filename);

      db = LorDataSource.getConnection();
      User user = User.getUser(db, (String) session.getAttribute("nick"));
      user.checkAnonymous();

      File file = new File(filename);
      String extension = ImageInfo.detectImageType(filename);

      Random random = new Random();

      String photoname;
      File photofile;

      do {
        photoname = Integer.toString(user.getId()) + ":" + random.nextInt() + "." + extension;
        photofile = new File(tmpl.getObjectConfig().getHTMLPathPrefix() + "/photos", photoname);
      } while (photofile.exists());

      if (!file.renameTo(photofile)) {
        logger.warning("Can't move photo to "+photofile);
        throw new ScriptErrorException("Can't move photo: internal error");
      }

      PreparedStatement pst = db.prepareStatement("UPDATE users SET photo=? WHERE id=?");
      pst.setString(1, photoname);
      pst.setInt(2, user.getId());
      pst.executeUpdate();

      logger.info("Установлена фотография пользователем " + user.getNick());

      response.sendRedirect(tmpl.getMainUrl() + "whois.jsp?nick=" + URLEncoder.encode(user.getNick()) + "&nocache=" + random.nextInt());
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
    <li>Размер не более 30 Kb</li>
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

<jsp:include page="WEB-INF/jsp/footer.jsp"/>
