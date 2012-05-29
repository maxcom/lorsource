<%@ tag import="ru.org.linux.tag.TagService" %>
<%@ tag import="ru.org.linux.topic.TopicListController" %>
<%@ tag import="ru.org.linux.util.StringUtil" %>
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

    if (TagService.isGoodTag(mtag)) {
      buf.append("<a class=tag rel=tag href=\"").append(TopicListController.tagListUrl(mtag)).append("\">").append(StringUtil.escapeHtml(mtag)).append("</a>");
    } else {
      buf.append(StringUtil.escapeHtml(mtag));
    }
  }

  String result = buf.toString();
%>
  Метки: <%= result %>
</p>


