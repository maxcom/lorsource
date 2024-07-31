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
package ru.org.linux.spring

import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.{ModelAttribute, RequestMapping, RequestParam}
import org.springframework.web.servlet.ModelAndView
import ru.org.linux.auth.AuthUtil.ModeratorOnly
import ru.org.linux.auth.IPBlockDao
import ru.org.linux.sameip.SameIpService
import ru.org.linux.site.BadInputException
import ru.org.linux.spring.dao.UserAgentDao
import ru.org.linux.user.UserService

import java.util
import java.util.regex.Pattern
import scala.jdk.CollectionConverters.SeqHasAsJava

object SameIPController {
  private val ipRE = Pattern.compile("^\\d+\\.\\d+\\.\\d+\\.\\d+$")
}

@Controller
class SameIPController(ipBlockDao: IPBlockDao, userService: UserService, userAgentDao: UserAgentDao, sameIpService: SameIpService) {
  @ModelAttribute("masks")
  def masks: util.List[(String, String)] =
    Seq("32" -> "Только IP", "24" -> "Сеть /24", "16" -> "Сеть /16", "0" -> "Любой IP").asJava

  @ModelAttribute("scores")
  def scores: util.List[(String, String)] =
    Seq("" -> "Любой score", "46" -> "score <= 45", "50" -> "score < 50", "100" -> "score < 100").asJava

  @RequestMapping(Array("/sameip.jsp"))
  def sameIP(@RequestParam(required = false) ip: String, @RequestParam(required = false, defaultValue = "32") mask: Int,
             @RequestParam(required = false, name = "ua") userAgent: Integer,
             @RequestParam(required = false) score: Integer): ModelAndView = ModeratorOnly { _ =>
    val mv = new ModelAndView("sameip")

    val ipMask = Option(ip).flatMap { ip =>
      val matcher = SameIPController.ipRE.matcher(ip)
      if (!matcher.matches) throw new BadInputException("not ip")

      if (mask < 0 || mask > 32) {
        throw new BadInputException("bad mask")
      }

      if (mask == 0) {
        None
      } else if (mask != 32) {
        Some(ip + "/" + mask)
      } else {
        Some(ip)
      }
    }.orNull

    val rowsLimit = 50

    val userAgentOpt = Option[Integer](userAgent).map(_.toInt)
    val scoreOpt = Option[Integer](score).map(_.toInt)

    val posts = sameIpService.getPosts(ip = Option(ipMask), userAgent = userAgentOpt, score = scoreOpt, limit = rowsLimit)

    mv.getModel.put("comments", posts.asJava)
    mv.getModel.put("hasMoreComments", posts.size == rowsLimit)
    mv.getModel.put("rowsLimit", rowsLimit)

    val users = userService.getUsersWithAgent(ip = Option(ipMask), userAgent = userAgentOpt, limit = rowsLimit)

    mv.getModel.put("users", users)
    mv.getModel.put("hasMoreUsers", users.size == rowsLimit)
    mv.getModel.put("score", score)

    if (ip != null) {
      mv.getModel.put("ip", ip)
      mv.getModel.put("mask", mask)

      val hasMask = mask < 32

      mv.getModel.put("hasMask", hasMask)

      if (!hasMask) {
        val blockInfo = ipBlockDao.getBlockInfo(ip)
        var allowPosting = false
        var captchaRequired = true

        if (blockInfo.isInitialized) {
          mv.getModel.put("blockInfo", blockInfo)
          allowPosting = blockInfo.isAllowRegistredPosting
          captchaRequired = blockInfo.isCaptchaRequired

          if (blockInfo.getModerator != 0) {
            mv.getModel.put("blockModerator", userService.getUserCached(blockInfo.getModerator))
          }
        }

        mv.addObject("allowPosting", allowPosting)
        mv.addObject("captchaRequired", captchaRequired)
      }
    }

    mv.getModel.put("newUsers", userService.getNewUsersByUAIp(ipMask, userAgent))

    if (userAgent != null) {
      mv.getModel.put("userAgent", userAgentDao.getUserAgentById(userAgent).orElse(null))
      mv.getModel.put("ua", userAgent)
    }

    mv
  }
}