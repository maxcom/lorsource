<%@ page pageEncoding="koi8-r" contentType="text/html; charset=utf-8"%>
<%@ page import="java.io.PrintWriter,java.io.StringWriter" isErrorPage="true" %>
<%@ page import="java.util.Date"%>
<%@ page import="java.util.Enumeration"%>
<%@ page import="java.util.Properties"%>
<%@ page import="java.util.logging.Logger"%>
<%@ page import="javax.mail.Session"%>
<%@ page import="javax.mail.Transport"%>
<%@ page import="javax.mail.internet.InternetAddress"%>
<%@ page import="javax.mail.internet.MimeMessage"%>
<%@ page import="ru.org.linux.site.MessageNotFoundException"%>
<%@ page import="ru.org.linux.site.ScriptErrorException"%>
<%@ page import="ru.org.linux.site.Template"%>
<%@ page import="ru.org.linux.site.UserErrorException"%>
<%@ page import="ru.org.linux.util.HTMLFormatter"%>
<%@ page import="ru.org.linux.util.ServletParameterException" %>
<%@ page import="ru.org.linux.util.StringUtil" %>
<% Template tmpl = Template.getTemplate(request);
  Logger logger = Logger.getLogger("ru.org.linux");

  if (exception==null) {
    exception = (Throwable) request.getAttribute("exception");
  }

  if (exception instanceof ServletException) {
    Throwable ex = ((ServletException) exception).getRootCause();
    if (ex!=null) {
      exception = ex;
    }
  }

  if (exception instanceof MessageNotFoundException) {
    response.setStatus(404);
  } else {
    response.setStatus(500);    
  }
%>
<jsp:include page="/WEB-INF/jsp/head.jsp"/>

<title>Ошибка: <%= HTMLFormatter.htmlSpecialChars(exception.getClass().getName()) %></title>
<jsp:include page="/WEB-INF/jsp/header.jsp"/>
<h1><%=exception.getMessage()==null?HTMLFormatter.htmlSpecialChars(exception.getClass().getName()):HTMLFormatter.htmlSpecialChars(exception.getMessage()) %></h1>

<% if (exception instanceof UserErrorException) { %>
<% } else if (exception instanceof ScriptErrorException || exception instanceof ServletParameterException) { %>
Скрипту, генерирующему страничку были переданы некорректные
параметры. Если на эту страничку вас привела одна из
страниц нашего сайта, пожалуйста сообщите нам адреса текущей и ссылающейся страниц.
<%
  logger.fine(exception.toString()+": "+StringUtil.getStackTrace(exception));
%>
<% } else { %>

К сожалению, произошла исключительная ситуация при генерации страницы.
<%

  String email = "bugz@linux.org.ru";

  InternetAddress mail = new InternetAddress(email);
  StringBuffer text = new StringBuffer();

  if (exception.getMessage()==null) {
    text.append(exception.getClass().getName());
  } else {
    text.append(exception.getMessage());
  }
  text.append("\n\n");
  text.append("Main URL: ").append(tmpl.getMainUrl()).append("\n");
  text.append("Req. URI: ").append(request.getAttribute("javax.servlet.error.request_uri")).append("\n");

  if (request.getQueryString()!=null) {
    text.append("Req. Query: ").append(request.getQueryString()).append("\n");
  }
  
  text.append(" Headers: ");
  Enumeration enu = request.getHeaderNames();
  while ( enu.hasMoreElements() ) {
    String paramName = (String) enu.nextElement();
    text.append("\n         ").append(paramName).append(": ").append(request.getHeader(paramName));
  }
  text.append("\n\n");

  StringWriter exceptionStackTrace = new StringWriter();
  exception.printStackTrace(new PrintWriter(exceptionStackTrace));
  text.append(exceptionStackTrace.toString());

  Properties props = new Properties();
  props.put("mail.smtp.host", "localhost");
  Session mailSession = Session.getDefaultInstance(props, null);

  MimeMessage emailMessage = new MimeMessage(mailSession);
  emailMessage.setFrom(new InternetAddress("no-reply@linux.org.ru"));

  emailMessage.addRecipient(MimeMessage.RecipientType.TO, mail);
  emailMessage.setSubject("Linux.org.ru: " + exception.getClass());
  emailMessage.setSentDate(new Date());
  emailMessage.setText(text.toString(), "UTF-8");

  out.println("\n<br>\n<br>");

  try {
    Transport.send(emailMessage);
    out.println("<b>Произошла непредвиденая ошибка. Администраторы получили об этом сигнал.</b>");
  } catch(Exception e) {
    out.println("<b>Произошла непредвиденая ошибка. К сожалению сервер временно не принимает сообщения об ошибках.</b>");
  } finally {
    logger.severe(exception.toString()+": "+StringUtil.getStackTrace(exception));
  }
%>
<% } %>

<jsp:include page="/WEB-INF/jsp/footer.jsp"/>