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
import ru.org.linux.user.UserErrorException;

import javax.servlet.http.HttpServletRequest;
import java.net.URLEncoder;
import java.sql.Timestamp;
import java.util.Calendar;
import java.util.Date;

@Controller
public class BanIPController {
  private IPBlockDao ipBlockDao;

  @Autowired
  public void setIpBlockDao(IPBlockDao ipBlockDao) {
    this.ipBlockDao = ipBlockDao;
  }

  @RequestMapping(value = "/banip.jsp", method = RequestMethod.POST)
  public ModelAndView banIP(
    HttpServletRequest request,
    @RequestParam("ip") String ip,
    @RequestParam("reason") String reason,
    @RequestParam("time") String time
  ) throws Exception {
    Template tmpl = Template.getTemplate(request);

    if (!tmpl.isModeratorSession()) {
      throw new IllegalAccessException("Not authorized");
    }
    BanPeriodEnum banPeriodEnum = BanPeriodEnum.valueOf(time);

    int days = (BanPeriodEnum.CUSTOM.equals(banPeriodEnum))
      ? ServletRequestUtils.getRequiredIntParameter(request, "ban_days")
      : 0;

    Calendar calendar = Calendar.getInstance();
    calendar.setTime(new Date());
    switch (banPeriodEnum) {
      case HOUR_1:
        calendar.add(Calendar.HOUR_OF_DAY, 1);
        break;
      case DAY_1:
        calendar.add(Calendar.DAY_OF_MONTH, 1);
        break;
      case MONTH_1:
        calendar.add(Calendar.MONTH, 1);
        break;
      case MONTH_3:
        calendar.add(Calendar.MONTH, 3);
        break;
      case MONTH_6:
        calendar.add(Calendar.MONTH, 6);
        break;
      case CUSTOM:
        if (days <= 0 || days > 180) {
          throw new UserErrorException("Invalid days count");
        }
        calendar.add(Calendar.DAY_OF_MONTH, days);
        break;
      default:
        break;
    }
    Timestamp ts = (BanPeriodEnum.PERMANENT.equals(banPeriodEnum))
      ? null
      : new Timestamp(calendar.getTimeInMillis());

    User user = tmpl.getCurrentUser();
    user.checkCommit();

    ipBlockDao.blockIP(ip, user, reason, ts);

    return new ModelAndView(new RedirectView("sameip.jsp?ip=" + URLEncoder.encode(ip)));
  }
}
