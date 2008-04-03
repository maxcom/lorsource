<%@ page pageEncoding="koi8-r" contentType="text/html; charset=utf-8"%>
<%@ page import="java.io.File"  %>
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
<% Template tmpl = Template.getTemplate(request);%>
<jsp:include page="WEB-INF/jsp/head.jsp"/>


<%
  boolean initial = request.getParameter("q") == null;

  String q = request.getParameter("q");
  int include = SearchViewer.parseInclude(request.getParameter("include"));
  int date = SearchViewer.parseDate(request.getParameter("date"));

  String strSection = request.getParameter("section");
  int section = 0;
  if (strSection!=null) {
    section = new ServletParameterParser(request).getInt("section");
  }

  int sort = SearchViewer.SORT_R;
  String strSort = request.getParameter("sort");
  if (strSort!=null) {
    sort = new ServletParameterParser(request).getInt("sort");
  }

  String username = request.getParameter("username");
  if (username==null) {
    username = "";
  }

%>

<title>Поиск по сайту <%= q != null ? (" - "+HTMLFormatter.htmlSpecialChars(q)):"" %></title>
<jsp:include page="WEB-INF/jsp/header.jsp"/>

<H1>Поиск по сайту</h1>
<h2>Поисковая система сайта</h2>

<FORM METHOD=GET ACTION="search.jsp">
Искать: <INPUT TYPE="text" NAME="q" SIZE=50 VALUE="<%= q!=null?q:"" %>"><p>
  <select name="include">
    <option value="topics" <%= (include==SearchViewer.SEARCH_TOPICS)?"selected":"" %>>только темы</option>
    <option value="all" <%= (include==SearchViewer.SEARCH_ALL)?"selected":"" %>>темы и комментарии</option>
  </select>

  За:
  <select name="date">
    <option value="3month" <%= (date==SearchViewer.SEARCH_3MONTH)?"selected":"" %>>три месяца</option>
    <option value="year" <%= (date==SearchViewer.SEARCH_YEAR)?"selected":"" %>>год</option>
    <option value="all" <%= (date==SearchViewer.SEARCH_ALL)?"selected":"" %>>весь период</option>
  </select>
<br>
  Раздел:
  <select name="section">

  <option value="1" <%= (section==1)?"selected":"" %>>новости</option>

  <option value="2" <%= (section==2)?"selected":"" %>>форум</option>

  <option value="3" <%= (section==3)?"selected":"" %>>галерея</option>

  <option value="0" <%= (section==0)?"selected":"" %>>все</option>
  </select>

  Пользователь:
  <INPUT TYPE="text" NAME="username" SIZE=20 VALUE="<%= HTMLFormatter.htmlSpecialChars(username) %>"><p>
  <br>

  Сортировать
  <select name="sort">
  <option value="<%= SearchViewer.SORT_DATE %>" <%= (sort==SearchViewer.SORT_DATE)?"selected":"" %>>по дате</option>

  <option value="<%= SearchViewer.SORT_R %>" <%= (sort==SearchViewer.SORT_R)?"selected":"" %>>по релевантности</option>
  </select>

  <br>
<input TYPE="submit" VALUE="Искать!"><BR>

</form>

