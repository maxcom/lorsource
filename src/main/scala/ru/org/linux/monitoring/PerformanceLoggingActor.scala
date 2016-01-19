package ru.org.linux.monitoring

import akka.actor.Status.Failure
import akka.actor.{Actor, ActorLogging, ActorSystem, Props}
import akka.pattern.PipeToSupport
import com.sksamuel.elastic4s.ElasticClient
import com.sksamuel.elastic4s.ElasticDsl._
import com.sksamuel.elastic4s.mappings.FieldType.{DateType, LongType, StringType}
import org.elasticsearch.action.admin.indices.template.put.PutIndexTemplateResponse
import org.elasticsearch.action.bulk.BulkResponse
import org.joda.time.DateTime
import org.joda.time.format.DateTimeFormat
import org.springframework.context.annotation.{Bean, Configuration}

import scala.concurrent.duration._

class PerformanceLoggingActor(elastic:ElasticClient) extends Actor with ActorLogging with PipeToSupport {
  import PerformanceLoggingActor._
  import context.dispatcher

  private var queue = Vector.empty[Metric]

  override def receive: Actor.Receive = initializing

  private val createSchedule = context.system.scheduler.schedule(10 seconds, 2 minutes, self, Initialize)

  private val initializing:Receive = {
    case m:Metric ⇒
      enqueue(m)

    case Initialize ⇒
      createIndex() pipeTo self

    case p:PutIndexTemplateResponse ⇒
      log.info("Initialized performance logging")
      createSchedule.cancel()
      context.become(ready)

    case Failure(ex) ⇒
      log.error(ex, "Failed to put template")
  }

  private def indexOf(date:DateTime) = IndexPrefix + "-" + indexDateFormat.print(date)

  private val ready:Receive = {
    case m:Metric ⇒
      enqueue(m)

      elastic execute {
        bulk {
          queue map { m ⇒
            index into indexOf(m.start) -> PerfType fields (
              "controller" -> m.name,
              "startdate"  -> m.start,
              "elapsed"    -> m.controllerTime,
              "view"       -> m.viewTime
            )
          }
        }
      } pipeTo self onFailure { case ex ⇒ log.error(ex, "Error callback :-(") }

      queue = Vector.empty[Metric]

      context.become(waiting)
  }

  private val waiting:Receive = {
    case m:Metric ⇒
      enqueue(m)
    case r:BulkResponse ⇒
      if (r.hasFailures) {
        log.warning(s"Failed to write perf metrics: ${r.buildFailureMessage()}")
      }
      log.debug(s"Logged ${r.getItems.length} metrics")
      context.become(ready)
    case Failure(ex) ⇒
      log.error(ex, "Failed to write perf metrics")
      context.become(ready)
  }

  private def enqueue(m:Metric):Unit = {
    queue = queue :+ m

    if (queue.size > MaxQueue) {
      queue = queue.drop(1)
      log.warning("Metrics queue too large, dropping metric")
    }
  }

  private def createIndex() = {
    log.info("Create performance index template")

    elastic.execute {
      create template s"$IndexPrefix-template" pattern PerfPattern mappings (
        mapping(PerfType) fields(
          field("controller", StringType) index NotAnalyzed,
          field("startdate", DateType) format "dateTime",
          field("elapsed", LongType),
          field("view", LongType)
          ) all false
        )
    }
  }

}

object PerformanceLoggingActor {
  val MaxQueue = 5000
  private val IndexPrefix = "perf"
  private val PerfType = "metric"
  private val PerfPattern = s"$IndexPrefix-*"
  private val indexDateFormat = DateTimeFormat.forPattern("YYYY-MM")

  private case object Initialize

  def props(elastic:ElasticClient) = Props(classOf[PerformanceLoggingActor], elastic)
}

case class Metric(name:String, start:DateTime, controllerTime: Long, viewTime: Long)

@Configuration
class PerformanceLoggingConfiguration {
  @Bean(name=Array("loggingActor"))
  def loggingActor(actorSystem:ActorSystem, elastic: ElasticClient) = {
    actorSystem.actorOf(PerformanceLoggingActor.props(elastic), "PerformanceLogger")
  }
}