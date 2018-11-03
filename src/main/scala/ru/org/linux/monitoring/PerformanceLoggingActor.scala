/*
 * Copyright 1998-2017 Linux.org.ru
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

import akka.actor.Status.Failure
import akka.actor.{Actor, ActorLogging, ActorRef, ActorSystem, Props}
import akka.pattern.PipeToSupport
import com.sksamuel.elastic4s.ElasticDsl._
import com.sksamuel.elastic4s.TcpClient
import com.sksamuel.elastic4s.bulk.RichBulkResponse
import org.elasticsearch.action.admin.indices.template.put.PutIndexTemplateResponse
import org.joda.time.DateTime
import org.joda.time.format.DateTimeFormat
import org.springframework.context.annotation.{Bean, Configuration}

import scala.concurrent.duration._

class PerformanceLoggingActor(elastic: TcpClient) extends Actor with ActorLogging with PipeToSupport {
  import PerformanceLoggingActor._
  import context.dispatcher

  private var queue = Vector.empty[Metric]

  override def receive: Actor.Receive = initializing

  private val createSchedule = context.system.scheduler.schedule(10 seconds, 2 minutes, self, Initialize)

  private val initializing: Receive = {
    case m:Metric ⇒
      enqueue(m)

    case Initialize ⇒
      createIndex() pipeTo self

    case _: PutIndexTemplateResponse ⇒
      log.info("Initialized performance logging")
      createSchedule.cancel()
      context.become(ready)

    case Failure(ex) ⇒
      log.error(ex, "Failed to put template")
  }

  private def indexOf(date: DateTime) = IndexPrefix + "-" + indexDateFormat.print(date)

  private val ready: Receive = {
    case m: Metric ⇒
      enqueue(m)

      elastic execute {
        bulk {
          queue map { m ⇒
            indexInto(indexOf(m.start), PerfType) fields (
              "controller" -> m.name,
              "startdate"  -> m.start,
              "elapsed"    -> m.controllerTime,
              "view"       -> m.viewTime
            )
          }
        }
      } pipeTo self

      queue = Vector.empty[Metric]

      context.become(waiting)
  }

  private val waiting: Receive = {
    case m: Metric ⇒
      enqueue(m)
    case r: RichBulkResponse ⇒
      if (r.hasFailures) {
        log.warning(s"Failed to write perf metrics: ${r.failureMessage}")
      }
      log.debug(s"Logged ${r.items.length} metrics")
      context.become(ready)
    case Failure(ex) ⇒
      log.error(ex, "Failed to write perf metrics")
      context.become(ready)
  }

  private def enqueue(m: Metric): Unit = {
    queue = queue :+ m

    if (queue.size > MaxQueue) {
      queue = queue.drop(1)
      log.warning("Metrics queue too large, dropping metric")
    }
  }

  private def createIndex() = {
    log.info("Create performance index template")

    elastic.execute {
      createTemplate(s"$IndexPrefix-template") pattern PerfPattern mappings (
        mapping(PerfType) fields(
          keywordField("controller"),
          dateField("startdate") format "dateTime",
          longField("elapsed"),
          longField("view")
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

  def props(elastic: TcpClient) = Props(classOf[PerformanceLoggingActor], elastic)
}

case class Metric(name: String, start: DateTime, controllerTime: Long, viewTime: Long)

@Configuration
class PerformanceLoggingConfiguration {
  @Bean(name=Array("loggingActor"))
  def loggingActor(actorSystem: ActorSystem, elastic: TcpClient): ActorRef = {
    actorSystem.actorOf(PerformanceLoggingActor.props(elastic), "PerformanceLogger")
  }
}
