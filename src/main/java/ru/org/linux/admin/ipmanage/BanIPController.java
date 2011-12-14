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
import org.springframework.web.bind.ServletRequestUtils;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.view.RedirectView;
import ru.org.linux.site.Template;
import ru.org.linux.user.User;

import javax.servlet.http.HttpServletRequest;
import java.net.URLEncoder;

@Controller
public class BanIPController {
  @Autowired
  private BanIpService banIpService;

  /**
   * Обработкчик запроса на бан по IP-адресу
   *
   * @param request
   * @param ip
   * @param reason
   * @param time
   * @return
   * @throws Exception
   */
  @RequestMapping(value = "/banip.jsp", method = RequestMethod.POST)
  public ModelAndView banIpRequestHandler(
    HttpServletRequest request,
    @RequestParam("ip") String ip,
    @RequestParam("reason") String reason,
    @RequestParam("time") String time
  ) throws Exception {
    Template tmpl = Template.getTemplate(request);

    if (!tmpl.isModeratorSession()) {
      throw new IllegalAccessException("Not authorized");
    }
    BanPeriodEnum banPeriod = BanPeriodEnum.valueOf(time);

    int days = (BanPeriodEnum.CUSTOM.equals(banPeriod))
      ? ServletRequestUtils.getRequiredIntParameter(request, "ban_days")
      : 0;

    User user = tmpl.getCurrentUser();
    banIpService.doBan(user, ip, reason, banPeriod, days);

    return new ModelAndView(new RedirectView("sameip.jsp?ip=" + URLEncoder.encode(ip, "UTF-8")));
  }
}
