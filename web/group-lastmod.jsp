<%@ page contentType="text/html; charset=koi8-r"%>
<%@ page import="java.net.URLEncoder,java.sql.Connection,java.sql.ResultSet,java.sql.Statement,java.sql.Timestamp,java.util.Date,ru.org.linux.site.BadGroupException" errorPage="/error.jsp" buffer="200kb"%>
<%@ page import="ru.org.linux.site.MissingParameterException"%>
<%@ page import="ru.org.linux.site.Template"%>
<%@ page import="ru.org.linux.util.ImageInfo"%>
<%@ page import="ru.org.linux.util.StringUtil"%>
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
        <table>
          <tr valign=middle>
            <td>
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
    </td>
  </tr>
</table>
</div>
</form>
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
		out.print("<em>");
		out.print(des);
		out.print("</em>");
	}

	rs.close();

%>
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
  String order="lastmod";
  double messages = tmpl.getProf().getInt("messages");

  rs=st.executeQuery("SELECT topics.title as subj, lastmod, nick, topics.id as msgid, deleted, topics.stat1, topics.stat2, topics.stat3, topics.stat4 FROM topics,groups,users, sections WHERE sections.id=groups.section AND (topics.moderate OR NOT sections.moderate) AND topics.userid=users.id AND topics.groupid="+group+" AND groups.id="+group+" AND NOT deleted ORDER BY "+order+" DESC LIMIT "+topics+" OFFSET "+offset);

  while (rs.next()) {
    Timestamp lastmod=rs.getTimestamp("lastmod");
    if (lastmod==null) lastmod=new Timestamp(0);

    out.print("<tr><td>");
    if (rs.getBoolean("deleted")) out.print("[X] ");

    out.print("<a href=\"jump-message.jsp?msgid=" + rs.getInt("msgid") + "&amp;lastmod="+lastmod.getTime()+"\" rev=contents>" +  StringUtil.makeTitle(rs.getString("subj")) + "</a>");

    int stat1=rs.getInt("stat1");

    int pagesInCurrent = (int) Math.ceil(stat1 / messages);
    if (pagesInCurrent > 1 ) {
      out.print("&nbsp;(стр.");
      for (int i = 1; i < pagesInCurrent; i++) {
        out.print(" <a href=\""+"jump-message.jsp?msgid="+rs.getInt("msgid")+"&amp;lastmod="+lastmod.getTime()+"&amp;page="+i+"\">"+(i + 1)+"</a>");
      }
      out.print(')');
    }

    out.print(" (" + rs.getString("nick") + ") ");
                out.print("</td>");
		out.print("<td align=center>");
		int stat3=rs.getInt("stat3");
		int stat4=rs.getInt("stat4");

		if (stat1>0)
			out.print("<b>"+stat1+"</b>/");
		else
			out.print("-/");

		if (stat3>0)
			out.print("<b>"+stat3+"</b>/");
		else
			out.print("-/");

		if (stat4>0)
			out.print("<b>"+stat4+"</b>");
		else
			out.print("-");



		out.print("</td></tr>");
  }
	rs.close();
%>
  <tfoot><tr><td colspan=2><p>
<%
	out.print("<div style=\"float: left\">");
	if (offset==0)
		out.print("<b>Назад</b>");
	else
		if ((offset-topics)==0)
			out.print("<a rel=prev rev=next href=\"group-lastmod.jsp?group="+group+"\">Назад</a>");
		else
			out.print("<a rel=prev rev=next href=\"group-lastmod.jsp?group="+group+"&amp;offset="+(offset-topics)+"\">Назад</a>");
	out.print("</div>");
	if (offset>0)
		out.print("<div style=\"text-align: center\"><a rel=start href=\"group-lastmod.jsp?group="+group+"\">Начало</a></div>");
	out.print("<div style=\"float: right\">");
	if (offset==topics*pages)
		out.print("<b>Вперед</b>");
	else
		out.print("<a rel=next rev=prev href=\"group-lastmod.jsp?group="+group+"&amp;offset="+(offset+topics)+"\">Вперед</a>");

	out.print("</div>");

%>
</td></tr></table>
</div></div>
<div align=center><p>
<%
  for (int i=0; i<pages+1; i++) {
    if (i!=0 && i!=pages && Math.abs(i*topics-offset)>7*topics)
      continue;

    if (i==pages)
        out.print("[<a href=\"group-lastmod.jsp?group="+group+"&amp;offset="+(i*topics)+"\">конец</a>] ");
    else if (i*topics==offset)
      out.print("[<b>"+(pages+1-i)+"</b>] ");
    else
      if (i!=0)
        out.print("[<a href=\"group-lastmod.jsp?group="+group+"&amp;offset="+(i*topics)+"\">"+(pages+1-i)+"</a>] ");
      else
        out.print("[<a href=\"group-lastmod.jsp?group="+group+"\">начало</a>] ");
  }
%>
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
