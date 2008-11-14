<%@ page pageEncoding="koi8-r" contentType="text/html; charset=utf-8"%>
<%@ page import="java.sql.Connection,java.sql.ResultSet,java.sql.Statement,java.util.Calendar"   buffer="200kb"%>
<%@ page import="java.util.Date" %>
<%@ page import="java.util.List" %>
<%@ page import="ru.org.linux.boxlet.BoxletVectorRunner" %>
<%@ page import="ru.org.linux.site.*" %>
<%@ page import="ru.org.linux.util.DateUtil" %>
<%@ page import="ru.org.linux.util.ServletParameterParser" %>
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
  int sectionid = new ServletParameterParser(request).getInt("section");
  Group group = null;

  db = LorDataSource.getConnection();

  Section section = new Section(db, sectionid);

  if (request.getParameter("group") != null) {
    int groupid = new ServletParameterParser(request).getInt("group");
    group = new Group(db, groupid);

    if (group.getSectionId() != sectionid) {
      throw new ScriptErrorException("группа #" + groupid + " не пренадлежит разделу #" + sectionid);
    }
  }

  String tag = null;
  if (request.getParameter("tag")!=null) {
    tag = new ServletParameterParser(request).getString("tag");
    Tags.checkTag(tag);
  }

  Statement st = db.createStatement();

  String navtitle = section.getName();

  if (group != null) {
    navtitle = "<a href=\"view-news.jsp?section=" + section.getId() + "\">" + section.getName() + "</a> - " + group.getTitle();
  }

  int month = 0;
  int year = 0;
  String ptitle;

  if (request.getParameter("month") == null) {
    ptitle = section.getName();
    if (group != null) {
      ptitle += " - " + group.getTitle();
    }

    if (tag !=null) {
      ptitle += " - " + tag;
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

  if (!section.isBrowsable() || sectionid == 2) {
    throw new BadSectionException(sectionid);
  }

%>
	<title><%= ptitle %></title>
        <LINK REL="alternate" HREF="section-rss.jsp?section=<%= sectionid %><%= (group!=null?("&amp;group="+group.getId()):"")%>" TYPE="application/rss+xml">

<jsp:include page="WEB-INF/jsp/header.jsp"/>

  <table class=nav><tr>
    <td align=left valign=middle>
      <strong><%= navtitle %></strong>
    </td>
    <td align=right valign=middle>
<%
  if (month==0) {
      if (section.isImagepost()) {
        out.print("[<a href=\"add.jsp?group=4962\">Добавить изображение</a>]");
      } else if (section.isVotePoll()) {
        out.print("[<a href=\"add-poll.jsp?group=19387\">Добавить голосование</a>]");
      } else {
        if (group==null) {
          out.print("[<a href=\"add-section.jsp?section="+section.getId()+"\">Добавить</a>]");
        } else {
          out.print("[<a href=\"add.jsp?group="+group.getId()+"\">Добавить</a>]");
        }
      }

    if (group==null) {
      out.print("[<a href=\"view-section.jsp?section="+section.getId()+"\">Таблица</a>]");
    } else {
      out.print("[<a href=\"group.jsp?group="+group.getId()+"\">Таблица</a>]");
    }
  }
  out.print("[<a href=\"view-news-archive.jsp?section="+sectionid+"\">Архив</a>]");
  out.print("[<a href=\"section-rss.jsp?section="+sectionid+(group!=null?("&amp;group="+group.getId()):"")+"\">RSS</a>]");

%>
    </td>
   </tr>
</table>

<H1><%= ptitle %></H1>
<%
  NewsViewer nw = new NewsViewer(tmpl.getConfig(), tmpl.getProf());
  nw.setSection(sectionid);
  if (group != null) {
    nw.setGroup(group.getId());
  }

  if (tag!=null) {
    nw.setTag(tag);
  }

  if (month != 0) {
    nw.setDatelimit("postdate>='" + year + "-" + month + "-01'::timestamp AND (postdate<'" + year + "-" + month + "-01'::timestamp+'1 month'::interval)");
  } else if (tag==null) {
    nw.setDatelimit("commitdate>(CURRENT_TIMESTAMP-'3 month'::interval)");
    nw.setLimit("LIMIT 20");
  }

  db.close(); db=null;

  out.print(ViewerCacher.getViewer(nw, tmpl, false));
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

