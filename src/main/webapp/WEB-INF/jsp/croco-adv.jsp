<%@ page import="org.apache.commons.lang.math.RandomUtils" %>
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

<%@ page contentType="text/html;charset=UTF-8" language="java" %>

<div class="infoblock" style="border: 1px solid #777; margin-left: auto; margin-right: auto; text-align: center; width: 728px">
  <%
    String header;
    String text;

    switch (RandomUtils.nextInt(4)) {
      case 0:
        header = "НАУЧИ КОМПЬЮТЕР ВАРИТЬ КОФЕ";
        text = "управление электрическими цепями с помощью компьютера<br>" +
               "лучший подарок для техногика; только открытые программы";
        break;
      case 1:
        header = "СКАЖИ СВОЕМУ КОМПЬЮТЕРУ, ЧТОБЫ ЗАПЕР ДВЕРЬ";
        text = "любительская автоматизация; устройство с открытой прошивкой<br>" +
                "исходные тексты всех программ, открытые библиотеки";
        break;
      case 2:
        header = "ЗАСТАВЬ КОМПЬЮТЕР ПОЛИВАТЬ ОГОРОД";
        text = "автоматизация своими руками: электроприборы под контролем компьютера<br>" +
               "beware of programmers who carry screwdrivers!";
        break;
      case 3:
        header = "ПОСАДИ КОМПЬЮТЕР НА ЦЕПЬ И ЗАСТАВЬ ЛАЯТЬ!";
        text = "домашняя автоматизация: сделай сам; лучший подарок для техногика<br>"
               ;
        break;
      default:
        throw new RuntimeException("Oops!");
    }
  %>

  <h2><a rel="nofollow" href="http://www.unicontrollers.com/products/unc01x"><%= header %></a></h2>
  <%= text %><br>
  <a rel="nofollow" href="http://www.unicontrollers.com/products/unc01x">http://www.unicontrollers.com/products/unc01x</a>
</div>