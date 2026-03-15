/*
 * Copyright 1998-2026 Linux.org.ru
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

import com.google.common.base.Strings
import org.joda.time.DateTimeZone
import org.springframework.stereotype.Controller
import org.springframework.ui.Model
import org.springframework.validation.BindingResult
import org.springframework.web.bind.WebDataBinder
import org.springframework.web.bind.annotation.{InitBinder, ModelAttribute, RequestAttribute, RequestMapping, RequestMethod}
import ru.org.linux.group.GroupDao
import ru.org.linux.search.SearchEnums.SearchInterval
import ru.org.linux.search.SearchEnums.SearchRange
import ru.org.linux.section.SectionService
import ru.org.linux.user.User
import ru.org.linux.user.UserPropertyEditor
import ru.org.linux.user.UserService
import ru.org.linux.util.ExceptionBindingErrorProcessor

import java.beans.PropertyEditorSupport
import scala.collection.immutable.VectorMap
import scala.jdk.CollectionConverters.*

@Controller
class SearchController(sectionService: SectionService, userService: UserService, groupDao: GroupDao,
                       searchService: SearchService) {
  @ModelAttribute("sorts")
  def getSorts: java.util.Map[String, String] = {
    // VectorMap preserves order!
    SearchOrder.values.view.map(v => v.id -> v.name).to(VectorMap).asJava
  }

  @ModelAttribute("intervals")
  def getIntervals: java.util.Map[SearchInterval, String] = {
    SearchInterval.values.view.map(v => v -> v.getTitle).to(VectorMap).asJava
  }

  @ModelAttribute("ranges")
  def getRanges: java.util.Map[SearchEnums.SearchRange, String] = {
    SearchRange.values().view.map(v => v -> v.getTitle).to(VectorMap).asJava
  }

  @RequestMapping(value = Array("/search.jsp"), method = Array(RequestMethod.GET, RequestMethod.HEAD))
  def search(model: Model, @ModelAttribute("query") query: SearchServiceRequest, bindingResult: BindingResult,
             @RequestAttribute(name="timezone") tz: DateTimeZone): String = {
    val params = model.asMap

    if (!query.isInitial && !bindingResult.hasErrors) {
      sanitizeQuery(query)

      val response = searchService.performSearch(query, tz)

      response.sectionFacet.foreach { facet =>
        params.put("sectionFacet", facet.asJava)
      }

      response.groupFacet.foreach { facet =>
        params.put("groupFacet", facet.asJava)
      }

      response.foundTags.foreach { facet =>
        params.put("tags", facet.asJava)
      }

      params.put("result", response.hits.asJava)
      params.put("searchTime", response.took)
      params.put("numFound", response.totalHits)

      if (response.totalHits > query.getOffset + SearchService.SearchRows) {
        params.put("nextLink", "/search.jsp?" + query.getQuery(query.getOffset + SearchService.SearchRows))
      }

      if (query.getOffset - SearchService.SearchRows >= 0) {
        params.put("prevLink", "/search.jsp?" + query.getQuery(query.getOffset - SearchService.SearchRows))
      }
    }

    "search"
  }

  private def sanitizeQuery(query: SearchServiceRequest): Unit = {
    if (!Strings.isNullOrEmpty(query.getSection)) {
      val section = sectionService.fuzzyNameToSection.get(query.getSection)

      if (section.isDefined) {
        query.setSection(section.get.getUrlName)

        if (!Strings.isNullOrEmpty(query.getGroup)) {
          val group = groupDao.getGroupOpt(section.get, query.getGroup, true)

          if (group.isEmpty) {
            query.setGroup("")
          } else {
            query.setGroup(group.get.urlName)
          }
        } else {
          query.setGroup("")
        }
      } else {
        query.setSection("")
        query.setGroup("")
      }
    } else {
      query.setGroup("")
      query.setSection("")
    }
  }

  @InitBinder
  def initBinder(binder: WebDataBinder): Unit = {
    binder.registerCustomEditor(classOf[SearchOrder], new PropertyEditorSupport() {
      @throws[IllegalArgumentException]
      override def setAsText(s: String): Unit = {
        s match {
          case "1" => setValue(SearchOrder.Relevance) // for old links
          case "2" => setValue(SearchOrder.Date)
          case _ => setValue(SearchOrder.valueOf(s))
        }
      }
    })

    binder.registerCustomEditor(classOf[SearchInterval], new PropertyEditorSupport() {
      @throws[IllegalArgumentException]
      override def setAsText(s: String): Unit = {
        setValue(SearchInterval.valueOf(s.toUpperCase))
      }
    })

    binder.registerCustomEditor(classOf[SearchEnums.SearchRange], new PropertyEditorSupport() {
      @throws[IllegalArgumentException]
      override def setAsText(s: String): Unit = {
        setValue(SearchRange.valueOf(s.toUpperCase))
      }
    })

    binder.registerCustomEditor(classOf[User], new UserPropertyEditor(userService))
    binder.setBindingErrorProcessor(new ExceptionBindingErrorProcessor)
  }
}