<%@ page pageEncoding="koi8-r" contentType="text/html; charset=utf-8"%>
<%@ page import="java.sql.Connection,java.sql.ResultSet,java.sql.Statement,java.sql.Timestamp,java.util.Date,java.util.List"   buffer="200kb"%>
<%@ page import="java.util.Map"%>
<%@ page import="ru.org.linux.site.*"%>
<%@ page import="ru.org.linux.util.ImageInfo" %>
<%@ page import="ru.org.linux.util.StringUtil" %>
<% Template tmpl = Template.getTemplate(request); %>
<jsp:include page="WEB-INF/jsp/head.jsp"/>

<%
  Connection db = null;
  try {
    response.setDateHeader("Expires", new Date(new Date().getTime() - 20 * 3600 * 1000).getTime());
    response.setDateHeader("Last-Modified", new Date(new Date().getTime() - 2 * 1000).getTime());

    if (request.getParameter("group") == null) {
      throw new MissingParameterException("group");
    }

    int groupid = Integer.parseInt(request.getParameter("group"));
    int offset;

    boolean firstPage;

    if (request.getParameter("offset") != null) {
      offset = Integer.parseInt(request.getParameter("offset"));
      firstPage = false;
    } else {
      offset = 0;
      firstPage = true;
    }

    boolean showIgnored = false;
    if (request.getParameter("showignored") != null) {
      showIgnored = "t".equals(request.getParameter("showignored"));
    }

    db = LorDataSource.getConnection();
    db.setAutoCommit(false);

    Group group = new Group(db, groupid);

    Statement st = db.createStatement();

    int count = group.calcTopicsCount(db, false);
    int topics = tmpl.getProf().getInt("topics");

    int pages = count / topics;

    if (count % topics != 0) {
      count = (pages + 1) * topics;
    }

    Section section = new Section(db, group.getSectionId());
    if (group.getSectionId() == 0) {
      throw new BadGroupException();
    }
    if (group.isLinksUp()) {
      throw new BadGroupException();
    }

    if (firstPage) {
      out.print("<title>" + group.getSectionName() + " - " + group.getTitle() + " (последние сообщения)</title>");
    } else {
      out.print("<title>" + group.getSectionName() + " - " + group.getTitle() + " (сообщения " + (count - offset) + '-' + (count - offset - topics) + ")</title>");
    }
    out.print("<link rel=\"parent\" title=\"" + group.getSectionName() + "\" href=\"view-section.jsp?section=" + group.getSectionId() + "\">");
%>
<jsp:include page="WEB-INF/jsp/header.jsp"/>
<form action="group-lastmod.jsp">

  <table class=nav>
    <tr>
      <td align=left valign=middle>
	<a href="view-section.jsp?section=<%= group.getSectionId() %>"><%= group.getSectionName() %></a> - <strong><%= group.getTitle() %></strong>
      </td>

      <td align=right valign=middle>
	      [<a href="/wiki/en/lor-faq">FAQ</a>]
	      [<a href="rules.jsp">Правила форума</a>]

	      [<a href="add.jsp?group=<%= groupid %>">Добавить сообщение</a>]

        <select name=group onChange="submit()" title="Быстрый переход">
  <%
          List<Group> groups = Group.getGroups(db, section);

          for (Group g: groups) {
                  int id = g.getId();
  %>
          <option value=<%= id %> <%= id==groupid?"selected":"" %> ><%= g.getTitle() %></option>
  <%
          }
  %>
        </select>
    </td>
  </tr>
</table>
</form>

<%
  String ignq = "";

  Map<Integer,String> ignoreList = IgnoreList.getIgnoreListHash(db, (String) session.getValue("nick"));

  if (!showIgnored && Template.isSessionAuthorized(session) && !session.getValue("nick").equals("anonymous")) {
    if (firstPage && ignoreList != null && !ignoreList.isEmpty()) {
      ignq = " AND topics.userid NOT IN (SELECT ignored FROM ignore_list, users WHERE userid=users.id and nick='" + session.getValue("nick") + "')";
    }
  }

  out.print("<h1>");

  out.print(group.getSectionName() + ": " + group.getTitle() + "</h1>");

  if (group.getImage() != null) {
    ImageInfo info = new ImageInfo(tmpl.getObjectConfig().getHTMLPathPrefix() + tmpl.getStyle() + group.getImage());
    out.print("<div align=center><img src=\"/" + tmpl.getStyle() + group.getImage() + "\" " + info.getCode() + " border=0 alt=\"Группа " + group.getTitle() + "\"></div>");
  }

  String des = group.getInfo();
  if (des != null) {
    out.print("<p style=\"margin-top: 0px\"><em>");
    out.print(des);
    out.print("</em></p>");
  }
%>
<form action="group-lastmod.jsp" method="GET">

  <input type=hidden name=group value=<%= groupid %>>
  <% if (!firstPage) { %>
	<input type=hidden name=offset value="<%= offset %>">
  <% } %>
  <div class=nav>
	фильтр тем: <select name="showignored">
  	  <option value="t" <%= (showIgnored?"selected":"") %>>все темы</option>
	  <option value="f" <%= (showIgnored?"":"selected") %>>без игнорируемых</option>
	  </select> <input type="submit" value="Обновить"> [<a href="ignore-list.jsp">настроить</a>]
  </div>
</form>

<div class=forum>
<table width="100%" class="message-table">
<thead>
<tr><th>Заголовок
<%
  out.print("<span style=\"font-weight: normal\">[порядок: ");

  out.print("<a href=\"group.jsp?group=" + groupid + "\" style=\"text-decoration: underline\">дата отправки</a> <b>дата изменения</b>");

  out.print("]</span>");
%></th><th>Число ответов<br>всего/день/час</th></tr>
<tbody>
<%
  double messages = tmpl.getProf().getInt("messages");

  ResultSet rs;

  if (firstPage) {
    rs=st.executeQuery("SELECT topics.title as subj, lastmod, nick, topics.id as msgid, deleted, topics.stat1, topics.stat2, topics.stat3, topics.stat4, topics.sticky FROM topics,groups,users, sections WHERE sections.id=groups.section AND (topics.moderate OR NOT sections.moderate) AND topics.userid=users.id AND topics.groupid="+groupid+" AND groups.id="+groupid+" AND NOT deleted " + ignq + " ORDER BY sticky DESC,lastmod DESC LIMIT "+topics+" OFFSET "+offset);
  } else {
    rs=st.executeQuery("SELECT topics.title as subj, lastmod, nick, topics.id as msgid, deleted, topics.stat1, topics.stat2, topics.stat3, topics.stat4, topics.sticky FROM topics,groups,users, sections WHERE sections.id=groups.section AND (topics.moderate OR NOT sections.moderate) AND topics.userid=users.id AND topics.groupid="+groupid+" AND groups.id="+groupid+" AND NOT deleted ORDER BY sticky DESC,lastmod DESC LIMIT "+topics+" OFFSET "+offset);
  }
  
  while (rs.next()) {
    StringBuffer outbuf = new StringBuffer();
    int stat1 = rs.getInt("stat1");

    Timestamp lastmod=rs.getTimestamp("lastmod");
    if (lastmod==null) {
      lastmod = new Timestamp(0);
    }

    outbuf.append("<tr><td>");
    if (rs.getBoolean("deleted")) {
      outbuf.append("[X] ");
    }
	else if(rs.getBoolean("sticky")) {
      outbuf.append("<img src=\"img/paper_clip.gif\" alt=\"Прикреплено\" title=\"Прикреплено\"> ");
    }

    int pagesInCurrent = (int) Math.ceil(stat1 / messages);

    if (firstPage) {
      if (pagesInCurrent <= 1) {
        outbuf.append("<a href=\"view-message.jsp?msgid=").append(rs.getInt("msgid")).append("&amp;lastmod=").append(lastmod.getTime()).append("\" rev=contents>").append(StringUtil.makeTitle(rs.getString("subj"))).append("</a>");
      } else {
        outbuf.append("<a href=\"view-message.jsp?msgid=").append(rs.getInt("msgid")).append("\" rev=contents>").append(StringUtil.makeTitle(rs.getString("subj"))).append("</a>");
      }
    } else {
      outbuf.append("<a href=\"view-message.jsp?msgid=").append(rs.getInt("msgid")).append("\" rev=contents>").append(StringUtil.makeTitle(rs.getString("subj"))).append("</a>");
    }

    if (pagesInCurrent > 1) {
      outbuf.append("&nbsp;(стр.");

      for (int i = 1; i < pagesInCurrent; i++) {
        outbuf.append(" <a href=\"view-message.jsp?msgid=").append(rs.getInt("msgid"));
        if ((i == pagesInCurrent - 1) && firstPage) {
          outbuf.append("&amp;lastmod=").append(lastmod.getTime());
        }
        outbuf.append("&amp;page=").append(i).append("\">");
        outbuf.append(i + 1).append("</a>");
      }
      outbuf.append(')');
    }

    outbuf.append(" (").append(rs.getString("nick")).append(") ");
                outbuf.append("</td>");
		outbuf.append("<td align=center>");
		int stat3=rs.getInt("stat3");
		int stat4=rs.getInt("stat4");

		if (stat1>0) {
                  outbuf.append("<b>").append(stat1).append("</b>/");
                }
		else {
                  outbuf.append("-/");
                }

		if (stat3>0) {
                  outbuf.append("<b>").append(stat3).append("</b>/");
                }
		else {
                  outbuf.append("-/");
                }

		if (stat4>0) {
                  outbuf.append("<b>").append(stat4).append("</b>");
                }
		else {
                  outbuf.append("-");
                }



		outbuf.append("</td></tr>");
		
		if (!firstPage && ignoreList != null && !ignoreList.isEmpty() && ignoreList.containsValue(rs.getString("nick"))) {
		  outbuf = new StringBuffer();
		  //new StringBuffer().append("<tr><td colspan=2>Тема создана игнорируемым пользователем</td></tr>");
		}
		
		out.print(outbuf.toString());
		
  }
	rs.close();
%>
  <tfoot><tr><td colspan=2><p>
<%
	String ignoredAdd = showIgnored?("&amp;showignored=t"):"";
	
	out.print("<div style=\"float: left\">");
	if (offset==0) {
          out.print("<b>Назад</b>");
        }
	else
		if ((offset-topics)==0) {
                  out.print("<a rel=prev rev=next href=\"group-lastmod.jsp?group=" + groupid + ignoredAdd + "\">Назад</a>");
                }
		else {
                  out.print("<a rel=prev rev=next href=\"group-lastmod.jsp?group=" + groupid + "&amp;offset=" + (offset - topics) + ignoredAdd + "\">Назад</a>");
                }
	out.print("</div>");
	if (offset>0) {
          out.print("<div style=\"text-align: center\"><a rel=start href=\"group-lastmod.jsp?group=" + groupid + ignoredAdd + "\">Начало</a></div>");
        }
	out.print("<div style=\"float: right\">");
	if (offset==topics*pages) {
          out.print("<b>Вперед</b>");
        }
	else {
          out.print("<a rel=next rev=prev href=\"group-lastmod.jsp?group=" + groupid + "&amp;offset=" + (offset + topics) + ignoredAdd + "\">Вперед</a>");
        }

	out.print("</div>");

%>
</td></tr></table>
</div>
<div align=center><p>
<%
  for (int i=0; i<pages+1; i++) {
    if (i!=0 && i!=pages && Math.abs(i*topics-offset)>7*topics) {
      continue;
    }

    if (i==pages) {
      out.print("[<a href=\"group-lastmod.jsp?group=" + groupid + "&amp;offset=" + (i * topics) + ignoredAdd + "\">конец</a>] ");
    }
    else if (i*topics==offset) {
      out.print("[<b>" + (pages + 1 - i) + "</b>] ");
    }
    else
      if (i!=0) {
        out.print("[<a href=\"group-lastmod.jsp?group=" + groupid + "&amp;offset=" + (i * topics) + ignoredAdd + "\">" + (pages + 1 - i) + "</a>] ");
      }
      else {
        out.print("[<a href=\"group-lastmod.jsp?group=" + groupid + ignoredAdd + "\">начало</a>] ");
      }
  }
%>
  </div>
<p>
<%
	st.close();
	db.commit();
%>
<%
  } finally {
    if (db!=null) {
      db.close();
    }
  }
%>
<jsp:include page="WEB-INF/jsp/footer.jsp"/>
