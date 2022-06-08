/*
 * Copyright 1998-2016 Linux.org.ru
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
package ru.org.linux.auth

import com.typesafe.scalalogging.StrictLogging
import org.joda.time.DateTime
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import play.api.libs.ws.StandaloneWSClient

import java.sql.Timestamp
import scala.concurrent.ExecutionContext.Implicits.global
import scala.util.{Failure, Success}

@Component
class TorBlockUpdater(wsClient: StandaloneWSClient, dao: IPBlockDao) extends StrictLogging {
  @Scheduled(fixedDelay = 60 * 60 * 1000, initialDelay = 30 * 60 * 1000)
  def updateTor(): Unit = {
    wsClient
      .url("https://www.dan.me.uk/torlist/?exit")
      .get()
      .map { response =>
        if (response.status == 200) {
          logger.debug("Updating TOR exit node list")

          response.body.linesIterator.foreach { ip =>
            dao.blockIP(ip, 0, "TOR Exit Node", new Timestamp(DateTime.now().plusMonths(1).getMillis),
              true, true)
          }
        } else {
          logger.warn(s"Can't update TOR exit node list: ${response.statusText}")
        }
      }.onComplete {
        case Success(_) =>
        case Failure(ex) =>
          logger.warn("Failed to update TOR exit node list", ex)
      }
  }
}
