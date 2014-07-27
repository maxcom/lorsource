package ru.org.linux.csp

import org.springframework.web.servlet.handler.HandlerInterceptorAdapter
import javax.servlet.http.{HttpServletResponse, HttpServletRequest}
import ru.org.linux.spring.SiteConfig
import org.springframework.beans.factory.annotation.Autowired

class CSPInterceptor @Autowired() (config:SiteConfig) extends HandlerInterceptorAdapter {
  override def preHandle(request: HttpServletRequest, response: HttpServletResponse, handler: Any) = {
    //if (!config.blahblahblahblah()) {
    //  return true
    //}
    
    response.addHeader("Content-Security-Policy", "frame-options 'deny'")
    // вообще-то правила можно было бы придумать и более гибкие чем эти.
    // но начала -- пойдёт! лучше чем ни чего :-)

    true
  }
}
