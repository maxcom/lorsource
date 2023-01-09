/*
 * Copyright 1998-2022 Linux.org.ru
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
import sttp.client3.*

import java.sql.Timestamp

@Component
class TorBlockUpdater(httpClient: SttpBackend[Identity, Any], dao: IPBlockDao) extends StrictLogging {
  @Scheduled(fixedDelay = 60 * 60 * 1000, initialDelay = 30 * 60 * 1000)
  def updateTor(): Unit = {
    val response = basicRequest
      .get(uri"https://www.dan.me.uk/torlist/?exit")
      .send(httpClient)

    response.body match {
      case Right(body) =>
        logger.debug("Updating TOR exit node list")

        body.linesIterator.foreach { ip =>
          dao.blockIP(ip, 0, "TOR Exit Node", new Timestamp(DateTime.now().plusMonths(1).getMillis),
            true, false)
        }
      case Left(error) =>
        logger.warn(s"Can't update TOR exit node list: $error")
    }
  }
}
