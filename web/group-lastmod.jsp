<%@ page contentType="text/html; charset=koi8-r"%>
<%@ page import="java.net.URLEncoder,java.sql.Connection,java.sql.ResultSet,java.sql.Statement,java.sql.Timestamp,java.util.Date,java.util.Map" errorPage="/error.jsp" buffer="200kb"%>
<%@ page import="ru.org.linux.site.BadGroupException"%>
<%@ page import="ru.org.linux.site.IgnoreList"%>
<%@ page import="ru.org.linux.site.MissingParameterException"%>
<%@ page import="ru.org.linux.site.Template"%>
<%@ page import="ru.org.linux.util.ImageInfo"%>
<%@ page import="ru.org.linux.util.StringUtil" %>
<% Template tmpl = new Template(request, config, response); %>
<%= tmpl.head() %>
<%
  Connection db = null;
  try {
    response.setDateHeader("Expires", new Date(new Date().getTime() - 20 * 3600 * 1000).getTime());
    response.setDateHeader("Last-Modified", new Date(new Date().getTime() - 2 * 1000).getTime());

    if (request.getParameter("group") == null)
      throw new MissingParameterException("group");

    int group = Integer.parseInt(request.getParameter("group"));
    int offset;

    boolean firstPage;

    if (request.getParameter("offset") != null) {
      offset = Integer.parseInt(request.getParameter("offset"));
      firstPage = false;
    } else {
      offset = 0;
      firstPage = true;
    }

	boolean showIgnored = tmpl.getProf().getBoolean("showignored");
	if (request.getParameter("showignored") != null) {
	  showIgnored = "t".equals(request.getParameter("showignored"));
	}

    String returnUrl;
    if (offset > 0)
      returnUrl = "group-lastmod.jsp?group=" + group + "&amp;offset=" + offset;
    else
      returnUrl = "group-lastmod.jsp?group=" + group;

    db = tmpl.getConnection("group-lastmod");
    db.setAutoCommit(false);

    Statement st = db.createStatement();

    ResultSet rs = st.executeQuery("SELECT count(topics.id) FROM topics,groups,sections WHERE (topics.moderate OR NOT sections.moderate) AND groups.section=sections.id AND topics.groupid=groups.id AND groups.id=" + group + " AND NOT topics.deleted");
    int count = 0;
    int pages = 0;
    int topics = tmpl.getProf().getInt("topics");

    if (rs.next()) {
      count = rs.getInt("count");
      pages = count / topics;
      if (count % topics != 0)
        count = (pages + 1) * topics;
    }
    rs.close();

    rs = st.executeQuery("SELECT title,sections.name,image, sections.linkup, sections.id FROM groups,sections WHERE groups.id=" + group + " AND section=sections.id");
    if (!rs.next()) throw new BadGroupException("Группа " + group + " не существует");
    int section = rs.getInt("id");
    if (section == 0) throw new BadGroupException();
    if (rs.getBoolean("linkup")) throw new BadGroupException();

    if (firstPage) {
      out.print("<title>" + rs.getString("name") + " - " + rs.getString("title") + " (последние сообщения)</title>");
    } else {
      out.print("<title>" + rs.getString("name") + " - " + rs.getString("title") + " (сообщения " + (count - offset) + '-' + (count - offset - topics) + ")</title>");
    }
    out.print("<link rel=\"parent\" title=\"" + rs.getString("title") + "\" href=\"view-section.jsp?section=" + rs.getInt("id") + "\">");
%>
<%=   tmpl.DocumentHeader() %>
<div class=messages>
<div class=nav>
<form action="group-lastmod.jsp">

<div class=color1>
  <table width="100%" cellspacing=1 cellpadding=1 border=0>
    <tr class=body>
      <td align=left valign=middle>
	<a href="view-section.jsp?section=<%= rs.getInt("id") %>"><%= rs.getString("name") %></a> - <strong><%= rs.getString("title") %></strong>
      </td>

      <td align=right valign=middle>
	      [<a style="text-decoration: none" href="faq.jsp">FAQ</a>]
	      [<a style="text-decoration: none" href="rules.jsp">Правила форума</a>]

	      [<a style="text-decoration: none" href="add.jsp?group=<%= group %>&amp;return=<%= URLEncoder.encode(returnUrl) %>">Добавить сообщение</a>]

              <select name=group onChange="submit()" title="Быстрый переход">
<%
	Statement sectionListSt = db.createStatement();
	ResultSet sectionList = sectionListSt.executeQuery("SELECT id, title FROM groups WHERE section="+section+" order by id");

	while (sectionList.next()) {
		int id = sectionList.getInt("id");
%>
		<option value=<%= id %> <%= id==group?"selected":"" %> ><%= sectionList.getString("title") %></option>
<%
	}

	sectionList.close();
	sectionListSt.close();
%>
            </select>
    </td>
  </tr>
</table>
</div>
</form>

<%
  String ignq = ""; 
   
  Map ignoreList = IgnoreList.getIgnoreListHash(db, (String) session.getValue("nick"));
   
  if (!showIgnored && tmpl.isSessionAuthorized(session) && !session.getValue("nick").equals("anonymous")) {
	if (firstPage && ignoreList != null && !ignoreList.isEmpty())
	  ignq = " AND topics.userid NOT IN (SELECT ignored FROM ignore_list, users WHERE userid=users.id and nick='" + session.getValue("nick") + "')";
  }
  
%>
</div>
</div>
<%
	out.print("<h1>");

	out.print(rs.getString("name")+": "+rs.getString("title")+"</h1>");

	if (rs.getString("image")!=null) {
		ImageInfo info=new ImageInfo(tmpl.getObjectConfig().getHTMLPathPrefix()+tmpl.getStyle()+rs.getString("image"));
		out.print("<div align=center><img src=\"/" + tmpl.getStyle() + rs.getString("image") + "\" " + info.getCode() + " border=0 alt=\"Группа " + rs.getString("title") + "\"></div>");
	}

	String des=tmpl.getObjectConfig().getStorage().readMessageNull("grinfo", String.valueOf(group));
	if (des!=null) {
		out.print("<p><em>");
		out.print(des);
		out.print("</em></p>");
	}

	rs.close();

%>
<div class=messages>
<div class=nav>
<form action="group-lastmod.jsp" method="GET">

  <input type=hidden name=group value=<%= group %>>
  <% if (!firstPage) { %>
	<input type=hidden name=offset value="<%= offset %>">
  <% } %>
<div class=color1>
  <table width="100%" cellspacing=1 cellpadding=0 border=0><tr class=body>
	<td><div align="center">фильтр тем: <select name="showignored">
  	  <option value="t" <%= (showIgnored?"selected":"") %>>все темы</option>
	  <option value="f" <%= (showIgnored?"":"selected") %>>без игнорируемых</option>
	  </select> <input type="submit" value="Обновить"> [<a style="text-decoration: none" href="ignore-list.jsp">настроить</a>]</div>
	</td>
  </tr>
  </table>
</div>
</form>

</div>
</div>
  
<div class=forum>
<table width="100%" class="message-table">
<thead>
<tr><th>Заголовок
<%
  out.print("<span style=\"font-weight: normal\">[порядок: ");

  out.print("<a href=\"group.jsp?group=" + group + "\" style=\"text-decoration: underline\">дата отправки</a> <b>дата изменения</b>");

  out.print("]</span>");
%></th><th>Число ответов<br>всего/день/час</th></tr>
<tbody>
<%
  double messages = tmpl.getProf().getInt("messages");

  if (firstPage) {
	rs=st.executeQuery("SELECT topics.title as subj, lastmod, nick, topics.id as msgid, deleted, topics.stat1, topics.stat2, topics.stat3, topics.stat4, topics.sticky FROM topics,groups,users, sections WHERE sections.id=groups.section AND (topics.moderate OR NOT sections.moderate) AND topics.userid=users.id AND topics.groupid="+group+" AND groups.id="+group+" AND NOT deleted " + ignq + " ORDER BY sticky DESC,lastmod DESC LIMIT "+topics+" OFFSET "+offset);
  } else {
	rs=st.executeQuery("SELECT topics.title as subj, lastmod, nick, topics.id as msgid, deleted, topics.stat1, topics.stat2, topics.stat3, topics.stat4, topics.sticky FROM topics,groups,users, sections WHERE sections.id=groups.section AND (topics.moderate OR NOT sections.moderate) AND topics.userid=users.id AND topics.groupid="+group+" AND groups.id="+group+" AND NOT deleted ORDER BY sticky DESC,lastmod DESC LIMIT "+topics+" OFFSET "+offset);
  }
  
  while (rs.next()) {
    StringBuffer outbuf = new StringBuffer();
    int stat1 = rs.getInt("stat1");

    Timestamp lastmod=rs.getTimestamp("lastmod");
    if (lastmod==null) lastmod=new Timestamp(0);

    outbuf.append("<tr><td>");
    if (rs.getBoolean("deleted")) outbuf.append("[X] ");
	else if(rs.getBoolean("sticky")) outbuf.append("<img src=\"img/paper_clip.gif\" alt=\"Прикреплено\" title=\"Прикреплено\"> ");

    int pagesInCurrent = (int) Math.ceil(stat1 / messages);

    if (firstPage) {
      if (pagesInCurrent <= 1) {
        outbuf.append("<a href=\"jump-message.jsp?msgid=").append(rs.getInt("msgid")).append("&amp;lastmod=").append(lastmod.getTime()).append("\" rev=contents>").append(StringUtil.makeTitle(rs.getString("subj"))).append("</a>");
      } else {
        outbuf.append("<a href=\"jump-message.jsp?msgid=").append(rs.getInt("msgid")).append("\" rev=contents>").append(StringUtil.makeTitle(rs.getString("subj"))).append("</a>");
      }
    } else {
      outbuf.append("<a href=\"jump-message.jsp?msgid=").append(rs.getInt("msgid")).append("\" rev=contents>").append(StringUtil.makeTitle(rs.getString("subj"))).append("</a>");
    }

    if (pagesInCurrent > 1) {
      outbuf.append("&nbsp;(стр.");

      for (int i = 1; i < pagesInCurrent; i++) {
        outbuf.append(" <a href=\"jump-message.jsp?msgid=").append(rs.getInt("msgid"));
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

		if (stat1>0)
                  outbuf.append("<b>").append(stat1).append("</b>/");
		else
			outbuf.append("-/");

		if (stat3>0)
                  outbuf.append("<b>").append(stat3).append("</b>/");
		else
			outbuf.append("-/");

		if (stat4>0)
                  outbuf.append("<b>").append(stat4).append("</b>");
		else
			outbuf.append("-");



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
	String ignoredAdd = tmpl.getProf().getBoolean("showignored")!=showIgnored?("&amp;showignored=" + (showIgnored ? "t" : "f")):"";
	
	out.print("<div style=\"float: left\">");
	if (offset==0)
		out.print("<b>Назад</b>");
	else
		if ((offset-topics)==0)
			out.print("<a rel=prev rev=next href=\"group-lastmod.jsp?group=" + group + ignoredAdd + "\">Назад</a>");
		else
			out.print("<a rel=prev rev=next href=\"group-lastmod.jsp?group=" + group + "&amp;offset=" + (offset-topics) + ignoredAdd + "\">Назад</a>");
	out.print("</div>");
	if (offset>0)
		out.print("<div style=\"text-align: center\"><a rel=start href=\"group-lastmod.jsp?group=" + group + ignoredAdd + "\">Начало</a></div>");
	out.print("<div style=\"float: right\">");
	if (offset==topics*pages)
		out.print("<b>Вперед</b>");
	else
		out.print("<a rel=next rev=prev href=\"group-lastmod.jsp?group=" + group + "&amp;offset=" + (offset+topics) + ignoredAdd + "\">Вперед</a>");

	out.print("</div>");

%>
</td></tr></table>
</div>
<div align=center><p>
<%
  for (int i=0; i<pages+1; i++) {
    if (i!=0 && i!=pages && Math.abs(i*topics-offset)>7*topics)
      continue;

    if (i==pages)
        out.print("[<a href=\"group-lastmod.jsp?group=" + group + "&amp;offset=" + (i*topics) + ignoredAdd + "\">конец</a>] ");
    else if (i*topics==offset)
      out.print("[<b>"+(pages+1-i)+"</b>] ");
    else
      if (i!=0)
        out.print("[<a href=\"group-lastmod.jsp?group=" + group + "&amp;offset=" + (i*topics) + ignoredAdd + "\">"+(pages+1-i)+"</a>] ");
      else
        out.print("[<a href=\"group-lastmod.jsp?group=" + group + ignoredAdd + "\">начало</a>] ");
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
    if (db!=null) db.close();
  }
%>
<%= tmpl.DocumentFooter() %>
