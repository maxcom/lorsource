/*
 * Copyright 1998-2022 Linux.org.ru
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

import com.google.common.collect.ImmutableList
import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.{ModelAttribute, RequestMapping, RequestParam}
import org.springframework.web.servlet.view.RedirectView
import org.springframework.web.servlet.{ModelAndView, View}
import ru.org.linux.group.GroupListDao
import ru.org.linux.site.Template
import ru.org.linux.user.{UserErrorException, UserService}

import java.net.URLEncoder
import javax.servlet.http.HttpServletRequest
import scala.jdk.CollectionConverters._

@Controller
class TrackerController(groupListDao: GroupListDao, userService: UserService) {
  @ModelAttribute("filters")
  def getFilter(request: HttpServletRequest): java.util.List[TrackerFilterEnum] = {
    val tmpl = Template.getTemplate(request)

    if (tmpl.isModeratorSession) {
      TrackerFilterEnum.values.toSeq.asJava
    } else {
      TrackerFilterEnum.values.toSeq.filterNot(_.isModeratorOnly).asJava
    }
  }

  @RequestMapping(Array("/tracker.jsp"))
  @throws[Exception]
  def trackerOldUrl(@RequestParam(value = "filter", defaultValue = "all") filterAction: String,
                    request: HttpServletRequest): View = {
    val tmpl = Template.getTemplate(request)
    val defaultFilter = tmpl.getProf.getTrackerMode
    val redirectView = new RedirectView("/tracker/")

    redirectView.setExposeModelAttributes(false)
    val filter = TrackerFilterEnum.getByValue(filterAction, tmpl.isModeratorSession)

    if (filter.isPresent && (filter.get ne defaultFilter)) {
      redirectView.setUrl("/tracker/?filter=" + URLEncoder.encode(filterAction, "UTF-8"))
    }

    redirectView
  }

  private def makeTitle(filter: TrackerFilterEnum, defaultFilter: TrackerFilterEnum) =
    if (filter != defaultFilter)
      "Последние сообщения (" + filter.getLabel + ")"
    else
      "Последние сообщения"

  @RequestMapping(Array("/tracker"))
  @throws[Exception]
  def tracker(@RequestParam(value = "filter", required = false) filterAction: String,
              @RequestParam(value = "offset", required = false, defaultValue = "0") offset: Int,
              request: HttpServletRequest): ModelAndView = {
    if (offset < 0 || offset > 300) throw new UserErrorException("Некорректное значение offset")

    val tmpl = Template.getTemplate(request)
    val defaultFilter = tmpl.getProf.getTrackerMode
    val trackerFilter = TrackerFilterEnum.getByValue(filterAction, tmpl.isModeratorSession).orElse(defaultFilter)

    val params = new java.util.HashMap[String, AnyRef]

    params.put("offset", Integer.valueOf(offset))
    params.put("filter", trackerFilter.getValue)

    if (trackerFilter != defaultFilter) {
      params.put("addition_query", "&amp;filter=" + trackerFilter.getValue)
    } else {
      params.put("addition_query", "")
    }

    params.put("defaultFilter", defaultFilter)

    val messages = tmpl.getProf.getMessages
    val topics = tmpl.getProf.getTopics

    params.put("topics", Integer.valueOf(topics))

    val user = Template.getCurrentUser
    params.put("title", makeTitle(trackerFilter, defaultFilter))

    val trackerTopics = groupListDao.getTrackerTopics(trackerFilter, user, topics, offset, messages).asScala

    params.put("msgs", trackerTopics.asJava)

    if (tmpl.isModeratorSession) {
      params.put("newUsers", userService.getNewUsers)
      params.put("frozenUsers", userService.getFrozenUsers)
      params.put("unFrozenUsers", userService.getUnFrozenUsers)
      params.put("blockedUsers", userService.getRecentlyBlocked)
      params.put("unBlockedUsers", userService.getRecentlyUnBlocked)
    } else {
      params.put("newUsers", ImmutableList.of())
      params.put("frozenUsers", ImmutableList.of())
      params.put("unFrozenUsers", ImmutableList.of())
      params.put("blockedUsers", ImmutableList.of())
      params.put("unBlockedUsers", ImmutableList.of())
    }

    if (!tmpl.getProf.isOldTracker) {
      new ModelAndView("tracker-new", params)
    } else {
      new ModelAndView("tracker", params)
    }
  }
}