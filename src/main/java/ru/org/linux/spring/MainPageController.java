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

package ru.org.linux.spring;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.ModelAndView;
import ru.org.linux.ApplicationController;
import ru.org.linux.site.Template;
import ru.org.linux.topic.TopicListService;
import ru.org.linux.topic.Topic;
import ru.org.linux.topic.TopicPrepareService;

import javax.servlet.http.HttpServletRequest;
import javax.sql.DataSource;
import java.util.List;

@Controller
public class MainPageController extends ApplicationController {
  @Autowired
  private TopicPrepareService prepareService;

  @Autowired
  private TopicListService topicListService;

  private JdbcTemplate jdbcTemplate;

  @Autowired
  public void setDataSource(DataSource ds) {
    jdbcTemplate = new JdbcTemplate(ds);
  }

  @RequestMapping({"/", "/index.jsp"})
  public ModelAndView mainPage(HttpServletRequest request) {
    Template tmpl = Template.getTemplate(request);

    List<Topic> messages = topicListService.getMainPageFeed(tmpl.getProf().isShowGalleryOnMain());

    ModelAndView modelAndView = new ModelAndView("index");

    modelAndView.addObject(
      "news",
      prepareService.prepareMessagesForUser(messages, request.isSecure(), tmpl.getCurrentUser())
    );

    if (tmpl.isModeratorSession() || tmpl.isCorrectorSession()) {
      int uncommited = jdbcTemplate.queryForInt("select count(*) from topics,groups,sections where section=sections.id AND sections.moderate and topics.groupid=groups.id and not deleted and not topics.moderate AND postdate>(CURRENT_TIMESTAMP-'1 month'::interval)");

      modelAndView.addObject("uncommited", uncommited);

      int uncommitedNews = 0;

      if (uncommited > 0) {
        uncommitedNews = jdbcTemplate.queryForInt("select count(*) from topics,groups where section=1 AND topics.groupid=groups.id and not deleted and not topics.moderate AND postdate>(CURRENT_TIMESTAMP-'1 month'::interval)");
      }

      modelAndView.addObject("uncommitedNews", uncommitedNews);
    }

    return render(modelAndView);
  }
}
