<%@ page import="org.javabb.bbcode.BBCodeProcessor" %>
<%@ page contentType="text/html;charset=UTF-8" language="java" %>

<html>
  <head><title>BBCode test page</title></head>
  <style type="text/css">
    div.quote {
      border: 1px dotted;
      margin-bottom: 1em;
    }

    div.quote h3 {
      margin-top: 0;
      margin-bottom: 0;
      font-size: medium;
    }

    div.code {
      border: 1px dotted;
      margin-bottom: 1em;
      font-family: monospace;
    }
  </style>
  <body>

<%
  String text = request.getParameter("text");
  if (text==null) {
    text="";
  }

  if (request.getMethod().equals("POST")) {

  BBCodeProcessor proc = new BBCodeProcessor();

  out.print(proc.preparePostText(text));
  out.print("<hr>");
} %>

    <form action="bbtest.jsp" method="POST">
      <textarea rows="20" cols="80" name="text"><%= text %></textarea>
      <br>
      <input type="submit" value="Submit" >
    </form>

  </body>
</html>
