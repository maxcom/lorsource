<%@ page pageEncoding="koi8-r" contentType="text/html; charset=utf-8"%>
<%@ page import="java.net.URLEncoder,java.sql.Connection"   buffer="60kb"%>
<%@ page import="javax.servlet.http.HttpServletResponse" %>
<%@ page import="ru.org.linux.site.*" %>
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

<% Template tmpl = Template.getTemplate(request); %>
<jsp:include page="/WEB-INF/jsp/head.jsp"/>

<%
  int msgid = new ServletParameterParser(request).getInt("msgid");

  String redirectUrl = tmpl.getMainUrl() + "view-message.jsp?msgid=" + msgid;
  StringBuffer options = new StringBuffer();

  if (request.getParameter("page") != null) {
    options.append("&page=");
    options.append(URLEncoder.encode(request.getParameter("page")));
  }

  if (request.getParameter("back") != null) {
    options.append("&back=");
    options.append(URLEncoder.encode(request.getParameter("back")));
  }

  if (request.getParameter("nocache") != null) {
    options.append("&nocache=");
    options.append(URLEncoder.encode(request.getParameter("nocache")));
  }

  if (request.getParameter("cid") != null) {
    Connection db = null;
    int cid = new ServletParameterParser(request).getInt("cid");

    try {
      db = LorDataSource.getConnection();
      Message topic = new Message(db, msgid);
      CommentList comments = CommentList.getCommentList(db, topic, false);
      CommentNode node = comments.getNode(cid);
      if (node == null) {
        throw new MessageNotFoundException(cid, "Сообщение #" + cid + " было удалено или не существует");
      }

      int pagenum = comments.getCommentPage(node.getComment(), tmpl);

      if (pagenum > 0) {
        options.append("&page=");
        options.append(pagenum);
      }

      if (!topic.isExpired() && topic.getPageCount(tmpl.getProf().getInt("messages"))-1==pagenum) {
        options.append("&lastmod=");
        options.append(topic.getLastModified().getTime());        
      }

      options.append("#comment-");
      options.append(cid);
    } finally {
      if (db != null) {
        db.close();
      }
    }
  }

  response.setHeader("Location", redirectUrl + options.toString());
  response.setStatus(HttpServletResponse.SC_MOVED_TEMPORARILY);
%>
<jsp:include page="WEB-INF/jsp/header.jsp"/>
Go to: <%= redirectUrl %>
<jsp:include page="WEB-INF/jsp/footer.jsp"/>
