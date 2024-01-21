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

package ru.org.linux.spring;

import com.google.common.collect.ImmutableList;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.ModelAndView;
import ru.org.linux.auth.AccessViolationException;
import ru.org.linux.auth.IPBlockDao;
import ru.org.linux.auth.IPBlockInfo;
import ru.org.linux.sameip.SameIpService;
import ru.org.linux.site.BadInputException;
import ru.org.linux.site.Template;
import ru.org.linux.spring.dao.UserAgentDao;
import ru.org.linux.user.UserAndAgent;
import ru.org.linux.user.UserService;
import scala.Tuple2;

import java.util.List;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Controller
public class SameIPController {
  private static final Pattern ipRE = Pattern.compile("^\\d+\\.\\d+\\.\\d+\\.\\d+$");

  private final IPBlockDao ipBlockDao;

  private final UserService userService;

  private final UserAgentDao userAgentDao;

  private final SameIpService sameIpService;

  public SameIPController(IPBlockDao ipBlockDao, UserService userService, UserAgentDao userAgentDao,
                          SameIpService sameIpService) {
    this.ipBlockDao = ipBlockDao;
    this.userService = userService;
    this.userAgentDao = userAgentDao;
    this.sameIpService = sameIpService;
  }

  @ModelAttribute("masks")
  public List<Tuple2<Integer, String>> getMasks() {
    return ImmutableList.of(
            Tuple2.apply(32, "IP"),
            Tuple2.apply(24, "Сеть /24"),
            Tuple2.apply(16, "Сеть /16"),
            Tuple2.apply(0, "Не фильтровать"));
  }

  @RequestMapping("/sameip.jsp")
  public ModelAndView sameIP(
    @RequestParam(required = false) String ip,
    @RequestParam(required = false, defaultValue = "32") int mask,
    @RequestParam(required = false, name="ua") Integer userAgent
  ) {
    Template tmpl = Template.getTemplate();

    if (!tmpl.isModeratorSession()) {
      throw new AccessViolationException("Not moderator");
    }

    ModelAndView mv = new ModelAndView("sameip");

    if (mask<0 || mask>32) {
      throw new BadInputException("bad mask");
    }

    String ipMask;

    if (ip!=null) {
      Matcher matcher = ipRE.matcher(ip);

      if (!matcher.matches()) {
        throw new BadInputException("not ip");
      }

      if (mask==0) {
        ipMask = null;
      } else if (mask!=32) {
        ipMask = ip + "/" + mask;
      } else {
        ipMask = ip;
      }
    } else {
      ipMask = null;
    }

    int rowsLimit = 50;

    var comments = sameIpService.getPosts(Optional.ofNullable(ipMask), Optional.ofNullable(userAgent), rowsLimit);

    mv.getModel().put("comments", comments);
    mv.getModel().put("hasMoreComments", comments.size() == rowsLimit);
    mv.getModel().put("rowsLimit", rowsLimit);

    List<UserAndAgent> users = userService.getUsersWithAgent(ipMask, userAgent, rowsLimit);
    mv.getModel().put("users", users);
    mv.getModel().put("hasMoreUsers", users.size() == rowsLimit);

    if (ip != null) {
      mv.getModel().put("ip", ip);
      mv.getModel().put("mask", mask);
      boolean hasMask = mask<32;
      mv.getModel().put("hasMask", hasMask);

      if (!hasMask) {
        IPBlockInfo blockInfo = ipBlockDao.getBlockInfo(ip);

        boolean allowPosting = false;
        boolean captchaRequired = true;

        if (blockInfo.isInitialized()) {
          mv.getModel().put("blockInfo", blockInfo);
          allowPosting = blockInfo.isAllowRegistredPosting();
          captchaRequired = blockInfo.isCaptchaRequired();

          if (blockInfo.getModerator() != 0) {
            mv.getModel().put("blockModerator", userService.getUserCached(blockInfo.getModerator()));
          }
        }
        mv.addObject("allowPosting", allowPosting);
        mv.addObject("captchaRequired", captchaRequired);
      }
    }

    mv.getModel().put("newUsers", userService.getNewUsersByUAIp(ipMask, userAgent));

    if (userAgent!=null) {
      mv.getModel().put("userAgent", userAgentDao.getUserAgentById(userAgent).orElse(null));
      mv.getModel().put("ua", userAgent);
    }

    return mv;
  }
}
