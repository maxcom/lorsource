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

package ru.org.linux.auth;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.ServletRequestUtils;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.view.RedirectView;
import ru.org.linux.site.Template;
import ru.org.linux.user.User;
import ru.org.linux.user.UserErrorException;

import javax.servlet.http.HttpServletRequest;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.sql.Timestamp;
import java.time.OffsetDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Optional;

@Controller
public class BanIPController {
  private final IPBlockDao ipBlockDao;

  public BanIPController(IPBlockDao ipBlockDao) {
    this.ipBlockDao = ipBlockDao;
  }

  @RequestMapping(value="/banip.jsp", method= RequestMethod.POST)
  public ModelAndView banIP(
    HttpServletRequest request,
    @RequestParam("ip") String ip,
    @RequestParam("reason") String reason,
    @RequestParam("time") String time,
    @RequestParam(value="allow_posting", required = false, defaultValue="false") boolean allowPosting,
    @RequestParam(value="captcha_required", required = false, defaultValue="false") boolean captchaRequired
  ) throws Exception {
    Template tmpl = Template.getTemplate();

    if (!tmpl.isModeratorSession()) {
      throw new IllegalAccessException("Not authorized");
    }

    Optional<OffsetDateTime> banTo = switch (time) {
      case "hour" -> Optional.of(OffsetDateTime.now().plusHours(1));
      case "day" -> Optional.of(OffsetDateTime.now().plusDays(1));
      case "month" -> Optional.of(OffsetDateTime.now().plusMonths(1));
      case "3month" -> Optional.of(OffsetDateTime.now().plusMonths(3));
      case "6month" -> Optional.of(OffsetDateTime.now().plusMonths(6));
      case "custom" -> {
        int days = ServletRequestUtils.getRequiredIntParameter(request, "ban_days");

        if (days <= 0 || days > 180) {
          throw new UserErrorException("Invalid days count");
        }

        yield Optional.of(OffsetDateTime.now().plusDays(days));
      }
      case "unlim" -> Optional.empty();
      case "remove" -> Optional.of(OffsetDateTime.now());
      default -> throw new UserErrorException("Invalid count");
    };

    User moderator = AuthUtil.getCurrentUser();

    ipBlockDao.blockIP(ip, moderator.getId(), reason, banTo.map(v -> new Timestamp(v.toInstant().toEpochMilli())).orElse(null),
            allowPosting, captchaRequired);

    return new ModelAndView(new RedirectView("sameip.jsp?ip=" + URLEncoder.encode(ip, StandardCharsets.UTF_8)));
  }
}
