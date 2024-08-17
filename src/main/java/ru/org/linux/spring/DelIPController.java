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

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.ModelAndView;
import ru.org.linux.auth.AccessViolationException;
import ru.org.linux.auth.AuthUtil;
import ru.org.linux.auth.IPBlockDao;
import ru.org.linux.comment.CommentDeleteService;
import ru.org.linux.comment.DeleteCommentResult;
import ru.org.linux.search.SearchQueueSender;
import ru.org.linux.site.Template;
import ru.org.linux.user.User;
import ru.org.linux.user.UserErrorException;

import java.sql.Timestamp;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;

@Controller
public class DelIPController {
  private final SearchQueueSender searchQueueSender;
  private final CommentDeleteService commentDeleteService;
  private final IPBlockDao ipBlockDao;

  public DelIPController(SearchQueueSender searchQueueSender, CommentDeleteService commentDeleteService,
                         IPBlockDao ipBlockDao) {
    this.searchQueueSender = searchQueueSender;
    this.commentDeleteService = commentDeleteService;
    this.ipBlockDao = ipBlockDao;
  }

  /**
   * Контроллер удаление топиков и сообщений по ip и времени
   * @param reason причина удаления
   * @param ip ip по которому удаляем
   * @param time время за которое удаляем (hour, day, 3day, 5day)
   * @return возвращаем страничку с результатом выполнения
   */
  @RequestMapping(value="/delip.jsp", method= RequestMethod.POST)
  public ModelAndView delIp(@RequestParam("reason") String reason,
                            @RequestParam("ip") String ip,
                            @RequestParam("time") String time,
                            @RequestParam(value = "ban_time", required = false) String banTime,
                            @RequestParam(value = "ban_mode", required = false) String banMode) {
    Map<String, Object> params = new HashMap<>();

    Template tmpl = Template.getTemplate();

    if (!tmpl.isModeratorSession()) {
      throw new AccessViolationException("Not moderator");
    }

    var delFrom = switch (time) {
      case "hour" -> Instant.now().minus(1, ChronoUnit.HOURS);
      case "day" -> Instant.now().minus(1, ChronoUnit.DAYS);
      case "3day" -> Instant.now().minus(3, ChronoUnit.DAYS);
      case "5day" ->Instant.now().minus(5, ChronoUnit.DAYS);
      default -> throw new UserErrorException("Invalid count");
    };

    Timestamp ts = new Timestamp(delFrom.toEpochMilli());
    params.put("message", "Удаляем темы и сообщения после "+ ts +" с IP "+ip+"<br>");

    User moderator = AuthUtil.getCurrentUser();

    if (banTime != null) {
      Optional<OffsetDateTime> banTo = switch (banTime) {
        case "hour" -> Optional.of(OffsetDateTime.now().plusHours(1));
        case "day" -> Optional.of(OffsetDateTime.now().plusDays(1));
        case "month" -> Optional.of(OffsetDateTime.now().plusMonths(1));
        case "3month" -> Optional.of(OffsetDateTime.now().plusMonths(3));
        case "6month" -> Optional.of(OffsetDateTime.now().plusMonths(6));
        case "unlim" -> Optional.empty();
        case "remove" -> Optional.of(OffsetDateTime.now());
        default -> throw new UserErrorException("Invalid count");
      };

      boolean allowPosting, captchaRequired;

      switch (banMode) {
        case "anonymous_and_captcha" -> {
          allowPosting = true;
          captchaRequired = true;
        }
        case "anonymous_only" -> {
          allowPosting = true;
          captchaRequired = false;
        }
        default -> {
          allowPosting = false;
          captchaRequired = false;
        }
      }

      ipBlockDao.blockIP(ip, moderator.getId(), reason, banTo.orElse(null), allowPosting, captchaRequired);
    }

    DeleteCommentResult deleteResult = commentDeleteService.deleteCommentsByIPAddress(ip, ts, moderator, reason);

    params.put("topics", deleteResult.getDeletedTopicIds().size());
    params.put("comments", deleteResult.getDeletedCommentIds().size());
    params.put("skipped", deleteResult.getSkippedComments());

    for (int topicId : deleteResult.getDeletedTopicIds()) {
      searchQueueSender.updateMessage(topicId, true);
    }

    searchQueueSender.updateComment(deleteResult.getDeletedCommentIds());

    return new ModelAndView("delip", params);
  }
}
