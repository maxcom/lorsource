/*
 * Copyright 1998-2010 Linux.org.ru
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

package ru.org.linux.admin.ipmanage;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.ModelAndView;
import ru.org.linux.site.ScriptErrorException;
import ru.org.linux.site.Template;
import ru.org.linux.user.AccessViolationException;
import ru.org.linux.util.ServletParameterParser;

import javax.servlet.http.HttpServletRequest;
import java.io.IOException;

@Controller
public class SameIpController {

  @Autowired
  SameIpService sameIpService;

  @Autowired
  BanIpService banIpService;

  /**
   * Обработчик web-запроса поиска информации по IP-адресу.
   *
   * @param request
   * @param msgid
   * @return
   * @throws Exception
   */
  @RequestMapping("/sameip.jsp")
  public ModelAndView sameIP(
    HttpServletRequest request,
    @RequestParam(required = false) Integer msgid
  ) throws Exception {
    Template tmpl = Template.getTemplate(request);

    if (!tmpl.isModeratorSession()) {
      throw new AccessViolationException("Not moderator");
    }


    ModelAndView modelAndView = new ModelAndView("sameip");

    SameIp.IpInfo ipInfo;
    if (msgid != null) {
      ipInfo = sameIpService.getIpInfo(msgid);

      if (ipInfo.getIpAddress() == null) {
        throw new ScriptErrorException("No IP data for #" + msgid);
      }
    } else {
      ipInfo = new SameIp.IpInfo();
      ipInfo.setIpAddress(ServletParameterParser.getIP(request, "ip"));
      ipInfo.setUserAgentId(0);
    }

    modelAndView.addObject("ip", ipInfo.getIpAddress());

    modelAndView.addObject("topics", sameIpService.getForumMessages(ipInfo.getIpAddress(), false));
    modelAndView.addObject("comments", sameIpService.getForumMessages(ipInfo.getIpAddress(), true));
    modelAndView.addObject("users", sameIpService.getUsers(ipInfo.getIpAddress(), ipInfo.getUserAgentId()));

    SameIp.BlockInfo blockInfo = sameIpService.getBlockInfo(ipInfo.getIpAddress());

    try {
      blockInfo.setTor(banIpService.getTor(ipInfo.getIpAddress()));
    } catch (IOException ignored) {
      blockInfo.setTor(false);
    }

    modelAndView.addObject("blockInfo", blockInfo);

    modelAndView.addObject("banPeriods", BanPeriodEnum.getDescriptions());
    modelAndView.addObject("customPeriodName", BanPeriodEnum.CUSTOM.toString());
    modelAndView.addObject("deletePeriods", DeletePeriodEnum.getDescriptions());

    return modelAndView;
  }
}
