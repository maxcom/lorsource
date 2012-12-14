/*
 * Copyright 1998-2012 Linux.org.ru
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
package ru.org.linux.rest;

import com.google.common.base.Enums;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import ru.org.linux.rest.decorator.TrackerListDecorator;
import ru.org.linux.rest.decorator.TrackerItemDecorator;
import ru.org.linux.site.Template;
import ru.org.linux.tracker.TrackerFilterEnum;
import ru.org.linux.tracker.TrackerService;
import ru.org.linux.user.UserErrorException;

import javax.servlet.http.HttpServletRequest;
import java.util.List;

/**
 * Обработчик запросов трекера.
 */
@Controller
@RequestMapping(value = Constants.URL_TRACKER)
public class TrackerRestController {

  @Autowired
  private TrackerService trackerService;

  /**
   * Возвращает список последних сообщений.
   * <p/>
   * Возвращаемые данные:
   * [
   *  {
   *   section: {
   *    link: Ссылка на REST-вызов для получения подробностей
   *    id: уникальный идентификатор секции
   *    alias: краткое обозначение секции (news)
   *    name: название секции (новости)
   *   },
   *   group: {
   *    link: Ссылка на REST-вызов для получения подробностей
   *    id: уникальный идентификатор группы
   *    alias: краткое обозначение группы (general)
   *    name: название группы (общий форум для технических вопросов, не подходящих в другие группы)
   *    countAll: сколько всего сообщений в группе
   *   },
   *   topic: {
   *    link: Ссылка на REST-вызов для получения подробностей
   *    id: уникальный идентификатор,
   *    isCompleted: признак решённого топика (true/false),
   *    author: { // информация об авторе
   *     link: Ссылка на REST-вызов для получения подробностей
   *     id: идентификационный номер пользователя
   *     nick: ник постера
   *     isBanned: зачеркивать ли ник
   *     aliveStars: количество звезд постера
   *     grayStars: количество серых звезд постера
   *     score: только для модераторов - срок пользователя
   *     maxScore: только для модераторов - возможное значение скора
   *     userAgent: только для модераторов - UA
   *    },
   *    title: заголовок топика,
   *    countTotal: всего ответов,
   *    tags: [ названия тегов топика
   *     {
   *      link: Ссылка на REST-вызов для получения списка сообщений с тегом,
   *      name: название тега
   *     }
   *    ],
   *   ,
   *  },
   * ]
   *
   * @param filterName
   * @param pageable
   * @param request
   * @return
   * @throws UserErrorException
   */
  @RequestMapping(method = RequestMethod.GET)
  @ResponseBody
  public List<TrackerItemDecorator> getSectionListHandler(
          @RequestParam(value = "filter", defaultValue = "all") String filterName,
          Pageable pageable,
          HttpServletRequest request
  ) throws UserErrorException {
    TrackerFilterEnum trackerFilter = Enums.getIfPresent(TrackerFilterEnum.class, filterName).or(TrackerFilterEnum.ALL);
    Template template = Template.getTemplate(request);

    return new TrackerListDecorator(
            trackerService.get(template, pageable.getOffset(), trackerFilter)
    );
  }

}
