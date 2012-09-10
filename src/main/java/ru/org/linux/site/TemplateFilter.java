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

package ru.org.linux.site;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.context.support.WebApplicationContextUtils;
import ru.org.linux.spring.Configuration;
import ru.org.linux.user.UserDao;

import javax.servlet.*;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

@Deprecated
public class TemplateFilter implements Filter {
  private static final Log logger = LogFactory.getLog(TemplateFilter.class);

  private FilterConfig filterConfig;

  private UserDao userDao;
  private Configuration configuration;

  @Override
  public void init(FilterConfig filterConfig) throws ServletException {
    this.filterConfig = filterConfig;

    WebApplicationContext ctx = WebApplicationContextUtils.getWebApplicationContext(filterConfig.getServletContext());

    userDao = ctx.getBean("userDao", UserDao.class);
    configuration = ctx.getBean(Configuration.class);

    MemCachedSettings.setMainUrl(configuration.getMainUrl());
  }

  @Override
  public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse, FilterChain filterChain) throws ServletException, IOException {
    if (filterConfig==null) {
      return;
    }

    HttpServletRequest hsr = (HttpServletRequest) servletRequest;

    String path = hsr.getServletPath();

    if (
            !path.endsWith(".css") &&
            !path.endsWith(".png") &&
            !path.endsWith(".jpg")
    ) {
      try {
        Template tmpl = new Template(
                (HttpServletRequest) servletRequest,
                (HttpServletResponse) servletResponse,
                userDao,
                configuration
        );

        servletRequest.setAttribute("template", tmpl);
      } catch (Exception ex) {
        logger.fatal("Can't build Template", ex);
        return;
      }
    }

    filterChain.doFilter(servletRequest, servletResponse);
  }

  @Override
  public void destroy() {
    filterConfig = null;
  }

}
