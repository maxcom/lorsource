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

import jakarta.servlet.http.HttpServletRequest
import org.springframework.stereotype.Controller
import org.springframework.web.bind.ServletRequestUtils
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestMethod
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.servlet.ModelAndView
import org.springframework.web.servlet.view.RedirectView
import ru.org.linux.auth.AuthUtil.ModeratorOnly
import ru.org.linux.user.UserErrorException

import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import java.time.OffsetDateTime

@Controller
class BanIPController(ipBlockDao: IPBlockDao) {
  @RequestMapping(value = Array("/banip.jsp"), method = Array(RequestMethod.POST))
  @throws[Exception]
  def banIP(request: HttpServletRequest, @RequestParam("ip") ip: String, @RequestParam("reason") reason: String,
            @RequestParam("time") time: String,
            @RequestParam(value = "allow_posting", required = false, defaultValue = "false") allowPosting: Boolean,
            @RequestParam(value = "captcha_required", required = false, defaultValue = "false") captchaRequired: Boolean): ModelAndView = ModeratorOnly { moderator =>

    val banTo = time match {
      case "hour" => Some(OffsetDateTime.now.plusHours(1))
      case "day" => Some(OffsetDateTime.now.plusDays(1))
      case "month" => Some(OffsetDateTime.now.plusMonths(1))
      case "3month" => Some(OffsetDateTime.now.plusMonths(3))
      case "6month" => Some(OffsetDateTime.now.plusMonths(6))
      case "custom" =>
        val days = ServletRequestUtils.getRequiredIntParameter(request, "ban_days")
        if (days <= 0 || days > 180) throw new UserErrorException("Invalid days count")

        Some(OffsetDateTime.now.plusDays(days))
      case "unlim" => None
      case "remove" => Some(OffsetDateTime.now)
      case _ => throw new UserErrorException("Invalid count")
    }

    ipBlockDao.blockIP(ip, moderator.user.getId, reason, banTo, allowPosting, captchaRequired)

    new ModelAndView(new RedirectView("sameip.jsp?ip=" + URLEncoder.encode(ip, StandardCharsets.UTF_8)))
  }
}