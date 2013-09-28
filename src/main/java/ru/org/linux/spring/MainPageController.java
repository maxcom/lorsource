/*
 * Copyright 1998-2013 Linux.org.ru
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
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.ModelAndView;
import ru.org.linux.section.Section;
import ru.org.linux.site.Template;
import ru.org.linux.topic.Topic;
import ru.org.linux.topic.TopicDao;
import ru.org.linux.topic.TopicListService;
import ru.org.linux.topic.TopicPrepareService;
import ru.org.linux.user.Profile;
import ru.org.linux.user.MemoriesDao;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.List;

@Controller
public class MainPageController {
  @Autowired
  private TopicPrepareService prepareService;

  @Autowired
  private TopicListService topicListService;

  @Autowired
  private TopicDao topicDao;
  
  @Autowired
  private MemoriesDao memoriesDao;

  private List<Topic> filterMiniNews(List<Topic> messages) {
    ImmutableList.Builder<Topic> filtred = new ImmutableList.Builder<Topic>();

    for (Topic message : messages) {
      if(message.isMinor()) {
        continue;
      }
      filtred.add(message);
    }
    return filtred.build();
  }

  @RequestMapping({"/", "/index.jsp"})
  public ModelAndView mainPage(HttpServletRequest request, HttpServletResponse response) {
    Template tmpl = Template.getTemplate(request);

    response.setDateHeader("Expires", System.currentTimeMillis() - 20 * 3600 * 1000);
    response.setDateHeader("Last-Modified", System.currentTimeMillis() - 2 * 1000);

    List<Topic> messages = topicListService.getMainPageFeed(tmpl.getProf().isShowGalleryOnMain());

    ModelAndView mv = new ModelAndView("index");

    Profile profile = tmpl.getProf();

    mv.getModel().put("news", prepareService.prepareMessagesForUser(
            profile.isMiniNewsBoxletOnMainPage() ? filterMiniNews(messages) : messages,
            request.isSecure(),
            tmpl.getCurrentUser(),
            profile,
            false
    ));
  
    if (tmpl.isSessionAuthorized()) {
      mv.getModel().put("hasDrafts", topicDao.hasDrafts(tmpl.getCurrentUser()));
      mv.getModel().put("favPresent", memoriesDao.isFavPresetForUser(tmpl.getCurrentUser()));
    }

    if (tmpl.isModeratorSession() || tmpl.isCorrectorSession()) {
      int uncommited = topicDao.getUncommitedCount();

      mv.getModel().put("uncommited", uncommited);

      int uncommitedNews = 0;

      if (uncommited > 0) {
        uncommitedNews = topicDao.getUncommitedCount(Section.SECTION_NEWS);
      }

      mv.getModel().put("uncommitedNews", uncommitedNews);
    }

    mv.getModel().put("showAdsense", !tmpl.isSessionAuthorized() || !tmpl.getProf().isHideAdsense());

    return mv;
  }
}
