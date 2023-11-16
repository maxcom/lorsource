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
package ru.org.linux.tracker

import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.{ModelAttribute, RequestMapping, RequestParam}
import org.springframework.web.servlet.view.RedirectView
import org.springframework.web.servlet.{ModelAndView, View}
import ru.org.linux.auth.AuthUtil.AuthorizedOpt
import ru.org.linux.group.GroupListDao
import ru.org.linux.site.Template
import ru.org.linux.user.{UserErrorException, UserService}

import java.net.URLEncoder
import scala.jdk.CollectionConverters.*
import scala.jdk.OptionConverters.{RichOption, RichOptional}

@Controller
class TrackerController(groupListDao: GroupListDao, userService: UserService) {
  @ModelAttribute("filters")
  def getFilter: java.util.List[TrackerFilterEnum] = AuthorizedOpt { currentUserOpt =>
    if (currentUserOpt.exists(_.moderator)) {
      TrackerFilterEnum.values.toSeq.asJava
    } else {
      TrackerFilterEnum.values.toSeq.filterNot(_.isModeratorOnly).asJava
    }
  }

  @RequestMapping(path = Array("/tracker.jsp"))
  @throws[Exception]
  def trackerOldUrl(@RequestParam(value = "filter", defaultValue = "all") filterAction: String): View = AuthorizedOpt { currentUserOpt =>
    val tmpl = Template.getTemplate
    val defaultFilter = tmpl.getProf.getTrackerMode
    val redirectView = new RedirectView("/tracker/")

    redirectView.setExposeModelAttributes(false)
    val filter = TrackerFilterEnum.getByValue(filterAction, currentUserOpt.exists(_.moderator)).toScala

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
             ): ModelAndView = AuthorizedOpt { currentUserOpt =>
    if (offset < 0 || offset > 300) throw new UserErrorException("Некорректное значение offset")

    val tmpl = Template.getTemplate
    val defaultFilter = tmpl.getProf.getTrackerMode
    val trackerFilter = TrackerFilterEnum.getByValue(filterAction, currentUserOpt.exists(_.moderator)).orElse(defaultFilter)

    val params = new java.util.HashMap[String, AnyRef]

    params.put("filter", trackerFilter.getValue)

    params.put("defaultFilter", defaultFilter)

    val messages = tmpl.getProf.getMessages
    val topics = tmpl.getProf.getTopics

    val user = currentUserOpt.map(_.user)
    params.put("title", makeTitle(trackerFilter, defaultFilter))

    val trackerTopics = groupListDao.getTrackerTopics(trackerFilter, user.toJava, topics, offset, messages)

    params.put("messages", trackerTopics)

    if (offset < 300 && trackerTopics.size == topics) {
      params.put("nextLink", buildTrackerUrl(offset + topics, Some(trackerFilter).filter(_ != defaultFilter)))
    }

    if (offset >= topics) {
      params.put("prevLink", buildTrackerUrl(offset - topics, Some(trackerFilter).filter(_ != defaultFilter)))
    }

    if (currentUserOpt.exists(_.moderator)) {
      params.put("newUsers", userService.getNewUsers)
      params.put("frozenUsers", userService.getFrozenUsers)
      params.put("unFrozenUsers", userService.getUnFrozenUsers)
      params.put("blockedUsers", userService.getRecentlyBlocked)
      params.put("unBlockedUsers", userService.getRecentlyUnBlocked)
      params.put("recentUserpics", userService.getRecentUserpics)
    } else {
      params.put("newUsers", Seq.empty.asJava)
      params.put("frozenUsers", Seq.empty.asJava)
      params.put("unFrozenUsers", Seq.empty.asJava)
      params.put("blockedUsers", Seq.empty.asJava)
      params.put("unBlockedUsers", Seq.empty.asJava)
      params.put("recentUserpics", Seq.empty.asJava)
    }

    new ModelAndView("tracker-new", params)
  }
}