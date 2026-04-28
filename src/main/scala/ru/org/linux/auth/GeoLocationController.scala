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

import io.circe.Json
import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.{RequestMapping, RequestMethod, RequestParam, ResponseBody}
import ru.org.linux.auth.AuthUtil.ModeratorOnly

@Controller
class GeoLocationController(geoLocationService: GeoLocationService) {
  @RequestMapping(value = Array("/admin/geoip"), method = Array(RequestMethod.GET))
  @ResponseBody
  def geoip(@RequestParam("ip") ip: String): Json = ModeratorOnly { _ =>
    geoLocationService.getLocation(ip) match {
      case Right(location) =>
        Json.obj(
          "country" -> Json.fromString(location.country.getOrElse("")),
          "region" -> Json.fromString(location.region.getOrElse("")),
          "city" -> Json.fromString(location.city.getOrElse("")),
          "org" -> Json.fromString(location.connection.flatMap(_.org).getOrElse(""))
        )
      case Left(message) =>
        Json.obj("error" -> Json.fromString(message))
    }
  }
}
