<%@ page import="java.io.File" %>
<%@ page import="java.io.IOException" %>
<%@ page import="java.io.PrintWriter" %>
<%@ page import="java.io.StringWriter" %>
<%@ page import="java.net.URLEncoder" %>
<%@ page import="java.sql.*" %>
<%@ page import="java.text.DateFormat" %>
<%@ page import="java.text.SimpleDateFormat" %>
<%@ page import="java.util.*" %>
<%@ page import="java.util.Date" %>
<%@ page import="java.util.logging.Logger" %>
<%@ page import="javax.mail.Session" %>
<%@ page import="javax.mail.Transport" %>
<%@ page import="javax.mail.internet.InternetAddress" %>
<%@ page import="javax.mail.internet.MimeMessage" %>
<%@ page import="javax.servlet.http.Cookie" %>
<%@ page import="javax.servlet.http.HttpServletResponse" %>
<%@ page import="com.danga.MemCached.MemCachedClient" %>
<%@ page import="org.apache.commons.fileupload.FileItem" %>
<%@ page import="org.apache.commons.fileupload.disk.DiskFileItemFactory" %>
<%@ page import="org.apache.commons.fileupload.servlet.ServletFileUpload" %>
<%@ page import="org.apache.commons.lang.StringUtils" %>
<%@ page import="ru.org.linux.boxlet.BoxletVectorRunner" %>
<%@ page import="ru.org.linux.site.*" %>
<%@ page import="ru.org.linux.storage.StorageNotFoundException" %>
<%@ page import="ru.org.linux.util.*" %>
<p><i><a href="${template.mainUrl}">${template.mainUrl}</a></i>

<div align=center>
  <iframe src="dw.jsp?width=728&amp;height=90&amp;main=0" width="728" height="90" scrolling="no" frameborder="0"></iframe>
</div>

<div align=center>
  <!--TopList LOGO-->
  <a target=_top href="http://top.list.ru/jump?from=71642"><img src="http://top.list.ru/counter?id=71642;t=11;l=1" border=0 height=31 width=88 alt="TopList"></a>
  <!--TopList LOGO-->
</div>

<script src="http://www.google-analytics.com/urchin.js" type="text/javascript"></script>
<script type="text/javascript">
  _uacct = "UA-2184304-1";
  urchinTracker();
</script>

${template.documentFooter}

</body></html>