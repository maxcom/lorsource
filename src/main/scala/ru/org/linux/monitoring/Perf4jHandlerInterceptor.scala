package ru.org.linux.monitoring

import org.springframework.web.servlet.handler.HandlerInterceptorAdapter
import org.springframework.web.servlet.resource.{DefaultServletHttpRequestHandler, ResourceHttpRequestHandler}
import org.springframework.web.method.HandlerMethod
import org.perf4j.slf4j.Slf4JStopWatch
import org.perf4j.StopWatch
import javax.servlet.http.{HttpServletResponse, HttpServletRequest}
import org.springframework.web.servlet.ModelAndView
import Perf4jHandlerInterceptor._

object Perf4jHandlerInterceptor {
  private final val ATTRIBUTE = "perf4jStopWatch"
  private final val TIME_THRESHOLD = 500
}

class Perf4jHandlerInterceptor extends HandlerInterceptorAdapter {
  override def preHandle(request: HttpServletRequest, response: HttpServletResponse, handler: AnyRef): Boolean = {
    if (handler.isInstanceOf[ResourceHttpRequestHandler] || handler.isInstanceOf[DefaultServletHttpRequestHandler]) {
      return true
    }

    val name = handler match {
      case method: HandlerMethod => method.getBeanType.getSimpleName
      case _ => handler.getClass.getSimpleName
    }

    val watch = new Slf4JStopWatch(name)
    watch.setTimeThreshold(TIME_THRESHOLD)
    request.setAttribute(ATTRIBUTE, watch)

    true
  }

  override def postHandle(request: HttpServletRequest, response: HttpServletResponse, handler: AnyRef, modelAndView: ModelAndView) {
    val stopWatch = request.getAttribute(ATTRIBUTE).asInstanceOf[StopWatch]

    if (stopWatch != null) {
      stopWatch.stop
    }
  }
}