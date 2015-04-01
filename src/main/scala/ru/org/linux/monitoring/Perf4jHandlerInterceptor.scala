/*
 * Copyright 1998-2015 Linux.org.ru
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

import java.util.concurrent.{ThreadLocalRandom, TimeUnit}
import javax.annotation.PostConstruct
import javax.servlet.http.{HttpServletRequest, HttpServletResponse}

import com.google.common.base.Stopwatch
import com.sksamuel.elastic4s.ElasticClient
import com.sksamuel.elastic4s.ElasticDsl._
import com.sksamuel.elastic4s.mappings.FieldType._
import com.typesafe.scalalogging.StrictLogging
import org.elasticsearch.client.Client
import org.joda.time.DateTime
import org.joda.time.format.DateTimeFormat
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.web.method.HandlerMethod
import org.springframework.web.servlet.ModelAndView
import org.springframework.web.servlet.handler.HandlerInterceptorAdapter
import org.springframework.web.servlet.resource.{DefaultServletHttpRequestHandler, ResourceHttpRequestHandler}
import ru.org.linux.monitoring.Perf4jHandlerInterceptor._

import scala.concurrent.Await
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._
import scala.util.control.NonFatal

object Perf4jHandlerInterceptor {
  private val Attribute = "perf4jStopWatch"
  private val LoggingThreshold = 500 millis
  private val ElasticProbability = 0.1
  private val IndexPrefix = "perf"
  private val PerfPattern = s"$IndexPrefix-*"
  private val PerfType = "metric"

  private val indexDateFormat = DateTimeFormat.forPattern("YYYY-MM")

  private class Metrics(val name:String, val path:String, val start:DateTime, controller:Stopwatch, view:Stopwatch) {
    def controllerDone():Unit = {
      controller.stop()
      view.start()
    }

    def complete():Unit = view.stop()

    def controllerTime = controller.elapsed(TimeUnit.MILLISECONDS)

    def controllerTimeHuman = controller.toString

    def viewTime = view.elapsed(TimeUnit.MILLISECONDS)

    def viewTimeHuman = view.toString
  }

  private object Metrics {
    def start(name:String, path:String) =
      new Metrics(name, path, DateTime.now, Stopwatch.createStarted(), Stopwatch.createUnstarted())
  }
}

class Perf4jHandlerInterceptor @Autowired() (javaElastic:Client) extends HandlerInterceptorAdapter with StrictLogging {
  private val elastic = ElasticClient.fromClient(javaElastic)

  private def indexOf(date:DateTime) = IndexPrefix + "-" + indexDateFormat.print(date)

  @PostConstruct
  def createIndex():Unit = {
    try {
      logger.info("Create performance index template")

      Await.result(elastic.execute {
        create template s"$IndexPrefix-template" pattern PerfPattern mappings (
          PerfType as(
            "controller" typed StringType index NotAnalyzed,
            "startdate" typed DateType format "dateTime",
            "elapsed" typed LongType,
            "view" typed LongType
          ) all false
        )
      }, 30 seconds)
    } catch {
      case NonFatal(ex) ⇒
        logger.warn("Unable to create performance index", ex)
    }
  }

  override def preHandle(request: HttpServletRequest, response: HttpServletResponse, handler: AnyRef): Boolean = {
    if (handler.isInstanceOf[ResourceHttpRequestHandler] || handler.isInstanceOf[DefaultServletHttpRequestHandler]) {
      return true
    }

    val name = handler match {
      case method: HandlerMethod => method.getBeanType.getSimpleName
      case _ => handler.getClass.getSimpleName
    }

    val watch = Metrics.start(name, request.getRequestURI)
    request.setAttribute(Attribute, watch)

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

          val future = elastic execute {
            index into indexOf(date) -> PerfType fields (
              "controller" -> stopWatch.name,
              "startdate"  -> date,
              "elapsed"    -> stopWatch.controllerTime,
              "view"       -> stopWatch.viewTime
            )
          }

          future.onFailure {
            case error ⇒ logger.info("Unable to log performance metric", error)
          }
        } catch {
          case NonFatal(failure) ⇒
            logger.info("Unable to log performance metric", failure)
        }
      }
    }

  }
}