<%
  if (!initial) {
    SearchViewer sv = new SearchViewer(q);

    sv.setDate(date);
    sv.setInclude(include);
    sv.setSection(section);
    sv.setSort(sort);
    sv.setUser(username);

    ViewerCacher cacher = new ViewerCacher();

    out.print(cacher.get(sv, tmpl, false));

    out.print("<p><i>");

    if (cacher.isFromCache()) {
      out.print("Результаты извлечены из кеша, время поиска: "+cacher.getTime()+"ms");
    } else {
      out.print("Результаты извлечены из БД, время поиска: "+cacher.getTime()+"ms");      
    }
    out.print("</i></p>");
  } else {
%>
<h2>Поиск через Google</h2>

<%
  if (tmpl.getStyle().equals("black")) {
%>
<!-- SiteSearch Google -->
<form method="get" action="http://www.google.ru/custom" target="_top">
<table border="0" bgcolor="#000000">
<tr><td nowrap="nowrap" valign="top" align="left" height="32">
<a href="http://www.google.com/">
<img src="http://www.google.com/logos/Logo_25blk.gif" border="0" alt="Google" align="middle"></img></a>
</td>
<td nowrap="nowrap">
<input type="hidden" name="domains" value="www.linux.org.ru"></input>
<label for="sbi" style="display: none">Введите условия поиска</label>
<input type="text" name="q" size="31" maxlength="255" value="" id="sbi"></input>
<label for="sbb" style="display: none">Отправить форму поиска</label>
<input type="submit" name="sa" value="Поиск" id="sbb"></input>
</td></tr>
<tr>
<td>&nbsp;</td>
<td nowrap="nowrap">
<table>
<tr>
<td>
<input type="radio" name="sitesearch" value="" id="ss0"></input>
<label for="ss0" title="Искать в Интернете"><font size="-1" color="white">Web</font></label></td>
<td>
<input type="radio" name="sitesearch" value="www.linux.org.ru" checked id="ss1"></input>
<label for="ss1" title="Поиск www.linux.org.ru"><font size="-1" color="white">www.linux.org.ru</font></label></td>
</tr>
</table>
<input type="hidden" name="client" value="pub-6069094673001350"></input>
<input type="hidden" name="forid" value="1"></input>
<input type="hidden" name="ie" value="UTF-8"></input>
<input type="hidden" name="oe" value="UTF-8"></input>
<input type="hidden" name="flav" value="0000"></input>
<input type="hidden" name="sig" value="VNPb2D8JZrqtw9dZ"></input>
<input type="hidden" name="cof" value="GALT:#3399FF;GL:1;DIV:#666666;VLC:FFFFFF;AH:center;BGC:000000;LBGC:FFFF00;ALC:FFFFFF;LC:FFFFFF;T:CCCCCC;GFNT:FFFFFF;GIMP:FFFFFF;LH:65;LW:30;L:http://www.linux.org.ru/black/img/angry-logo.gif;S:http://;LP:1;FORID:1"></input>
<input type="hidden" name="hl" value="ru"></input>
</td></tr></table>
</form>
<!-- SiteSearch Google -->
<% } else { %>
<!-- SiteSearch Google -->
<form method="get" action="http://www.google.ru/custom" target="_top">
<table border="0" bgcolor="#ffffff">
<tr><td nowrap="nowrap" valign="top" align="left" height="32">
<a href="http://www.google.com/">
<img src="http://www.google.com/logos/Logo_25wht.gif" border="0" alt="Google" align="middle"></img></a>
</td>
<td nowrap="nowrap">
<input type="hidden" name="domains" value="www.linux.org.ru"></input>
<label for="sbi" style="display: none">Введите условия поиска</label>
<input type="text" name="q" size="31" maxlength="255" value="" id="sbi"></input>
<label for="sbb" style="display: none">Отправить форму поиска</label>
<input type="submit" name="sa" value="Поиск" id="sbb"></input>
</td></tr>
<tr>
<td>&nbsp;</td>
<td nowrap="nowrap">
<table>
<tr>
<td>
<input type="radio" name="sitesearch" value="" id="ss0"></input>
<label for="ss0" title="Искать в Интернете"><font size="-1" color="#000000">Web</font></label></td>
<td>
<input type="radio" name="sitesearch" value="www.linux.org.ru" checked id="ss1"></input>
<label for="ss1" title="Поиск www.linux.org.ru"><font size="-1" color="#000000">www.linux.org.ru</font></label></td>
</tr>
</table>
<input type="hidden" name="client" value="pub-6069094673001350"></input>
<input type="hidden" name="forid" value="1"></input>
<input type="hidden" name="ie" value="UTF-8"></input>
<input type="hidden" name="oe" value="UTF-8"></input>
<input type="hidden" name="flav" value="0000"></input>
<input type="hidden" name="sig" value="VNPb2D8JZrqtw9dZ"></input>
<input type="hidden" name="cof" value="GALT:#008000;GL:1;DIV:#336699;VLC:663399;AH:center;BGC:FFFFFF;LBGC:336699;ALC:0000FF;LC:0000FF;T:000000;GFNT:0000FF;GIMP:0000FF;LH:65;LW:30;L:http://www.linux.org.ru/white2/img/angry-logo.gif;S:http://;LP:1;FORID:1"></input>
<input type="hidden" name="hl" value="ru"></input>
</td></tr></table>
</form>
<!-- SiteSearch Google -->
<% } %>
<% } %>

<jsp:include page="WEB-INF/jsp/footer.jsp"/>
