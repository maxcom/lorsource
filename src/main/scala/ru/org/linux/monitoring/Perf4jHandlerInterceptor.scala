package ru.org.linux.monitoring

import org.springframework.web.servlet.handler.HandlerInterceptorAdapter
import org.springframework.web.servlet.resource.{DefaultServletHttpRequestHandler, ResourceHttpRequestHandler}
import org.springframework.web.method.HandlerMethod
import org.perf4j.slf4j.Slf4JStopWatch
import org.perf4j.StopWatch
import javax.servlet.http.{HttpServletResponse, HttpServletRequest}
import org.springframework.web.servlet.ModelAndView
import Perf4jHandlerInterceptor._
import org.springframework.beans.factory.annotation.Autowired
import org.elasticsearch.client.Client
import org.apache.commons.io.IOUtils
import javax.annotation.PostConstruct
import com.typesafe.scalalogging.slf4j.Logging
import org.elasticsearch.ElasticsearchException
import org.elasticsearch.action.ActionListener
import org.elasticsearch.action.index.IndexResponse
import scala.collection.JavaConverters._

object Perf4jHandlerInterceptor {
  private val ATTRIBUTE = "perf4jStopWatch"
  private val LOGGING_THRESHOLD = 500
  private val ELASTIC_THRESHOLD = 500
  private val PERF_INDEX = "perf"
  private val PERF_TYPE = "metric"
}

class Perf4jHandlerInterceptor @Autowired() (elastic:Client) extends HandlerInterceptorAdapter with Logging {
  @PostConstruct
  def createIndex {
    try {
      if (!elastic.admin.indices.prepareExists(PERF_INDEX).execute.actionGet.isExists) {
        val mappingSource: String = IOUtils.toString(getClass.getClassLoader.getResource("perf-mapping.json"))
        logger.info("Create performance index")
        elastic.admin.indices.prepareCreate(PERF_INDEX).setSource(mappingSource).execute.actionGet
      }
    } catch {
      case ex:ElasticsearchException => logger.warn("Unable to create performance index", ex)
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
    watch.setTimeThreshold(LOGGING_THRESHOLD)
    request.setAttribute(ATTRIBUTE, watch)

    true
  }

  override def postHandle(request: HttpServletRequest, response: HttpServletResponse, handler: AnyRef, modelAndView: ModelAndView) {
    val stopWatch = request.getAttribute(ATTRIBUTE).asInstanceOf[StopWatch]

    if (stopWatch != null) {
      stopWatch.stop

      if (stopWatch.getElapsedTime > ELASTIC_THRESHOLD) {
        val doc:Map[String, AnyRef] = Map(
          "controller" -> stopWatch.getTag,
          "startdate" -> Long.box(stopWatch.getStartTime),
          "elapsed" -> Long.box(stopWatch.getElapsedTime)
        )

        try {
          val result = elastic.prepareIndex(PERF_INDEX, PERF_TYPE).setSource(doc.asJava).execute()

          result.addListener(new ActionListener[IndexResponse] {
            override def onResponse(response: IndexResponse): Unit = {}
            override def onFailure(e: Throwable): Unit = {
              logger.info("Unable to log performance metric", e)
            }
          })
        } catch {
          case ex:ElasticsearchException => logger.info("Unable to log performance metric", ex)
        }
      }
    }
  }
}