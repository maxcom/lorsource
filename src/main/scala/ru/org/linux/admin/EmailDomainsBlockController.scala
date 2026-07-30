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

package ru.org.linux.admin

import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.{RequestMapping, RequestMethod, RequestParam}
import org.springframework.web.servlet.ModelAndView
import org.springframework.web.servlet.view.RedirectView
import ru.org.linux.auth.AuthUtil.ModeratorOnly
import ru.org.linux.site.BadInputException
import ru.org.linux.user.{EmailDomainsBlockDao, User, UserService}

import java.sql.Timestamp
import java.time.OffsetDateTime
import java.util.regex.Pattern
import javax.annotation.Nullable
import scala.beans.BeanProperty
import scala.jdk.CollectionConverters.SeqHasAsJava

object EmailDomainsBlockController:
  private val domainRE = Pattern.compile("^[a-z0-9]([a-z0-9.-]*[a-z0-9])?$")

case class PreparedEmailDomainBlock(
    @BeanProperty
    domain: String,
    @BeanProperty
    blockUntil: Timestamp,
    @Nullable @BeanProperty
    moderator: User,
    @BeanProperty
    blockedAt: Timestamp)

@Controller
class EmailDomainsBlockController(dao: EmailDomainsBlockDao, userService: UserService):
  @RequestMapping(Array("/admin/email-domains"))
  def list(
      @RequestParam(value = "offset", defaultValue = "0")
      offset: Int): ModelAndView =
    ModeratorOnly { session =>
      val limit = session.profile.messages
      val count = dao.manualCount

      if offset < 0 || (count > 0 && offset >= count) then
        throw new BadInputException("Wrong offset")

      val blocks =
        if count > 0 then
          dao.getManualDomains(offset, limit)
        else
          Seq.empty

      val prepared = blocks.map { b =>
        PreparedEmailDomainBlock(
          domain = b.domain,
          blockUntil = Timestamp.from(b.blockUntil.toInstant),
          moderator = b.moderatorId.map(id => userService.getUserCached(id)).orNull,
          blockedAt = Timestamp.from(b.blockedAt.toInstant)
        )
      }

      val mv = new ModelAndView("email-domains")
      mv.getModel.put("blocks", prepared.asJava)
      mv.getModel.put("offset", offset)
      mv.getModel.put("limit", limit)
      mv.getModel.put("hasMore", count > (offset + limit))
      mv.getModel.put("count", count)
      mv
    }

  @RequestMapping(value = Array("/admin/email-domains/add"), method = Array(RequestMethod.POST))
  def add(
      @RequestParam("domain")
      domain: String): ModelAndView =
    ModeratorOnly { session =>
      val normalized = normalize(domain)

      if normalized.length > 255 || !EmailDomainsBlockController.domainRE.matcher(normalized).matches then
        throw new BadInputException("Invalid domain")

      dao.blockDomainManual(normalized, OffsetDateTime.now.plusYears(3), session.user.id)
      new ModelAndView(new RedirectView("/admin/email-domains", true))
    }

  @RequestMapping(value = Array("/admin/email-domains/delete"), method = Array(RequestMethod.POST))
  def delete(
      @RequestParam("domain")
      domain: String): ModelAndView =
    ModeratorOnly { _ =>
      dao.unblockDomain(normalize(domain))
      new ModelAndView(new RedirectView("/admin/email-domains", true))
    }

  private def normalize(domain: String): String =
    Option(domain)
      .map(_.trim.toLowerCase)
      .filter(_.nonEmpty)
      .getOrElse {
        throw new BadInputException("Empty domain")
      }
end EmailDomainsBlockController
