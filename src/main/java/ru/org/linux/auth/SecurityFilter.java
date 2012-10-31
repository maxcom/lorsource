/*
 * Copyright 1998-2012 Linux.org.ru
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 */

package ru.org.linux.auth;

import org.springframework.beans.factory.InitializingBean;
import org.springframework.security.core.Authentication;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.context.support.WebApplicationContextUtils;
import org.springframework.web.filter.GenericFilterBean;
import ru.org.linux.csrf.CSRFProtectionService;
import ru.org.linux.site.Template;
import ru.org.linux.spring.Configuration;
import ru.org.linux.user.Profile;
import ru.org.linux.util.LorHttpUtils;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.io.IOException;
import java.util.Properties;

/**
 */
public class SecurityFilter extends GenericFilterBean implements InitializingBean {

  public void doFilter(ServletRequest req, ServletResponse res, FilterChain chain)
      throws IOException, ServletException {

    WebApplicationContext ctx = WebApplicationContextUtils.getWebApplicationContext(getServletContext());
    HttpServletRequest request = (HttpServletRequest) req;
    request.getSession().setAttribute("configuration", ctx.getBean(Configuration.class));
    request.getSession().setAttribute("template", new Template(ctx));
    if (AuthUtil.isSessionAuthorized()) {
      request.getSession().setAttribute("currentStyle", AuthUtil.getCurrentUser().getStyle());
      request.getSession().setAttribute("currentProfile", AuthUtil.getCurrentProfile());
      request.getSession().setAttribute("currentProperties", AuthUtil.getProf());
      forWikiManipulation(request, (HttpServletResponse) res, AuthUtil.getAuthentication());
    } else {
      request.getSession().setAttribute("currentStyle", "tango");
      request.getSession().setAttribute("currentProfile", Profile.getDefaultProfile());
      request.getSession().setAttribute("currentProperties", AuthUtil.getProf());
    }
    request.setCharacterEncoding("utf-8"); // блядский tomcat
    CSRFManipulation(request, (HttpServletResponse) res);
    chain.doFilter(req, res);
  }

  private void CSRFManipulation(HttpServletRequest request, HttpServletResponse response) {
    Properties cookies = LorHttpUtils.getCookies(request.getCookies());
    if (cookies.get(CSRFProtectionService.CSRF_COOKIE) == null) {
      CSRFProtectionService.generateCSRFCookie(request, response);
    } else {
      request.setAttribute(CSRFProtectionService.CSRF_ATTRIBUTE, cookies.getProperty(CSRFProtectionService.CSRF_COOKIE).trim());
    }
    response.addHeader("Cache-Control", "private");
  }

  private void forWikiManipulation(HttpServletRequest request, HttpServletResponse response, Authentication authentication) {
    HttpSession session = request.getSession();
    AuthUtil.getCurrentUser().acegiSecurityHack(response, session);
  }

}
