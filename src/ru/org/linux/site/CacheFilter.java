package ru.org.linux.site;

import java.io.IOException;
import java.util.Enumeration;
import java.util.logging.Logger;

import javax.servlet.*;
import javax.servlet.http.HttpServletResponse;

/**
 *  Project Core
 */
public class CacheFilter implements Filter {

  private FilterConfig objFilterConfig;
  
  // Create a log attribute to allow access to log files
  private static final Logger logger = Logger.getLogger("ru.org.linux");
  private static final boolean isDebugEnabled = false;
  private static final String VERSION_STRING = CacheFilter.class.getName() + '/' + "2.4"; // WTF is com.ieseries.core.Constants?

  /**
   * init
   * @param filterConfig the filter configuration object
   */
  public void init(FilterConfig filterConfig) {
    objFilterConfig = filterConfig;
  }

  /**
   * doFilter
   * @param req the ServletRequest object
   * @param res the ServletResponse object
   * @param filterChain the FilterChain
   * @throws IOException
   * @throws ServletException
   */
  public void doFilter(ServletRequest req, ServletResponse res, FilterChain filterChain) throws IOException, ServletException {
    if (isDebugEnabled) logger.info("Doing Filter Cache");
    HttpServletResponse response = (HttpServletResponse) res;

    // set the provided HTTP response parameters
    Enumeration enu = objFilterConfig.getInitParameterNames();
    while ( enu.hasMoreElements() ) {
      String headerName = (String) enu.nextElement();
      // response.setHeader(headerName,objFilterConfig.getInitParameter(headerName));
      // RG : use addHeader not setHeader so multiple headers can be added...
      if (isDebugEnabled) logger.info("Setting Header : " + objFilterConfig.getInitParameter(headerName));
      response.addHeader(headerName, objFilterConfig.getInitParameter(headerName));
    }

    // pass the request/response on to the rest of the filters
    filterChain.doFilter(req, response);
  }

  /**
   * toString
   * @return string containing the version information
   */
  public String toString() {
    return VERSION_STRING;
  }  

  /**
   * destroy
   */
  public void destroy() {
    if (isDebugEnabled) logger.info("Destroy Cache Filter");
    objFilterConfig = null;
  }

}
