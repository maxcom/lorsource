/*
 * Copyright 1998-2024 Linux.org.ru
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
package ru.org.linux.search

import jakarta.servlet.ServletRequest
import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestMethod
import org.springframework.web.servlet.ModelAndView
import ru.org.linux.auth.AuthUtil.AdministratorOnly
import ru.org.linux.topic.TopicDao

import java.util.Calendar

@Controller
class SearchControlController(searchQueueSender: SearchQueueSender, topicDao: TopicDao) {
  @RequestMapping(value = Array("/admin/search-reindex"), method = Array(RequestMethod.POST), params = Array("action=all"))
  def reindexAll(request: ServletRequest): ModelAndView = AdministratorOnly { _ =>
    val startDate = topicDao.getTimeFirstTopic

    val start = Calendar.getInstance

    start.setTime(startDate)
    start.set(Calendar.DAY_OF_MONTH, 1)
    start.set(Calendar.HOUR, 0)
    start.set(Calendar.MINUTE, 0)

    val i = Calendar.getInstance

    while (i.after(start)) {
      searchQueueSender.updateMonth(i.get(Calendar.YEAR), i.get(Calendar.MONTH) + 1)
      i.add(Calendar.MONTH, -1)
    }

    searchQueueSender.updateMonth(1970, 1)

    new ModelAndView("action-done", "message", "Scheduled reindex")
  }

  @RequestMapping(value = Array("/admin/search-reindex"), method = Array(RequestMethod.POST),
    params = Array("action=current"))
  def reindexCurrentMonth(request: ServletRequest): ModelAndView = AdministratorOnly { _ =>
    val current = Calendar.getInstance

    for (_ <- 0 until 3) {
      searchQueueSender.updateMonth(current.get(Calendar.YEAR), current.get(Calendar.MONTH) + 1)
      current.add(Calendar.MONTH, -1)
    }

    new ModelAndView("action-done", "message", "Scheduled reindex last 3 month")
  }

  @RequestMapping(value = Array("/admin/search-reindex"), method = Array(RequestMethod.GET))
  def reindexAll = AdministratorOnly { _ =>
    new ModelAndView("search-reindex")
  }
}