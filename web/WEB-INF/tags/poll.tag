<%@ tag import="ru.org.linux.site.PollVariant" %>
<%@ tag import="java.util.List" %>
<%@ tag import="ru.org.linux.util.ImageInfo" %>
<%@ tag import="ru.org.linux.util.HTMLFormatter" %>
<%@ tag import="ru.org.linux.site.Poll" %>
<%@ tag import="ru.org.linux.site.Template" %>
<%@ tag pageEncoding="UTF-8"%>
<%--
  ~ Copyright 1998-2010 Linux.org.ru
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
<%@ attribute name="db" required="true" type="java.sql.Connection" %>
<%@ attribute name="poll" required="true" type="ru.org.linux.site.Poll" %>
<%@ attribute name="highlight" required="false" type="java.lang.Integer" %>
<table class=poll>
<%
  Template tmpl = Template.getTemplate(request);

  int max = poll.getMaxVote(db);
  List<PollVariant> vars = poll.getPollVariants(db, Poll.ORDER_VOTES);
  ImageInfo info = new ImageInfo(tmpl.getConfig().getProperty("HTMLPathPrefix") + tmpl.getProf().getString("style") + "/img/votes.png");
  int total = 0;
  for (PollVariant var : vars) {
    out.append("<tr><td>");
    int id = var.getId();
    int votes = var.getVotes();
    if (highlight!=null && id == highlight) {
      out.append("<b>");
    }
    out.append(HTMLFormatter.htmlSpecialChars(var.getLabel()));
    if (highlight!=null && id == highlight) {
      out.append("</b>");
    }
    out.append("</td><td>");
    out.append(Integer.toString(votes));
    out.append("</td><td>");
    total += votes;
    for (int i = 0; i < 20 * votes / max; i++) {
      out.append("<img src=\"/").append(tmpl.getProf().getString("style")).append("/img/votes.png\" alt=\"*\" ").append(info.getCode()).append('>');
    }
    out.append("</td></tr>");
  }
%><tr>
    <td colspan=2>Всего голосов: <%= total %></td>
</tr>
</table>