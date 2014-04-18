package ru.org.linux.auth

import org.springframework.web.servlet.handler.HandlerInterceptorAdapter
import javax.servlet.http.{HttpServletResponse, HttpServletRequest}
import ru.org.linux.spring.SiteConfig
import org.springframework.beans.factory.annotation.Autowired

class HstsInterceptor @Autowired() (config:SiteConfig) extends HandlerInterceptorAdapter {
  override def preHandle(request: HttpServletRequest, response: HttpServletResponse, handler: Any) = {
    if (request.isSecure && config.enableHsts()) {
      val currentUser = AuthUtil.getCurrentUser

      if (currentUser!=null && currentUser.getMaxScore>=100) {
        response.addHeader("Strict-Transport-Security", "max-age=7776000")
      }
    }

    true
  }
}
