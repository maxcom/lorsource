<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ page contentType="text/html; charset=utf-8" %>
<%@ page import="ru.org.linux.site.Message" %>
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
<%--@elvariable id="message" type="ru.org.linux.site.Message"--%>
<%--@elvariable id="group" type="ru.org.linux.dto.GroupDto"--%>

<jsp:include page="/WEB-INF/jsp/head.jsp"/>

<title>Смена параметров сообщения</title>
<jsp:include page="/WEB-INF/jsp/header.jsp"/>
<%
  Message msg = (Message) request.getAttribute("message");

  int postscore = msg.getPostScore();
  boolean sticky = msg.isSticky();
  boolean notop = msg.isNotop();
  boolean minor = msg.isMinor();
%>
<h1>Смена режима параметров сообщения</h1>
Данная форма предназначена для администраторов сайта и пользователей,
имеющих права подтверждения сообщений.
<form method=POST action="setpostscore.jsp">
  <input type=hidden name=msgid value="${message.id}">
  <br>
  <label>
  Ограничение комментирования:

  <select name="postscore">
    <option <%= postscore==Message.POSTSCORE_UNRESTRICTED?"selected":"" %> value="<%= Message.POSTSCORE_UNRESTRICTED %>">без ограничений</option>
    <option <%= postscore==Message.POSTSCORE_REGISTERED_ONLY?"selected":"" %> value="<%= Message.POSTSCORE_REGISTERED_ONLY %>">для зарегистрированных</option>
    <option <%= postscore==50?"selected":"" %> value="50">score>=50</option>
    <option <%= postscore==100?"selected":"" %> value="100">100 - одна "звезда"</option>
    <option <%= postscore==200?"selected":"" %> value="200">200 - две "звезды"</option>
    <option <%= postscore==300?"selected":"" %> value="300">300 - три "звезды"</option>
    <option <%= postscore==400?"selected":"" %> value="400">400 - четыре "звезды"</option>
    <option <%= postscore==500?"selected":"" %> value="500">500 - пять "звезд"</option>
    <option <%= postscore==Message.POSTSCORE_MOD_AUTHOR?"selected":"" %> value="<%= Message.POSTSCORE_MOD_AUTHOR%>">только для модераторов и автора</option>
    <option <%= postscore==Message.POSTSCORE_MODERATORS_ONLY?"selected":"" %> value="<%= Message.POSTSCORE_MODERATORS_ONLY%>">только для модераторов</option>
  </select>
    </label>
    <br>
  <c:if test="${not group.moderated}">
    <label>Прикрепить сообщение <input type=checkbox name="sticky" <%= sticky?"checked":"" %>></label><br>
  </c:if>
  <label>Удалить из top10 <input type=checkbox name="notop" <%= notop?"checked":"" %>></label><br>
  <c:if test="${group.moderated}">
    <label>Мини-новость <input type=checkbox name="minor" <%= minor?"checked":"" %>></label><br>
  </c:if>
  <input type=submit value="Изменить">
</form>
<jsp:include page="/WEB-INF/jsp/footer.jsp"/>
