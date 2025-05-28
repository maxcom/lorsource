/*
 * Copyright 1998-2025 Linux.org.ru
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

import jakarta.servlet.http.HttpServletResponse
import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.servlet.ModelAndView
import ru.org.linux.auth.AuthUtil.MaybeAuthorized
import ru.org.linux.group.GroupPermissionService
import ru.org.linux.section.{Section, SectionService}
import ru.org.linux.topic.*
import ru.org.linux.user.MemoriesDao

import scala.jdk.CollectionConverters.{ListHasAsScala, SeqHasAsJava}

@Controller
class MainPageController(prepareService: TopicPrepareService, topicListService: TopicListService, topicDao: TopicDao,
                         memoriesDao: MemoriesDao, groupPermissionService: GroupPermissionService,
                         sectionService: SectionService, topicService: TopicService) {
  @RequestMapping(path = Array("/", "/index.jsp"))
  def mainPage(response: HttpServletResponse): ModelAndView = MaybeAuthorized { implicit session =>
    response.setDateHeader("Expires", System.currentTimeMillis - 20 * 3600 * 1000)
    response.setDateHeader("Last-Modified", System.currentTimeMillis)

    val allTopics = topicListService.getMainPageFeed(25)

    val (messages, titles) = allTopics.foldLeft((Vector.empty[Topic], Vector.empty[Topic])) { case ((big, small), topic) =>
      if (big.count(!_.minor)<5) {
        (big :+ topic, small)
      } else {
        (big, small :+ topic)
      }
    }

    val mv = new ModelAndView("index")

    mv.getModel.put("news", prepareService.prepareTopics(messages, loadUserpics = false).asJava)

    val briefNewsByDate = TopicListTools.datePartition(titles)

    mv.getModel.put(
      "briefNews",
      TopicListTools.split(briefNewsByDate.map(p => p._1 -> prepareService.prepareBrief(p._2, groupInTitle = false))))

    session.userOpt.foreach { user =>
      mv.getModel.put("hasDrafts", Boolean.box(topicDao.hasDrafts(user)))
      mv.getModel.put("favPresent", Boolean.box(memoriesDao.isFavPresetForUser(user)))
    }

    if (session.moderator || session.corrector) {
      val uncommitedCounts = topicService.getUncommitedCounts
      val uncommited = uncommitedCounts.map(_._2).sum

      mv.getModel.put("uncommited", Int.box(uncommited))
      mv.getModel.put("uncommitedCounts", uncommitedCounts.asJava)
    }

    mv.getModel.put("showAdsense", Boolean.box(!session.authorized || !session.profile.hideAdsense))

    val sectionNews = sectionService.getSection(Section.SECTION_NEWS)

    if (groupPermissionService.isTopicPostingAllowed(sectionNews)) {
      mv.getModel.put("addNews", AddTopicController.getAddUrl(sectionNews))
    }

    mv
  }
}