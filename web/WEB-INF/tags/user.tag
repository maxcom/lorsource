<%@ tag import="ru.org.linux.site.User"
        pageEncoding="UTF-8"
%><%--
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
  --%><%@ attribute name="db" required="false" type="java.sql.Connection" %><%@
        attribute name="decorate" required="false" type="java.lang.Boolean" %><%@
        attribute name="user" required="false" type="ru.org.linux.site.User" %><%@
        attribute name="id" required="false" type="java.lang.Integer" %><%
  if (user!=null) {
    id = user.getId();
  }

  if (id == 2) {
    out.print("anonymous");
  } else {
    if (user==null) {
      user = User.getUserCached(db, id);
    }

    if (decorate != null && decorate) {
      if (user.isBlocked()) {
        out.print("<s>");
      }
    }

    out.print(user.getNick());

    if (decorate != null && decorate) {
      if (user.isBlocked()) {
        out.print("</s>");
      }
    }
  }
%>