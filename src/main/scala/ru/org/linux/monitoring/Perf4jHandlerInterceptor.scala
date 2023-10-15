/*
 * Copyright 1998-2023 Linux.org.ru
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

package ru.org.linux.monitoring

import akka.actor.ActorRef
import com.google.common.base.Stopwatch
import com.typesafe.scalalogging.StrictLogging
import org.joda.time.DateTime
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.web.method.HandlerMethod
import org.springframework.web.servlet.resource.{DefaultServletHttpRequestHandler, ResourceHttpRequestHandler}
import org.springframework.web.servlet.{HandlerInterceptor, ModelAndView}
import ru.org.linux.monitoring.Perf4jHandlerInterceptor.*

import java.util.concurrent.{ThreadLocalRandom, TimeUnit}
import javax.servlet.http.{HttpServletRequest, HttpServletResponse}
import scala.concurrent.duration.*
import scala.util.control.NonFatal

object Perf4jHandlerInterceptor {
  private val Attribute = "perf4jStopWatch"
  private val LoggingThreshold = 250.millis
  private val ElasticProbability = 0.1
  private val BootDuration = 2.minutes // do not log slow when starting up

  private class Metrics(val name: String, val path: String, val start: DateTime, controller: Stopwatch, view: Stopwatch) {
    def controllerDone():Unit = {
      if (controller.isRunning) {
        controller.stop()
        view.start()
      }
    }

    def complete():Unit = {
      if (view.isRunning) {
        view.stop()
      }
    }

    def controllerTime = controller.elapsed(TimeUnit.MILLISECONDS)

    def controllerTimeHuman = controller.toString

    def viewTime = view.elapsed(TimeUnit.MILLISECONDS)

    def viewTimeHuman = view.toString
  }

  private object Metrics {
    def start(name: String, path: String) =
      new Metrics(name, path, DateTime.now, Stopwatch.createStarted(), Stopwatch.createUnstarted())
  }
}

class Perf4jHandlerInterceptor(@Qualifier("loggingActor") loggingActor: ActorRef)
  extends HandlerInterceptor with StrictLogging {

  private lazy val LogAfter = BootDuration.fromNow

  override def preHandle(request: HttpServletRequest, response: HttpServletResponse, handler: AnyRef): Boolean = {
    if (LogAfter.hasTimeLeft()) {
      return true
    }

    if (handler.isInstanceOf[ResourceHttpRequestHandler] || handler.isInstanceOf[DefaultServletHttpRequestHandler]) {
      return true
    }

    if (request.getAttribute(Attribute).asInstanceOf[Metrics] == null) {
      val name = handler match {
        case method: HandlerMethod => method.getBeanType.getSimpleName
        case _ => handler.getClass.getSimpleName
      }

      val watch = Metrics.start(name, request.getRequestURI)
      request.setAttribute(Attribute, watch)
    }

    true
  }

  override def postHandle(request: HttpServletRequest, response: HttpServletResponse, handler: AnyRef,
                          modelAndView: ModelAndView):Unit = {
    val stopWatch = request.getAttribute(Attribute).asInstanceOf[Metrics]

    if (stopWatch != null) {
      stopWatch.controllerDone()
    }
  }

  override def afterCompletion(request: HttpServletRequest, response: HttpServletResponse,
                               handler: scala.Any, ex: Exception): Unit = {
    val stopWatch = request.getAttribute(Attribute).asInstanceOf[Metrics]

    if (stopWatch != null) {
      stopWatch.complete()

      if (stopWatch.controllerTime > LoggingThreshold.toMillis) {
        logger.warn(s"Slow controller ${stopWatch.name} ${stopWatch.path} took ${stopWatch.controllerTimeHuman}")
      }

      if (stopWatch.viewTime > LoggingThreshold.toMillis) {
        logger.warn(s"Slow view ${stopWatch.name} ${stopWatch.path} took ${stopWatch.viewTimeHuman}")
      }

      if (ThreadLocalRandom.current().nextDouble() < ElasticProbability) {
        try {
          val date = stopWatch.start

          loggingActor ! Metric(stopWatch.name, date, stopWatch.controllerTime, stopWatch.viewTime)
        } catch {
          case NonFatal(failure) =>
            logger.info("Unable to log performance metric", failure)
        }
      }
    }

  }
}
