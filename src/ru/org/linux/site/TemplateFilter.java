package ru.org.linux.site;

import java.io.IOException;
import java.util.Properties;
import java.util.logging.Logger;

import javax.servlet.*;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.context.support.WebApplicationContextUtils;

import ru.org.linux.util.StringUtil;

public class TemplateFilter implements Filter {
  private static final Logger logger = Logger.getLogger("ru.org.linux");

  private FilterConfig filterConfig;

  private Properties properties;

  public void init(FilterConfig filterConfig) throws ServletException {
    this.filterConfig = filterConfig;

    properties=getProperties(filterConfig.getServletContext());

    MemCachedSettings.setMainUrl(properties.getProperty("MainUrl"));
  }

  public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse, FilterChain filterChain) throws ServletException, IOException {
    if (filterConfig==null) {
      return;
    }

    try {
      Template tmpl = new Template((HttpServletRequest) servletRequest, properties, (HttpServletResponse) servletResponse);

      servletRequest.setAttribute("template", tmpl);
    } catch (Exception ex) {
      logger.severe("Can't build Template "+ StringUtil.getStackTrace(ex));
      return;
    }

    filterChain.doFilter(servletRequest, servletResponse);
  }

  public void destroy() {
    filterConfig = null;
  }

  private static Properties getProperties(ServletContext sc)  {
    WebApplicationContext ctx = WebApplicationContextUtils.getWebApplicationContext(sc);

    Properties prop = (Properties) ctx.getBean("config.properties");
    if (prop.isEmpty()) {
      prop = (Properties) ctx.getBean("config.properties.dist");
    }

    return prop;
  }
}
