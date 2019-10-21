/*
 * Copyright 1998-2019 Linux.org.ru
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

import java.net.URLEncoder
import javax.servlet.http.HttpServletRequest

import org.joda.time.DateTime
import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.{ModelAttribute, RequestMapping, RequestParam}
import org.springframework.web.servlet.view.RedirectView
import org.springframework.web.servlet.{ModelAndView, View}
import ru.org.linux.site.Template
import ru.org.linux.user.{UserErrorException, UserService}

import scala.jdk.CollectionConverters._

@Controller
class TrackerController(trackerDao: TrackerDao, userService: UserService) {
  @ModelAttribute("filters")
  def getFilter: java.util.List[TrackerFilterEnum] = TrackerFilterEnum.values.toSeq.asJava

  @RequestMapping(Array("/tracker.jsp"))
  @throws[Exception]
  def trackerOldUrl(@RequestParam(value = "filter", defaultValue = "all") filterAction: String,
                    request: HttpServletRequest): View = {
    val tmpl = Template.getTemplate(request)
    val defaultFilter = tmpl.getProf.getTrackerMode
    val redirectView = new RedirectView("/tracker/")

    redirectView.setExposeModelAttributes(false)
    val filter = TrackerFilterEnum.getByValue(filterAction)

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
    val trackerFilter = TrackerFilterEnum.getByValue(filterAction).or(defaultFilter)

    val params = new java.util.HashMap[String, AnyRef]

    params.put("offset", Integer.valueOf(offset))
    params.put("filter", trackerFilter.getValue)

    if (trackerFilter != defaultFilter) {
      params.put("addition_query", "&amp;filter=" + trackerFilter.getValue)
    } else {
      params.put("addition_query", "")
    }

    params.put("defaultFilter", defaultFilter)

    val startDate = DateTime.now.minusDays(4).toDate
    val messages = tmpl.getProf.getMessages
    val topics = tmpl.getProf.getTopics

    params.put("topics", Integer.valueOf(topics))

    val user = tmpl.getCurrentUser
    params.put("title", makeTitle(trackerFilter, defaultFilter))

    val trackerTopics = trackerDao.getTrackAll(trackerFilter, user, startDate, topics, offset, messages).asScala

    params.put("msgs", trackerTopics.asJava)

    if (tmpl.isModeratorSession) {
      params.put("newUsers", userService.getNewUsers)
    }

    val userAgent = Option(request.getHeader("user-agent"))

    val useNew = user!=null && !tmpl.getProf.isOldTracker &&
      userAgent.forall(agent => !agent.contains("Opera Mini") && !agent.contains("MSIE")) &&
      ((user.getId % 20 < 5) || user.isAdministrator)

    if (user!=null && useNew) {
      new ModelAndView("tracker-new", params)
    } else {
      new ModelAndView("tracker", params)
    }
  }
}