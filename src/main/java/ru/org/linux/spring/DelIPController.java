/*
 * Copyright 1998-2015 Linux.org.ru
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

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.ModelAndView;
import ru.org.linux.auth.AccessViolationException;
import ru.org.linux.comment.CommentService;
import ru.org.linux.comment.DeleteCommentResult;
import ru.org.linux.search.SearchQueueSender;
import ru.org.linux.site.Template;
import ru.org.linux.user.User;
import ru.org.linux.user.UserErrorException;

import javax.servlet.http.HttpServletRequest;
import java.sql.Timestamp;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

@Controller
public class DelIPController {
  @Autowired
  private SearchQueueSender searchQueueSender;

  @Autowired
  private CommentService commentService;

  /**
   * Контроллер удаление топиков и сообщений по ip и времени
   * @param request http запрос (для получения текущего пользователя)
   * @param reason причина удаления
   * @param ip ip по которому удаляем
   * @param time время за которое удаляем (hour, day, 3day)
   * @return возвращаем страничку с результатом выполнения
   * @throws Exception по дороге может что-то сучится
   */
  @RequestMapping(value="/delip.jsp", method= RequestMethod.POST)
  public ModelAndView delIp(HttpServletRequest request,
                            @RequestParam("reason") String reason,
                            @RequestParam("ip") String ip,
                            @RequestParam("time") String time
                            ) throws Exception {
    Map<String, Object> params = new HashMap<>();

    Template tmpl = Template.getTemplate(request);

    if (!tmpl.isModeratorSession()) {
      throw new AccessViolationException("Not moderator");
    }

    Calendar calendar = Calendar.getInstance();
    calendar.setTime(new Date());

    if ("hour".equals(time)) {
      calendar.add(Calendar.HOUR_OF_DAY, -1);
    } else if ("day".equals(time)) {
      calendar.add(Calendar.DAY_OF_MONTH, -1);
    } else if ("3day".equals(time)) {
      calendar.add(Calendar.DAY_OF_MONTH, -3);
    } else {
      throw new UserErrorException("Invalid count");
    }

    Timestamp ts = new Timestamp(calendar.getTimeInMillis());
    params.put("message", "Удаляем темы и сообщения после "+ts.toString()+" с IP "+ip+"<br>");

    User moderator = tmpl.getCurrentUser();

    DeleteCommentResult deleteResult = commentService.deleteCommentsByIPAddress(ip, ts, moderator, reason);

    params.put("topics", deleteResult.getDeletedTopicIds().size()); // кол-во удаленных топиков
    params.put("deleted", deleteResult.getDeleteInfo());

    for(int topicId : deleteResult.getDeletedTopicIds()) {
      searchQueueSender.updateMessage(topicId, true);
    }

    searchQueueSender.updateComment(deleteResult.getDeletedCommentIds());

    return new ModelAndView("delip", params);
  }
}
