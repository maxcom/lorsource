<%@ tag import="org.springframework.web.context.WebApplicationContext" %>
<%@ tag import="org.springframework.web.servlet.support.RequestContextUtils" %>
<%@ tag import="ru.org.linux.site.Template" %>
<%@ tag import="ru.org.linux.spring.SiteConfig" %>
<%@ tag pageEncoding="UTF-8"%>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib prefix="lor" uri="http://www.linux.org.ru" %>
<%@ taglib prefix="spring" uri="http://www.springframework.org/tags" %>

<%@ attribute name="ipBlockInfo" required="false" type="ru.org.linux.auth.IPBlockInfo" %>
<%--
  ~ Copyright 1998-2024 Linux.org.ru
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
  Template tmpl = Template.getTemplate();

  if (!tmpl.isSessionAuthorized() || ipBlockInfo != null && ipBlockInfo.isCaptchaRequired()) {
    WebApplicationContext ctx=RequestContextUtils.findWebApplicationContext(request);

    String key = ((SiteConfig) ctx.getBean("siteConfig")).getCaptchaPublicKey();
%>
<script src="https://js.hcaptcha.com/1/api.js" async defer></script>
<div class="h-captcha" data-sitekey="<%= key %>"></div>
<%
  }
%>
