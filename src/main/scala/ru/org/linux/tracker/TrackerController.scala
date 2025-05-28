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
package ru.org.linux.tracker

import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.{ModelAttribute, RequestMapping, RequestParam}
import org.springframework.web.servlet.view.RedirectView
import org.springframework.web.servlet.{ModelAndView, View}
import ru.org.linux.auth.AuthUtil.MaybeAuthorized
import ru.org.linux.auth.IPBlockDao
import ru.org.linux.group.GroupListDao
import ru.org.linux.topic.{TopicPrepareService, TopicService}
import ru.org.linux.user.{UserErrorException, UserService}

import java.net.URLEncoder
import scala.jdk.CollectionConverters.*
import scala.jdk.OptionConverters.RichOptional

@Controller
class TrackerController(groupListDao: GroupListDao, userService: UserService, ipBlockDao: IPBlockDao,
                        topicPrepareService: TopicPrepareService, topicService: TopicService) {
  @ModelAttribute("filters")
  def getFilter: java.util.List[TrackerFilterEnum] = TrackerFilterEnum.values.toSeq.asJava

  @RequestMapping(path = Array("/tracker.jsp"))
  @throws[Exception]
  def trackerOldUrl(@RequestParam(value = "filter", defaultValue = "all") filterAction: String): View = MaybeAuthorized { session =>
    val defaultFilter = session.profile.trackerMode
    val redirectView = new RedirectView("/tracker/")

    redirectView.setExposeModelAttributes(false)
    val filter = TrackerFilterEnum.getByValue(filterAction).toScala

    if (!filter.contains(defaultFilter)) {
      redirectView.setUrl("/tracker/?filter=" + URLEncoder.encode(filterAction, "UTF-8"))
    }

    redirectView
  }

  private def makeTitle(filter: TrackerFilterEnum, defaultFilter: TrackerFilterEnum) =
    if (filter != defaultFilter)
      "Последние сообщения (" + filter.getLabel + ")"
    else
      "Последние сообщения"

  private def buildTrackerUrl(offset: Int, filter: Option[TrackerFilterEnum]): String = {
    val additionalQuery = filter.map("filter=" + _.getValue)

    if (offset > 0) {
      s"/tracker/?offset=$offset${additionalQuery.map("&amp;" + _).getOrElse("")}"
    } else {
      s"/tracker/${additionalQuery.map("?" + _).getOrElse("")}"
    }
  }

  @RequestMapping(path = Array("/tracker"))
  @throws[Exception]
  def tracker(@RequestParam(value = "filter", required = false) filterAction: String,
              @RequestParam(value = "offset", required = false, defaultValue = "0") offset: Int
             ): ModelAndView = MaybeAuthorized { implicit session =>
    if (offset < 0 || offset > 300) throw new UserErrorException("Некорректное значение offset")

    val defaultFilter = session.profile.trackerMode
    val trackerFilter = TrackerFilterEnum.getByValue(filterAction).orElse(defaultFilter)

    val params = new java.util.HashMap[String, AnyRef]

    params.put("filter", trackerFilter.getValue)

    params.put("defaultFilter", defaultFilter)

    val topics = session.profile.topics

    params.put("title", makeTitle(trackerFilter, defaultFilter))

    val trackerTopics = groupListDao.getTrackerTopics(trackerFilter, offset)
      .map(topicPrepareService.prepareListItem)

    params.put("messages", trackerTopics.asJava)

    if (offset < 300 && trackerTopics.size == topics) {
      params.put("nextLink", buildTrackerUrl(offset + topics, Some(trackerFilter).filter(_ != defaultFilter)))
    }

    if (offset >= topics) {
      params.put("prevLink", buildTrackerUrl(offset - topics, Some(trackerFilter).filter(_ != defaultFilter)))
    }

    if (session.moderator) {
      params.put("newUsers", userService.getNewUsers)
      params.put("frozenUsers", userService.getFrozenUsers.asJava)
      params.put("unFrozenUsers", userService.getUnFrozenUsers.asJava)
      params.put("blockedUsers", userService.getRecentlyBlocked.asJava)
      params.put("unBlockedUsers", userService.getRecentlyUnBlocked.asJava)
      params.put("recentUserpics", userService.getRecentUserpics.asJava)
      params.put("blockedIps", ipBlockDao.getRecentlyBlocked)
      params.put("unBlockedIps", ipBlockDao.getRecentlyUnBlocked)
    } else {
      params.put("newUsers", Seq.empty.asJava)
      params.put("frozenUsers", Seq.empty.asJava)
      params.put("unFrozenUsers", Seq.empty.asJava)
      params.put("blockedUsers", Seq.empty.asJava)
      params.put("unBlockedUsers", Seq.empty.asJava)
      params.put("recentUserpics", Seq.empty.asJava)
      params.put("blockedIps", Seq.empty.asJava)
      params.put("unBlockedIps", Seq.empty.asJava)
    }

    if (session.moderator || session.corrector) {
      val uncommitedCounts = topicService.getUncommitedCounts

      params.put("uncommitedCounts", uncommitedCounts.asJava)
    }

    new ModelAndView("tracker-new", params)
  }
}