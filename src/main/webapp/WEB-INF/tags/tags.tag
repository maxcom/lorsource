<%@ tag import="java.io.UnsupportedEncodingException" %>
<%@ tag import="java.net.URLEncoder" %>
<%@ tag import="java.util.List" %>
<%--
  ~ Copyright 1998-2012 Linux.org.ru
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
<%@ tag pageEncoding="UTF-8" %>
<%@ attribute name="list" required="true" type="java.util.List" %>
<p class="tags">
<%
  StringBuilder buf = new StringBuilder();

  for (String mtag : (List<String>) list) {
    if (buf.length() > 0) {
      buf.append(", ");
    }

    try {
      buf.append("<a rel=tag href=\"view-news.jsp?tag=").append(URLEncoder.encode(mtag, "UTF-8")).append("\">").append(mtag).append("</a>");
    } catch (UnsupportedEncodingException e) {
      throw new RuntimeException(e);
    }
  }

  String result = buf.toString();
%>
  Метки: <span class=tag><%= result %></span>
</p>


