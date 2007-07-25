<%@ page contentType="text/html; charset=koi8-r"%>
<%@ page import="java.net.URLEncoder,javax.servlet.http.HttpServletResponse,ru.org.linux.site.Template" errorPage="/error.jsp" buffer="60kb"%>
<% Template tmpl = new Template(request, config, response); %>
<%= tmpl.head() %>
<%
  int msgid = tmpl.getParameters().getInt("msgid");

  String redirectUrl = tmpl.getMainUrl() + "view-message.jsp?msgid=" + msgid;
  StringBuffer options = new StringBuffer();

  if (tmpl.isSessionAuthorized(session)) {
    redirectUrl = tmpl.getRedirectUrl() + "view-message.jsp?msgid=" + msgid;
  } else if (tmpl.getCookie("profile") != null && !"".equals(tmpl.getCookie("profile"))) {
    redirectUrl = tmpl.getRedirectUrl(tmpl.getCookie("profile")) + "view-message.jsp?msgid=" + msgid;
  }

  if (request.getParameter("page") != null) {
    options.append("&page=");
    options.append(URLEncoder.encode(request.getParameter("page")));
  }

  if (request.getParameter("back") != null) {
    options.append("&back=");
    options.append(URLEncoder.encode(request.getParameter("back")));
  }

  if (request.getParameter("nocache") != null) {
    options.append("&nocache=");
    options.append(URLEncoder.encode(request.getParameter("nocache")));
  }

  response.setHeader("Location", redirectUrl + options.toString());
  response.setStatus(HttpServletResponse.SC_MOVED_TEMPORARILY);
%>
<%= tmpl.DocumentHeader() %>
Go to: <%= redirectUrl %>
<%= tmpl.DocumentFooter() %>
