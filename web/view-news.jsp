<%@ page contentType="text/html; charset=utf-8"%>
<%@ page import="java.net.URLEncoder,java.sql.Connection,java.sql.Statement,java.util.Calendar"   buffer="200kb"%>
<%@ page import="java.util.List" %>
<%@ page import="ru.org.linux.site.*" %>
<%@ page import="ru.org.linux.util.DateUtil" %>
<%@ page import="ru.org.linux.util.ServletParameterException" %>
<%@ page import="ru.org.linux.util.ServletParameterParser" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
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

<% Template tmpl = Template.getTemplate(request); %>
<jsp:include page="WEB-INF/jsp/head.jsp"/>
<%
  Connection db = null;
  try {
    if (request.getParameter("month") == null) {
      response.setDateHeader("Expires", System.currentTimeMillis()+60*1000);
      response.setDateHeader("Last-Modified", System.currentTimeMillis());
    } else {
      long expires = System.currentTimeMillis()+30*24*60*60*1000L;

      response.setDateHeader("Expires", System.currentTimeMillis()+30*24*60*60*1000L);

      int month= new ServletParameterParser(request).getInt("month");
      int year= new ServletParameterParser(request).getInt("year");

      Calendar calendar = Calendar.getInstance();
      calendar.set(year, month-1, 1);
      calendar.add(Calendar.MONTH, 1);

      long lastmod = calendar.getTimeInMillis();

      if (lastmod<System.currentTimeMillis()) {
        response.setDateHeader("Expires", expires);
        response.setDateHeader("Last-Modified", lastmod);
      } else {
        response.setDateHeader("Expires", System.currentTimeMillis()+60*1000);
        response.setDateHeader("Last-Modified", System.currentTimeMillis());
      }
    }
%>

<%
  db = LorDataSource.getConnection();

  int sectionid = 0;
  Section section = null;

  if (request.getParameter("section")!=null) {
    sectionid = new ServletParameterParser(request).getInt("section");
    section = new Section(db, sectionid);
  }

  Group group = null;

  if (request.getParameter("group") != null) {
    int groupid = new ServletParameterParser(request).getInt("group");
    group = new Group(db, groupid);

    if (group.getSectionId() != sectionid) {
      throw new ScriptErrorException("группа #" + groupid + " не принадлежит разделу #" + sectionid);
    }
  }

  String tag = null;
  if (request.getParameter("tag")!=null) {
    tag = new ServletParameterParser(request).getString("tag");
    Tags.checkTag(tag);
  }

  if (section==null && tag ==null) {
    throw new ServletParameterException("section or tag required");
  }

  Statement st = db.createStatement();

  String navtitle;
  if (section!=null) {
    navtitle = section.getName();
  } else {
    navtitle = tag;
  }

  if (group != null) {
    navtitle = "<a href=\"view-news.jsp?section=" + section.getId() + "\">" + section.getName() + "</a> - " + group.getTitle();
  }

  int month = 0;
  int year = 0;
  String ptitle;

  if (request.getParameter("month") == null) {
    if (section != null) {
      ptitle = section.getName();
      if (group != null) {
        ptitle += " - " + group.getTitle();
      }

      if (tag !=null) {
        ptitle += " - " + tag;
      }
    } else {
      ptitle = tag;
    }
  } else {
    month = new ServletParameterParser(request).getInt("month");
    year = new ServletParameterParser(request).getInt("year");
    ptitle = "Архив: " + section.getName();

    if (group != null) {
      ptitle += " - " + group.getTitle();
    }

    if (tag !=null) {
      ptitle += " - " + tag;
    }

    ptitle += ", " + year + ", " + DateUtil.getMonth(month);
    navtitle += " - Архив " + year + ", " + DateUtil.getMonth(month);
  }

  if (sectionid == 2) {
    throw new BadSectionException(sectionid);
  }

%>
	<title><%= ptitle %></title>
<% if (section!=null) { %>
        <LINK REL="alternate" HREF="section-rss.jsp?section=<%= sectionid %><%= (group!=null?("&amp;group="+group.getId()):"")%>" TYPE="application/rss+xml">
<% } %>

<jsp:include page="WEB-INF/jsp/header.jsp"/>
<% if (section!=null) { %>
  <table class=nav><tr>
    <td align=left valign=middle id="navPath">
      <strong><%= navtitle %></strong>
    </td>
    <td align=right valign=middle>
<%
  if (month == 0) {
    if (section.isVotePoll()) {
      out.print("[<a href=\"add-poll.jsp?group=19387\">Добавить голосование</a>]");
    } else {
      if (group == null) {
        out.print("[<a href=\"add-section.jsp?section=" + section.getId() + "\">Добавить</a>]");
      } else {
        out.print("[<a href=\"add.jsp?group=" + group.getId() + "\">Добавить</a>]");
      }
    }

    if (group == null) {
      out.print("[<a href=\"view-section.jsp?section=" + section.getId() + "\">Таблица</a>]");
    } else {
      out.print("[<a href=\"group.jsp?group=" + group.getId() + "\">Таблица</a>]");
    }
  }

  out.print("[<a href=\"view-news-archive.jsp?section="+sectionid+"\">Архив</a>]");
  out.print("[<a href=\"section-rss.jsp?section="+sectionid+(group!=null?("&amp;group="+group.getId()):"")+"\">RSS</a>]");

%>
    </td>
   </tr>
</table>

<% } %>

<H1><%= ptitle %></H1>
<%
  NewsViewer newsViewer = new NewsViewer();
  if (section!=null) {
    newsViewer.setSection(sectionid);
  }

  if (group != null) {
    newsViewer.setGroup(group.getId());
  }

  if (tag!=null) {
    newsViewer.setTag(tag);
  }

  int offset = 0;
  if (request.getParameter("offset")!=null) {
    offset = new ServletParameterParser(request).getInt("offset");

    if (offset<0) {
	offset = 0;
    }

    if (offset>200) {
      offset=200;
    }
  }

  if (month != 0) {
    newsViewer.setDatelimit("postdate>='" + year + '-' + month + "-01'::timestamp AND (postdate<'" + year + '-' + month + "-01'::timestamp+'1 month'::interval)");
  } else if (tag==null) {
    newsViewer.setDatelimit("commitdate>(CURRENT_TIMESTAMP-'6 month'::interval)");
    newsViewer.setLimit("LIMIT 20" + (offset > 0 ? (" OFFSET " + offset) : ""));
  } else {
    newsViewer.setLimit("LIMIT 20" + (offset > 0 ? (" OFFSET " + offset) : ""));
  }

  st.close();
%>
<c:forEach var="msg" items="<%= newsViewer.getMessagesCached(db, tmpl) %>">
  <lor:news db="<%= db %>" message="${msg}" multiPortal="<%= sectionid==0 && group==null %>" moderateMode="false"/>
</c:forEach>

<%
  String params = "section="+sectionid;
  if (tag!=null) {
    params += "&amp;tag="+ URLEncoder.encode(tag, "UTF-8");
  }

  if (group!=null) {
    params += "&amp;group="+group.getId();
  }

  if (month==0 && tag==null) {
%>
<table class="nav">
  <tr>
  <% if (offset<200) { %>
    <td align="left" width="35%">
      <a href="view-news.jsp?<%= params %>&amp;offset=<%=offset+20%>">← предыдущие</a>
    </td>
  <% } %>
  <% if (offset>20) { %>
    <td width="35%" align="right">
      <a href="view-news.jsp?<%= params %>&amp;offset=<%= (offset-20) %>">следующие →</a>
    </td>
  <% } else if (offset==20) { %>
    <td width="35%" align="right">
      <a href="view-news.jsp?<%= params %>">следующие →</a>
    </td>
  <% } %>
  </tr>
</table>

<%
    }
  } finally {
    if (db!=null) {
      db.close();
    }
  }
%>

<jsp:include page="WEB-INF/jsp/footer.jsp"/>

