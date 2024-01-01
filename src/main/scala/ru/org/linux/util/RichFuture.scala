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

package ru.org.linux.util

import akka.actor.ActorSystem
import akka.pattern.after

import scala.concurrent._
import scala.concurrent.duration.FiniteDuration

object RichFuture {
  implicit class RichFuture[T](val future: Future[T]) extends AnyVal {
    def withTimeout(duration: FiniteDuration)(implicit system: ActorSystem, executor: ExecutionContext): Future[T] = {
      if (future.isCompleted) {
        future
      } else {
        Future firstCompletedOf Seq(
          future,
          after(duration, system.scheduler)(Future.failed(new TimeoutException(s"Timed out after ${duration.toMillis}ms"))))
      }
    }
  }
}
