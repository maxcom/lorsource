package ru.org.linux.monitoring

import java.util.concurrent.ThreadLocalRandom
import javax.annotation.PostConstruct
import javax.servlet.http.{HttpServletRequest, HttpServletResponse}

import com.sksamuel.elastic4s.ElasticClient
import com.sksamuel.elastic4s.ElasticDsl._
import com.sksamuel.elastic4s.mappings.FieldType._
import com.typesafe.scalalogging.slf4j.StrictLogging
import org.elasticsearch.client.Client
import org.joda.time.DateTime
import org.joda.time.format.DateTimeFormat
import org.perf4j.StopWatch
import org.perf4j.slf4j.Slf4JStopWatch
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
  private val ElasticProbability = 0.01
  private val IndexPrefix = "perf"
  private val PerfPattern = s"$IndexPrefix-*"
  private val PerfType = "metric"

  private val indexDateFormat = DateTimeFormat.forPattern("YYYY-MM")
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
            "elapsed" typed LongType
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

    val watch = new Slf4JStopWatch(name)
    watch.setTimeThreshold(LoggingThreshold.toMillis)
    request.setAttribute(Attribute, watch)

    true
  }

  override def postHandle(request: HttpServletRequest, response: HttpServletResponse, handler: AnyRef, modelAndView: ModelAndView):Unit = {
    val stopWatch = request.getAttribute(Attribute).asInstanceOf[StopWatch]

    if (stopWatch != null) {
      stopWatch.stop

      if (ThreadLocalRandom.current().nextDouble() < ElasticProbability) {
        try {
          val date = new DateTime(stopWatch.getStartTime)

          val future = elastic execute {
            index into indexOf(date) -> PerfType fields (
              "controller" -> stopWatch.getTag,
              "startdate"  -> date,
              "elapsed"    -> stopWatch.getElapsedTime
            )
          }

          future.onFailure {
            case error ⇒ logger.info("Unable to log performance metric", error)
          }
        } catch {
          case NonFatal(ex) ⇒
            logger.info("Unable to log performance metric", ex)
        }
      }
    }
  }
}
