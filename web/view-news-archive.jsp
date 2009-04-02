<%@ page pageEncoding="koi8-r" contentType="text/html; charset=utf-8"%>
<%@ page import="java.sql.Connection,java.sql.ResultSet,java.sql.Statement,ru.org.linux.site.BadSectionException,ru.org.linux.site.LorDataSource,ru.org.linux.site.Section"   buffer="200kb"%>
<%@ page import="ru.org.linux.util.DateUtil" %>
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

<jsp:include page="WEB-INF/jsp/head.jsp"/>

<% Connection db=null;
  try { %>
<%
  int sectionid = new ServletParameterParser(request).getInt("section");

  db = LorDataSource.getConnection();

  Section section = new Section(db, sectionid);

  Statement st = db.createStatement();

  String ptitle = section.getName() + " - Архив";
%>
	<title><%= ptitle %></title>
<jsp:include page="WEB-INF/jsp/header.jsp"/>
<H1><%= ptitle %></H1>
<%

if (!section.isBrowsable()) { throw new BadSectionException(sectionid); }

%>
<%

  ResultSet rs = st.executeQuery("select year, month, c from monthly_stats where section=" + sectionid + " order by year, month");
  while (rs.next()) {
    int tMonth = rs.getInt("month");
    int tYear = rs.getInt("year");
    out.print("<a href=\"view-news.jsp?year=" + tYear + "&amp;month=" + tMonth + "&amp;section=" + sectionid + "\">" + rs.getInt("year") + ' ' + DateUtil.getMonth(tMonth) + "</a> (" + rs.getInt("c") + ")<br>");
  }
  rs.close();
%>

<%
	st.close();
  } finally {
    if (db!=null) {
      db.close();
    }
  }
%>
<jsp:include page="WEB-INF/jsp/footer.jsp"/>