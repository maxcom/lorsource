<%@ page contentType="text/html; charset=utf-8"%>
<%@ page import="ru.org.linux.site.SearchViewer"  %>
<%@ page import="ru.org.linux.site.Template" %>
<%@ page import="ru.org.linux.site.ViewerCacher" %>
<%@ page import="ru.org.linux.util.HTMLFormatter" %>
<%@ page import="ru.org.linux.util.ServletParameterParser" %>
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
<jsp:include page="WEB-INF/jsp/head.jsp"/>


<%
  boolean initial = request.getParameter("q") == null;

  String q = request.getParameter("q");
  if (q==null) {
    q="";
  }

  int include = SearchViewer.parseInclude(request.getParameter("include"));
  int date = SearchViewer.parseDate(request.getParameter("date"));

//  String strSection = request.getParameter("section");
//  int section = 0;
//  if (strSection!=null) {
//    section = new ServletParameterParser(request).getInt("section");
//  }

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

<title>Поиск по сайту <%= initial ? "" : (" - " + HTMLFormatter.htmlSpecialChars(q)) %></title>
<jsp:include page="WEB-INF/jsp/header.jsp"/>

<H1>Поиск по сайту</h1>
<h2>Поисковая система сайта</h2>

<FORM METHOD=GET ACTION="search.jsp">
Искать: <INPUT TYPE="text" NAME="q" SIZE=50 VALUE="<%= HTMLFormatter.htmlSpecialChars(q) %>"><p>
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
<%--
  Раздел:
  <select name="section">

  <option value="1" <%= (section==1)?"selected":"" %>>новости</option>

  <option value="2" <%= (section==2)?"selected":"" %>>форум</option>

  <option value="3" <%= (section==3)?"selected":"" %>>галерея</option>

  <option value="0" <%= (section==0)?"selected":"" %>>все</option>
  </select>
--%>

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
<jsp:include page="/WEB-INF/jsp/${template.style}/google-search.jsp"/>
<% } %>

<jsp:include page="WEB-INF/jsp/footer.jsp"/>
