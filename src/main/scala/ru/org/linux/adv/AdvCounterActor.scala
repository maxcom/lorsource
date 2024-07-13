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
package ru.org.linux.adv

import akka.actor.typed.SupervisorStrategy
import akka.actor.typed.Behavior
import akka.actor.typed.scaladsl.Behaviors

import scala.collection.mutable
import scala.concurrent.duration.DurationInt

object AdvCounterActor {
  sealed trait Protocol

  case class Count(path: String) extends Protocol
  private case object Store extends Protocol

  def behavior(advCounterDao: AdvCounterDao): Behavior[Protocol] = Behaviors.supervise(
    Behaviors.setup[Protocol] { _ =>
      val current = mutable.HashMap[String, Long]()

      Behaviors.withTimers { timers =>
        timers.startSingleTimer(Store, 1.minute)

        Behaviors.receiveMessage[Protocol] {
          case Count(path) =>
            current.updateWith(path)(v => Some(v.getOrElse(0L) + 1))

            Behaviors.same
          case Store =>
            current.foreach { case (k, v) =>
              advCounterDao.count(k, v)
            }

            current.clear()

            timers.startSingleTimer(Store, 1.minute)

            Behaviors.same
        }
      }
    }).onFailure(SupervisorStrategy.restartWithBackoff(1.minute, 5.minutes, 0.2))
}
