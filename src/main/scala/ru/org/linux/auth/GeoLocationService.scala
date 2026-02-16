/*
 * Copyright 1998-2026 Linux.org.ru
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
import io.circe.Decoder
import io.circe.generic.semiauto._
import org.springframework.stereotype.Service
import sttp.client3._
import sttp.client3.Identity

case class GeoLocation(
  country: Option[String],
  region: Option[String],
  city: Option[String],
  connection: Option[Connection])

case class Connection(
  org: Option[String])

case class IpWhoIsResponse(
  success: Boolean,
  message: Option[String],
  country: Option[String],
  region: Option[String],
  city: Option[String],
  connection: Option[Connection])

object Connection {
  implicit val decoder: Decoder[Connection] = deriveDecoder[Connection]
}

object IpWhoIsResponse {
  implicit val decoder: Decoder[IpWhoIsResponse] = deriveDecoder[IpWhoIsResponse]
}

object GeoLocation {
  implicit val decoder: Decoder[GeoLocation] = deriveDecoder[GeoLocation]
}

@Service
class GeoLocationService(httpClient: SttpBackend[Identity, Any]) extends StrictLogging {
  def getLocation(ip: String): Either[String, GeoLocation] = {
    val response = basicRequest
      .get(uri"http://ipwho.is/$ip")
      .send(httpClient)

    response.body match {
      case Right(body) =>
        io.circe.parser.decode[IpWhoIsResponse](body) match {
          case Right(apiResponse) =>
            if (!apiResponse.success) {
              val message = apiResponse.message.getOrElse("Unknown error")
              logger.warn(s"IP geolocation API error for $ip: $message")
              Left(message)
            } else {
              val location = GeoLocation(
                country = apiResponse.country,
                region = apiResponse.region,
                city = apiResponse.city,
                connection = apiResponse.connection)
              Right(location)
            }
          case Left(error) =>
            logger.warn(s"Failed to parse IP geolocation for $ip: $error")
            Left(s"Parse error: $error")
        }
      case Left(error) =>
        logger.warn(s"IP geolocation request failed for $ip: $error")
        Left(s"Request error: $error")
    }
  }
}
