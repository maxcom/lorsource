package ru.org.linux.csp

import org.springframework.web.servlet.handler.HandlerInterceptorAdapter
import javax.servlet.http.{HttpServletResponse, HttpServletRequest}

class CSPInterceptor extends HandlerInterceptorAdapter {
  override def preHandle(request: HttpServletRequest, response: HttpServletResponse, handler: Any) = {
    response.addHeader("Content-Security-Policy", "frame-ancestors 'none'")
    // вообще-то правила можно было бы придумать и более гибкие чем эти.
    // но для начала -- пойдёт! лучше чем ни чего :-)

    true
  }
}
