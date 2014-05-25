package ru.org.linux.clickjacking

import org.springframework.web.servlet.handler.HandlerInterceptorAdapter
import javax.servlet.http.{HttpServletResponse, HttpServletRequest}
import ru.org.linux.spring.SiteConfig
import org.springframework.beans.factory.annotation.Autowired

class CJInterceptor @Autowired() (config:SiteConfig) extends HandlerInterceptorAdapter {
  override def preHandle(request: HttpServletRequest, response: HttpServletResponse, handler: Any) = {
    //if (!config.blahblahblahblah()) {
    //  return true
    //}
    
    response.addHeader("X-Frame-Options", "DENY")
    // или же быть может (вместо "DENY") лучше использовать "SAMEORIGIN"?

    true
  }
}
