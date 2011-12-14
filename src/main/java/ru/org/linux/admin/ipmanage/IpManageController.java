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
import ru.org.linux.comment.DeleteCommentResult;
import ru.org.linux.site.ScriptErrorException;
import ru.org.linux.site.Template;
import ru.org.linux.user.AccessViolationException;
import ru.org.linux.user.User;
import ru.org.linux.util.ServletParameterParser;

import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.net.URLEncoder;
import java.sql.Timestamp;
import java.util.HashMap;
import java.util.Map;

@Controller
public class IpManageController {
  @Autowired
  private BanIpService banIpService;

  @Autowired
  private DeleteMessagesByIpService deleteMessagesByIpService;

  @Autowired
  SameIpService sameIpService;

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
  @RequestMapping(value = "/admin/ipmanage/ban", method = RequestMethod.POST)
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
    Timestamp timestamp = banIpService.calculateTimestamp(banPeriod, days);
    banIpService.doBan(user, ip, reason, timestamp);

    return new ModelAndView(new RedirectView("/admin/ipmanage/same?ip=" + URLEncoder.encode(ip, "UTF-8")));
  }

  /**
   * Контроллер удаление топиков и сообщений по ip и времени.
   *
   * @param request http запрос (для получения текущего пользователя)
   * @param reason  причина удаления
   * @param ip      ip по которому удаляем
   * @param time    время за которое удаляем (hour, day, 3day)
   * @return возвращаем страничку с результатом выполнения
   * @throws Exception по дороге может что-то сучится
   */
  @RequestMapping(value = "/admin/ipmanage/delmessages", method = RequestMethod.POST)
  public ModelAndView deleteMessagesByIpRequestHandler(HttpServletRequest request,
                                                       @RequestParam("reason") String reason,
                                                       @RequestParam("ip") String ip,
                                                       @RequestParam("time") String time
  ) throws Exception {
    Map<String, Object> params = new HashMap<String, Object>();

    Template tmpl = Template.getTemplate(request);

    if (!tmpl.isModeratorSession()) {
      throw new AccessViolationException("Not moderator");
    }

    User moderator = tmpl.getCurrentUser();
    Timestamp ts = deleteMessagesByIpService.calculateTimestamp(DeletePeriodEnum.valueOf(time));

    DeleteCommentResult deleteResult = deleteMessagesByIpService.deleteMessages(ip, moderator, ts, reason);

    params.put("message", "Удаляем темы и сообщения после " + ts.toString() + " с IP " + ip + "<br>");
    params.put("topics", deleteResult.getDeletedTopicIds().size()); // кол-во удаленных топиков
    params.put("deleted", deleteResult.getDeleteInfo());

    return new ModelAndView("admin/ipmanage/delip", params);
  }

  /**
   * Обработчик web-запроса поиска информации по IP-адресу.
   *
   * @param request
   * @param msgid
   * @return
   * @throws Exception
   */
  @RequestMapping("/admin/ipmanage/same")
  public ModelAndView sameIpRequestHandler(
    HttpServletRequest request,
    @RequestParam(required = false) Integer msgid
  ) throws Exception {
    Template tmpl = Template.getTemplate(request);

    if (!tmpl.isModeratorSession()) {
      throw new AccessViolationException("Not moderator");
    }

    ModelAndView modelAndView = new ModelAndView("admin/ipmanage/sameip");

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
