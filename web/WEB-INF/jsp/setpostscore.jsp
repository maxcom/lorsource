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
<jsp:include page="/WEB-INF/jsp/head.jsp"/>

<title>Смена параметров сообщения</title>
<jsp:include page="/WEB-INF/jsp/header.jsp"/>
<%
  Message msg = (Message) request.getAttribute("message");

  int postscore = msg.getPostScore();
  boolean sticky = msg.isSticky();
  boolean notop = msg.isNotop();

%>
<h1>Смена режима параметров сообщения</h1>
Данная форма предназначена для администраторов сайта и пользователей,
имеющих права подтверждения сообщений.
<form method=POST action="setpostscore.jsp">
  <input type=hidden name=msgid value="${message.id}">
  <br>
  Текущий уровень
  записи: <%= (postscore < 0 ? "только для модераторов" : Integer.toString(postscore)) %>
  <select name="postscore">
    <option value="0">0 - без ограничений</option>
    <option value="50">50 - для зарегистрированных</option>
    <option value="100">100 - одна "звезда"</option>
    <option value="200">200 - две "звезды"</option>
    <option value="300">300 - три "звезды"</option>
    <option value="400">400 - четыре "звезды"</option>
    <option value="500">500 - пять "звезд"</option>
    <option value="-1">только для модераторов</option>
  </select><br>
  Прикрепить сообщение <input type=checkbox name="sticky" <%= sticky?"checked":"" %>><br>
  Удалить из top10 <input type=checkbox name="notop" <%= notop?"checked":"" %>><br>
  <input type=submit value="Изменить">
</form>
<jsp:include page="/WEB-INF/jsp/footer.jsp"/>
