/*
 * Copyright 1998-2023 Linux.org.ru
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

package ru.org.linux.spring

import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.servlet.ModelAndView
import ru.org.linux.auth.AuthUtil
import ru.org.linux.section.Section
import ru.org.linux.site.Template
import ru.org.linux.topic.*
import ru.org.linux.user.MemoriesDao

import javax.servlet.http.HttpServletResponse
import scala.jdk.CollectionConverters.*

@Controller
class MainPageController(
  prepareService: TopicPrepareService,
  topicListService: TopicListService,
  topicDao: TopicDao,
  memoriesDao: MemoriesDao
) {
  @RequestMapping(Array("/", "/index.jsp"))
  def mainPage(response: HttpServletResponse): ModelAndView = {
    val tmpl = Template.getTemplate

    response.setDateHeader("Expires", System.currentTimeMillis - 20 * 3600 * 1000)
    response.setDateHeader("Last-Modified", System.currentTimeMillis - 2 * 1000)

    val profile = tmpl.getProf

    val allTopics = topicListService.getMainPageFeed(tmpl.getProf.isShowGalleryOnMain, 25,
      profile.isMiniNewsBoxletOnMainPage)

    val (messages, titles) = allTopics.foldLeft((Vector.empty[Topic], Vector.empty[Topic])) { case ((big, small), topic) =>
      if (big.count(!_.minor)<5) {
        (big :+ topic, small)
      } else {
        (big, small :+ topic)
      }
    }

    val mv = new ModelAndView("index")

    mv.getModel.put("news",
      prepareService.prepareTopicsForUser(
        messages,
        AuthUtil.getCurrentUser,
        profile,
        loadUserpics = false)
    )

    val briefNewsByDate = TopicListTools.datePartition(titles)

    mv.getModel.put(
      "briefNews",
      TopicListTools.split(briefNewsByDate.map(p => p._1 -> BriefTopicRef.fromTopicNoGroup(p._2))))

    if (tmpl.isSessionAuthorized) {
      mv.getModel.put("hasDrafts", Boolean.box(topicDao.hasDrafts(AuthUtil.getCurrentUser)))
      mv.getModel.put("favPresent", Boolean.box(memoriesDao.isFavPresetForUser(AuthUtil.getCurrentUser)))
    }

    if (tmpl.isModeratorSession || tmpl.isCorrectorSession) {
      val uncommited = topicDao.getUncommitedCount

      mv.getModel.put("uncommited", Int.box(uncommited))

      val uncommitedNews = if (uncommited > 0) {
        topicDao.getUncommitedCount(Section.SECTION_NEWS)
      } else {
        0
      }

      mv.getModel.put("uncommitedNews", Int.box(uncommitedNews))
    }

    mv.getModel.put("showAdsense", Boolean.box(!tmpl.isSessionAuthorized || !tmpl.getProf.isHideAdsense))

    mv
  }
}