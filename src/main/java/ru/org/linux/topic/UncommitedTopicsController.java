/*
 * Copyright 1998-2016 Linux.org.ru
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

package ru.org.linux.topic;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.ModelAndView;
import ru.org.linux.section.Section;
import ru.org.linux.section.SectionNotFoundException;
import ru.org.linux.section.SectionService;
import ru.org.linux.site.Template;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

@Controller
@RequestMapping(value = "/view-all.jsp", method = {RequestMethod.GET, RequestMethod.HEAD})
public class UncommitedTopicsController {
  private final SectionService sectionService;

  private final TopicListService topicListService;

  private final TopicPrepareService prepareService;

  public UncommitedTopicsController(SectionService sectionService, TopicListService topicListService, TopicPrepareService prepareService) {
    this.sectionService = sectionService;
    this.topicListService = topicListService;
    this.prepareService = prepareService;
  }

  @RequestMapping
  public ModelAndView viewAll(
          @RequestParam(value = "section", required = false, defaultValue = "0") int sectionId,
          HttpServletRequest request,
          HttpServletResponse response
  ) {
    Template tmpl = Template.getTemplate(request);

    ModelAndView modelAndView = new ModelAndView("view-all");

    Section section = null;
    if (sectionId != 0) {
      section = sectionService.getSection(sectionId);
      modelAndView.addObject("section", section);
      modelAndView.addObject("addlink", AddTopicController.getAddUrl(section));
    }

    response.setDateHeader("Expires", new Date(System.currentTimeMillis() - 20 * 3600 * 1000).getTime());
    response.setDateHeader("Last-Modified", new Date(System.currentTimeMillis() - 120 * 1000).getTime());

    String title;

    switch (sectionId) {
      case Section.SECTION_NEWS:
        title = "Неподтвержденные новости";
        break;
      case Section.SECTION_POLLS:
        title = "Неподтвержденные опросы";
        break;
      case Section.SECTION_GALLERY:
        title = "Неподтвержденные изображения";
        break;
      case 0:
        title = "Просмотр неподтвержденных сообщений";
        break;
      default:
        title = "Неподтвержденные: "+section.getName();
        break;
    }

    modelAndView.addObject("title", title);

    Calendar calendar = Calendar.getInstance();
    calendar.setTime(new Date());
    calendar.add(Calendar.MONTH, -3);

    List<Topic> messages = topicListService.getUncommitedTopic(section, calendar.getTime(),
            tmpl.isModeratorSession() || tmpl.isCorrectorSession());

    modelAndView.addObject(
            "messages",
            prepareService.prepareMessagesForUser(
                    messages,
                    tmpl.getCurrentUser(),
                    tmpl.getProf(),
                    false
            )
    );

    List<DeletedTopic> deleted = topicListService.getDeletedTopics(sectionId, !tmpl.isModeratorSession());

    modelAndView.addObject("deletedTopics", deleted);
    modelAndView.addObject("sections", sectionService.getSectionList());

    return modelAndView;
  }

  @ExceptionHandler(SectionNotFoundException.class)
  @ResponseStatus(HttpStatus.NOT_FOUND)
  public ModelAndView handleNotFoundException() {
    return new ModelAndView("errors/code404");
  }
}
