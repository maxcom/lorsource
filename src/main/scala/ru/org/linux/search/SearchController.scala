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

import com.google.common.base.Strings
import com.google.common.collect.ImmutableSortedMap
import com.sksamuel.elastic4s.ElasticClient
import jakarta.servlet.http.HttpServletRequest
import org.joda.time.DateTimeZone
import org.springframework.stereotype.Controller
import org.springframework.ui.Model
import org.springframework.validation.BindingResult
import org.springframework.web.bind.WebDataBinder
import org.springframework.web.bind.annotation.InitBinder
import org.springframework.web.bind.annotation.ModelAttribute
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestMethod
import ru.org.linux.group.GroupDao
import ru.org.linux.search.SearchEnums.SearchInterval
import ru.org.linux.search.SearchEnums.SearchRange
import ru.org.linux.section.SectionService
import ru.org.linux.user.User
import ru.org.linux.user.UserPropertyEditor
import ru.org.linux.user.UserService
import ru.org.linux.util.ExceptionBindingErrorProcessor

import java.beans.PropertyEditorSupport
import scala.collection.immutable.{SortedMap, VectorMap}
import scala.jdk.CollectionConverters.{IterableHasAsJava, MapHasAsJava}

@Controller
class SearchController(sectionService: SectionService, userService: UserService, groupDao: GroupDao,
                       client: ElasticClient, resultsService: SearchResultsService) {
  @ModelAttribute("sorts")
  def getSorts: java.util.Map[String, String] = {
    // VectorMap preserves order!
    SearchOrder.values.map(v => v.id -> v.name).to(VectorMap).asJava
  }

  @ModelAttribute("intervals")
  def getIntervals: java.util.Map[SearchInterval, String] = {
    val builder = ImmutableSortedMap.naturalOrder[SearchInterval, String]

    for (value <- SearchInterval.values) {
      builder.put(value, value.getTitle)
    }

    SearchInterval.values.view.map(v => v -> v.getTitle).to(SortedMap).asJava
  }

  @ModelAttribute("ranges")
  def getRanges: java.util.Map[SearchEnums.SearchRange, String] = {
    SearchRange.values().view.map(v => v -> v.getTitle).to(SortedMap).asJava
  }

  @RequestMapping(value = Array("/search.jsp"), method = Array(RequestMethod.GET, RequestMethod.HEAD))
  def search(model: Model, @ModelAttribute("query") query: SearchRequest, bindingResult: BindingResult,
             request: HttpServletRequest): String = {
    val params = model.asMap

    if (!query.isInitial && !bindingResult.hasErrors) {
      sanitizeQuery(query)

      val sv = new SearchViewer(query, client)
      val tz = request.getAttribute("timezone").asInstanceOf[DateTimeZone]

      val response = sv.performSearch(tz)
      val current = System.currentTimeMillis

      val res = resultsService.prepareAll(response.hits.hits)

      if (response.aggregations != null) {
        val countFacet = response.aggregations.filter("sections")
        val sectionsFacet = countFacet.terms("sections")

        if (sectionsFacet.buckets.size > 1 || !Strings.isNullOrEmpty(query.getSection)) {
          params.put("sectionFacet", resultsService.buildSectionFacet(countFacet, Option.apply(Strings.emptyToNull(query.getSection))))

          if (!Strings.isNullOrEmpty(query.getSection)) {
            val selectedSection = sectionsFacet.bucketOpt(query.getSection)

            if (!Strings.isNullOrEmpty(query.getGroup)) {
              params.put("groupFacet", resultsService.buildGroupFacet(selectedSection, Some(query.getSection -> query.getGroup)))
            } else {
              params.put("groupFacet", resultsService.buildGroupFacet(selectedSection, None))
            }
          }
        } else if (Strings.isNullOrEmpty(query.getSection) && sectionsFacet.buckets.size == 1) {
          val onlySection = sectionsFacet.buckets.head

          query.setSection(onlySection.key)

          params.put("groupFacet", resultsService.buildGroupFacet(Some(onlySection), None))
        }

        params.put("tags", resultsService.foundTags(response.aggregations))
      }

      val time = System.currentTimeMillis - current

      params.put("result", res.asJavaCollection)
      params.put("searchTime", response.took)
      params.put("numFound", response.totalHits)

      if (response.totalHits > query.getOffset + SearchViewer.SearchRows) {
        params.put("nextLink", "/search.jsp?" + query.getQuery(query.getOffset + SearchViewer.SearchRows))
      }

      if (query.getOffset - SearchViewer.SearchRows >= 0) {
        params.put("prevLink", "/search.jsp?" + query.getQuery(query.getOffset - SearchViewer.SearchRows))
      }

      params.put("time", time)
    }

    "search"
  }

  private def sanitizeQuery(query: SearchRequest): Unit = {
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