<%@ tag import="net.tanesha.recaptcha.ReCaptcha" %>
<%@ tag import="org.springframework.web.context.WebApplicationContext" %>
<%@ tag import="org.springframework.web.servlet.support.RequestContextUtils" %>
<%@ tag import="ru.org.linux.site.Template" %>
<%@ tag pageEncoding="UTF-8"%>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib prefix="lor" uri="http://www.linux.org.ru" %>
<%@ taglib prefix="spring" uri="http://www.springframework.org/tags" %>

<%@ attribute name="ipBlockInfo" required="true" type="ru.org.linux.auth.IPBlockInfo" %>
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

<%
  if (!Template.isSessionAuthorized(session) || ipBlockInfo != null && ipBlockInfo.isCaptchaRequired()) {
%>
    <p>
<%
    WebApplicationContext ctx=RequestContextUtils.getWebApplicationContext(request);

    out.print(((ReCaptcha) ctx.getBean("reCaptcha")).createRecaptchaHtml(null, null));
  }
%>
