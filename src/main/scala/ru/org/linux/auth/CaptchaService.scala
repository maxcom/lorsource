/*
 * Copyright 1998-2018 Linux.org.ru
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

import javax.servlet.ServletRequest

import com.typesafe.scalalogging.StrictLogging
import org.springframework.stereotype.Service
import org.springframework.validation.Errors
import play.api.libs.json.{JsValue, Reads}
import play.api.libs.ws.StandaloneWSClient
import ru.org.linux.spring.SiteConfig
import play.api.libs.ws.JsonBodyReadables._
import play.api.libs.ws.DefaultBodyWritables._
import play.api.libs.json._
import play.api.libs.json.Reads._
import play.api.libs.functional.syntax._

import scala.concurrent.Await
import scala.concurrent.duration._
import scala.util.control.NonFatal
import scala.concurrent.ExecutionContext.Implicits.global

@Service
class CaptchaService(wsClient: StandaloneWSClient, siteConfig: SiteConfig) extends StrictLogging {
  def checkCaptcha(request: ServletRequest, errors: Errors): Unit = {
    val captchaResponse = request.getParameter("g-recaptcha-response")

    if (captchaResponse == null) {
      errors.reject(null, "Код проверки защиты от роботов не указан")
    } else {
      try {
        val params = Map(
          "secret" -> siteConfig.getCaptchaPrivateKey,
          "response" -> captchaResponse,
          "remoteip" -> request.getRemoteAddr)

        val apiResponse = Await.result(wsClient
          .url("https://www.google.com/recaptcha/api/siteverify")
          .post(params)
          .map { response ⇒
            val jsonData = response.body[JsValue]

            jsonData.as[CaptchaResponse]
          }, 1 minute)

        if (!apiResponse.success) {
          val errorTexts = apiResponse.errorCodes.toSeq.flatten

          errors.reject(null, s"Код проверки защиты от роботов не совпадает (${errorTexts.mkString(",")})")
        }
      } catch {
        case NonFatal(e) ⇒
          logger.warn("Unable to check captcha", e)
          errors.reject(null, "Unable to check captcha: " + e.getMessage)
      }
    }
  }
}

case class CaptchaResponse(success: Boolean, errorCodes: Option[Seq[String]])
object CaptchaResponse {
  implicit val reads: Reads[CaptchaResponse] = (
    (__ \ "success").read[Boolean] and
      (__ \ "error-codes").readNullable[Seq[String]]
  )(CaptchaResponse.apply _)
}