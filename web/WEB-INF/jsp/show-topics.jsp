<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib tagdir="/WEB-INF/tags" prefix="lor" %>
<%@ page contentType="text/html; charset=utf-8" %>
<%@ page import="ru.org.linux.site.Template" %>
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
<jsp:include page="/WEB-INF/jsp/head.jsp"/>

<%
  String nick = (String) request.getAttribute("nick");
  int offset = (Integer) request.getAttribute("offset");
  boolean firstPage = (Boolean) request.getAttribute("firstPage");
  int pages = (Integer) request.getAttribute("pages");

  int topicsInPage = tmpl.getProf().getInt("topics");

%>
<c:set var="title">Последние
  <c:if test="${firstPage}">
    ${topicsInPage}
  </c:if>
  <c:if test="${not firstPage}">
    ${count - offset} - ${count - offset - topicsInPage}
  </c:if>
  тем пользователя ${nick}
</c:set>
<title>${title}</title>
<jsp:include page="/WEB-INF/jsp/header.jsp"/>
<h1>${title}</h1>

<div class=forum>
  <table width="100%" class="message-table">
    <thead>
    <tr>
      <th>Раздел</th>
      <th>Группа</th>
      <th>Заглавие</th>
      <th>Последнее добавление</th>
    </tr>
    <tbody>
    <c:forEach items="${topicsList}" var="topic">
      <tr>
        <td>${topic.sectionTitle}</td>
        <td>${topic.groupTitle}</td>
        <td><a href="view-message.jsp?msgid=${topic.msgid}" rev="contents">
          <c:if test="${topic.deleted}">
            [X]
          </c:if>
            ${topic.subj}
        </a></td>
        <td class="dateinterval">
          <lor:dateinterval date="${topic.lastmod}"/>
        </td>
      </tr>
    </c:forEach>
    </tbody>
    <tfoot>
    <tr>
      <td colspan=5><p>
        <div style="float: left">
          <%
  // НАЗАД
  if (firstPage) {
    out.print("");
  } else if (offset == pages * topicsInPage) {
    out.print("<a href=\"show-topics.jsp?nick=" + nick + "\">← первая</a> ");
  } else {
    out.print("<a rel=prev rev=next href=\"show-topics.jsp?nick=" + nick + "&amp;offset=" + (offset + topicsInPage) + "\">← назад</a>");
  }

  out.print("</div>");

  // ВПЕРЕД
  out.print("<div style=\"float: right\">");

  if (firstPage) {
    out.print("<a rel=next rev=prev href=\"show-topics.jsp?nick=" + nick + "&amp;offset=" + (pages * topicsInPage) + "\">архив →</a>");
  } else if (offset == 0 && !firstPage) {
  } else {
    out.print("<a rel=next rev=prev href=\"show-topics.jsp?nick=" + nick + "&amp;offset=" + (offset - topicsInPage) + "\">вперед →</a>");
  }
%>
        </div>
      </td>
    </tr>
    </tfoot>
  </table>
</div>
<div align=center><p>
    <%
	for (int i=0; i<=pages+1; i++) {
	  if (firstPage) {
		if (i!=0 && i!=(pages+1) && i>7) {
                  continue;
                }
	  } else {
		if (i!=0 && i!=(pages+1) && Math.abs((pages+1-i)*topicsInPage-offset)>7*topicsInPage) {
                  continue;
                }
	  }
	  
	  if (i==pages+1) {
		if (offset!=0 || firstPage) {
                  out.print("[<a href=\"show-topics.jsp?nick=" + nick + "&amp;offset=0\">последняя</a>] ");
                } else {
                  out.print("[<b>последняя</b>] ");
                }
	  } else if (i==0) {
		if (firstPage) {
                  out.print("[<b>первая</b>] ");
                } else {
                  out.print("[<a href=\"show-topics.jsp?nick=" + nick + "\">первая</a>] ");
                }
	  } else if ((pages+1-i)*topicsInPage==offset) {
		out.print("<b>"+(pages+1-i)+"</b> ");
	  } else {
		out.print("<a href=\"show-topics.jsp?nick="+nick+"&amp;offset="+((pages+1-i)*topicsInPage)+"\">"+(pages+1-i)+"</a> ");
	  }
	}
%>
</div>
<jsp:include page="/WEB-INF/jsp/footer.jsp"/>
