/*
 * Copyright 1998-2024 Linux.org.ru
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

import org.apache.pekko.actor.Status.Failure
import org.apache.pekko.actor.{Actor, ActorLogging, ActorRef, ActorSystem, Props, Timers}
import org.apache.pekko.pattern.PipeToSupport
import com.sksamuel.elastic4s.ElasticClient
import com.sksamuel.elastic4s.ElasticDsl.*
import com.sksamuel.elastic4s.handlers.index.CreateIndexTemplateResponse
import com.sksamuel.elastic4s.requests.bulk.BulkResponse
import org.joda.time.DateTime
import org.joda.time.format.DateTimeFormat
import org.springframework.context.annotation.{Bean, Configuration}

import scala.concurrent.duration.*

class PerformanceLoggingActor(elastic: ElasticClient) extends Actor with ActorLogging with PipeToSupport with Timers {
  import PerformanceLoggingActor.*
  import context.dispatcher

  private var queue: Seq[Metric] = Vector.empty[Metric]

  override def receive: Actor.Receive = initializing

  timers.startTimerWithFixedDelay(Initialize, Initialize, 2.minutes)

  private val initializing: Receive = {
    case m: Metric =>
      enqueue(m)

    case Initialize =>
      createIndex().map(_.result).pipeTo(self)

    case _: CreateIndexTemplateResponse =>
      log.info("Initialized performance logging")
      timers.cancel(Initialize)
      context.become(ready)

    case Failure(ex) =>
      log.error(ex, "Failed to put template")
  }

  private def indexOf(date: DateTime) = IndexPrefix + "-" + indexDateFormat.print(date)

  private val ready: Receive = {
    case m: Metric =>
      enqueue(m)

      elastic execute {
        bulk {
          queue map { m =>
            indexInto(indexOf(m.start)).fields(
              "controller" -> m.name,
              "startdate"  -> m.start,
              "elapsed"    -> m.controllerTime,
              "view"       -> m.viewTime
            )
          }
        }
      } map (_.result) pipeTo self

      queue = Vector.empty[Metric]

      context.become(waiting)
  }

  private val waiting: Receive = {
    case m: Metric =>
      enqueue(m)
    case r: BulkResponse =>
      if (r.hasFailures) {
        log.warning(s"Failed to write perf metrics: ${r.failures.flatMap(_.error).map(_.reason).mkString(", ")}")
      }
      context.become(ready)
    case Failure(ex) =>
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
      createIndexTemplate(s"$IndexPrefix-template", PerfPattern).mappings (
        properties(
          keywordField("controller"),
          dateField("startdate") format "dateTime",
          longField("elapsed"),
          longField("view")
          )
        )
    }
  }
}

object PerformanceLoggingActor {
  val MaxQueue = 5000
  private val IndexPrefix = "perf"
  private val PerfPattern = s"$IndexPrefix-*"
  private val indexDateFormat = DateTimeFormat.forPattern("YYYY-MM")

  private case object Initialize

  def props(elastic: ElasticClient): Props = Props(new PerformanceLoggingActor(elastic))
}

case class Metric(name: String, start: DateTime, controllerTime: Long, viewTime: Long)

@Configuration
class PerformanceLoggingConfiguration {
  @Bean(name=Array("loggingActor"))
  def loggingActor(actorSystem: ActorSystem, elastic: ElasticClient): ActorRef = {
    actorSystem.actorOf(PerformanceLoggingActor.props(elastic), "PerformanceLogger")
  }
}
