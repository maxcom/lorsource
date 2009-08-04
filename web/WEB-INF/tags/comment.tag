<%@ tag import="ru.org.linux.site.CommentView" %>
<%@ tag import="ru.org.linux.site.Template" %>
<%@ tag pageEncoding="UTF-8"%>
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

<%@ attribute name="comment" required="true" type="ru.org.linux.site.Comment" %>
<%@ attribute name="db" required="true" type="java.sql.Connection" %>
<%@ attribute name="comments" required="true" type="ru.org.linux.site.CommentList" %>
<%@ attribute name="user" required="true" type="java.lang.String"%>
<%@ attribute name="expired" required="true" type="java.lang.Boolean"%>

<%
  CommentView view = new CommentView();
  Template tmpl = Template.getTemplate(request);

  out.append(view.printMessage(comment, tmpl, db, comments, true, tmpl.isModeratorSession(), user, expired));
%>
