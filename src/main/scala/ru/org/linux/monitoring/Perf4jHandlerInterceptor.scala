package ru.org.linux.monitoring

import javax.annotation.PostConstruct
import javax.servlet.http.{HttpServletRequest, HttpServletResponse}

import com.sksamuel.elastic4s.ElasticClient
import com.sksamuel.elastic4s.ElasticDsl._
import com.sksamuel.elastic4s.mappings.FieldType._
import com.typesafe.scalalogging.slf4j.StrictLogging
import org.elasticsearch.ElasticsearchException
import org.elasticsearch.client.Client
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
  private val ElasticThreshold = 200 millis
  private val PerfIndex = "perf"
  private val PerfType = "metric"
}

class Perf4jHandlerInterceptor @Autowired() (javaElastic:Client) extends HandlerInterceptorAdapter with StrictLogging {
  private val elastic = ElasticClient.fromClient(javaElastic)

  @PostConstruct
  def createIndex():Unit = {
    try {
      if (!Await.result(elastic.exists(PerfIndex), 30 seconds).isExists) {
        logger.info("Create performance index")

        Await.result(elastic.execute {
          create index PerfIndex mappings(
            PerfType as (
              "controller" typed StringType index NotAnalyzed,
              "startdate" typed DateType format "dateTime",
              "elapsed" typed LongType
            ) all false
          )
        }, 30 seconds)
      }
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

      if (stopWatch.getElapsedTime > ElasticThreshold.toMillis) {
        try {
          val future = elastic execute {
            index into PerfIndex -> PerfType fields (
              "controller" -> stopWatch.getTag,
              "startdate"  -> stopWatch.getStartTime,
              "elapsed"    -> stopWatch.getElapsedTime
            )
          }

          future.onFailure {
            case error ⇒ logger.info("Unable to log performance metric", error)
          }
        } catch {
          case ex:ElasticsearchException => logger.info("Unable to log performance metric", ex)
        }
      }
    }
  }
}
