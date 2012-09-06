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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.context.support.WebApplicationContextUtils;
import org.springframework.web.filter.GenericFilterBean;
import ru.org.linux.spring.Configuration;
import ru.org.linux.user.Profile;

import javax.servlet.*;
import javax.servlet.http.HttpServletRequest;
import java.io.IOException;

/**
 */
public class ConfigurationFilter extends GenericFilterBean implements InitializingBean {
  private static final Log logger = LogFactory.getLog(ConfigurationFilter.class);

  public void doFilter(ServletRequest req, ServletResponse res, FilterChain chain)
      throws IOException, ServletException {

    WebApplicationContext ctx = WebApplicationContextUtils.getWebApplicationContext(getServletContext());
    HttpServletRequest request = (HttpServletRequest) req;
    request.getSession().setAttribute("configuration", ctx.getBean(Configuration.class));
    if(AuthUtil.isSessionAuthorized()) {
      request.getSession().setAttribute("currentStyle", AuthUtil.getCurrentUser().getStyle());
      request.getSession().setAttribute("currentProfile", AuthUtil.getCurrentProfile());
    } else {
      request.getSession().setAttribute("currentStyle", "tango");
      request.getSession().setAttribute("currentProfile", Profile.getDefaultProfile());
    }

    chain.doFilter(req, res);
  }
}
