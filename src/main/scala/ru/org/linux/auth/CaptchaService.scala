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
package ru.org.linux.auth

import com.typesafe.scalalogging.StrictLogging
import io.circe.*
import org.springframework.stereotype.Service
import org.springframework.validation.Errors
import ru.org.linux.spring.SiteConfig
import sttp.client3.circe.*
import sttp.client3.*

import javax.servlet.ServletRequest
import scala.util.control.NonFatal

@Service
class CaptchaService(httpClient: SttpBackend[Identity, Any], siteConfig: SiteConfig) extends StrictLogging {
  def checkCaptcha(request: ServletRequest, errors: Errors): Unit = {
    val captchaResponse = request.getParameter("h-captcha-response")

    if (captchaResponse == null) {
      errors.reject(null, "Код проверки защиты от роботов не указан")
    } else {
      try {
        val params = Map(
          "secret" -> siteConfig.getCaptchaPrivateKey,
          "response" -> captchaResponse,
          "remoteip" -> request.getRemoteAddr,
          "sitekey" -> siteConfig.getCaptchaPublicKey)

        val response = basicRequest
          .body(params)
          .post(uri"https://hcaptcha.com/siteverify")
          .response(asJson[CaptchaResponse])
          .send(httpClient)

        val apiResponse = response.body.toTry.get

        if (!apiResponse.success) {
          val errorTexts = apiResponse.errorCodes.toSeq.flatten

          errors.reject(null, s"Код проверки защиты от роботов не совпадает (${errorTexts.mkString(",")})")
        }
      } catch {
        case NonFatal(e) =>
          logger.warn("Unable to check captcha", e)
          errors.reject(null, "Unable to check captcha: " + e.toString)
      }
    }
  }
}

case class CaptchaResponse(success: Boolean, errorCodes: Option[Seq[String]])

object CaptchaResponse {
  implicit val decoder: Decoder[CaptchaResponse] =
    Decoder.forProduct2("success", "error-codes")(CaptchaResponse.apply)
}
