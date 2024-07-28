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

package ru.org.linux.search;

import com.google.common.base.Strings;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableMap.Builder;
import com.google.common.collect.ImmutableSortedMap;
import com.sksamuel.elastic4s.ElasticClient;
import com.sksamuel.elastic4s.requests.searches.SearchResponse;
import com.sksamuel.elastic4s.requests.searches.aggs.responses.FilterAggregationResult;
import com.sksamuel.elastic4s.requests.searches.aggs.responses.bucket.TermBucket;
import com.sksamuel.elastic4s.requests.searches.aggs.responses.bucket.Terms;
import org.joda.time.DateTimeZone;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.InitBinder;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import ru.org.linux.group.Group;
import ru.org.linux.group.GroupDao;
import ru.org.linux.search.SearchEnums.SearchInterval;
import ru.org.linux.search.SearchEnums.SearchRange;
import ru.org.linux.section.Section;
import ru.org.linux.section.SectionService;
import ru.org.linux.user.User;
import ru.org.linux.user.UserPropertyEditor;
import ru.org.linux.user.UserService;
import ru.org.linux.util.ExceptionBindingErrorProcessor;
import scala.None$;
import scala.Option;
import scala.Tuple2;

import java.beans.PropertyEditorSupport;
import java.util.Arrays;
import java.util.Collection;
import java.util.Map;
import java.util.Optional;
import javax.servlet.http.HttpServletRequest;

@Controller
public class SearchController {
  private final SectionService sectionService;
  private final UserService userService;
  private final GroupDao groupDao;
  private final ElasticClient client;
  private final SearchResultsService resultsService;

  public SearchController(SectionService sectionService, UserService userService, GroupDao groupDao,
                          ElasticClient client, SearchResultsService resultsService) {
    this.sectionService = sectionService;
    this.userService = userService;
    this.groupDao = groupDao;
    this.client = client;
    this.resultsService = resultsService;
  }

  @ModelAttribute("sorts")
  public static Map<String, String> getSorts() {
    Builder<String, String> builder = ImmutableMap.builder(); // preserves order!

    for (SearchOrder value : SearchOrder$.MODULE$.jvalues()) {
      builder.put(value.id(), value.name());
    }

    return builder.build();
  }

  @ModelAttribute("intervals")
  public static Map<SearchInterval, String> getIntervals() {
    Builder<SearchInterval, String> builder = ImmutableSortedMap.naturalOrder();

    for (SearchInterval value : SearchInterval.values()) {
      builder.put(value, value.getTitle());
    }

    return builder.build();
  }

  @ModelAttribute("ranges")
  public static Map<SearchRange, String> getRanges() {
    Builder<SearchRange, String> builder = ImmutableSortedMap.naturalOrder();

    for (SearchRange value : SearchRange.values()) {
      builder.put(value, value.getTitle());
    }

    return builder.build();
  }

  @RequestMapping(value = "/search.jsp", method = {RequestMethod.GET, RequestMethod.HEAD})
  public String search(
          Model model,
          @ModelAttribute("query") SearchRequest query,
          BindingResult bindingResult,
          HttpServletRequest request
  ) {
    Map<String, Object> params = model.asMap();

    if (!query.isInitial() && !bindingResult.hasErrors()) {
      sanitizeQuery(query);

      SearchViewer sv = new SearchViewer(query, client);

      final DateTimeZone tz = (DateTimeZone)request.getAttribute("timezone");
      SearchResponse response = sv.performSearch(tz);

      long current = System.currentTimeMillis();

      Collection<SearchItem> res = resultsService.prepareAll(Arrays.asList(response.hits().hits()));

      if (response.aggregations() != null) {
        FilterAggregationResult countFacet = response.aggregations().filter("sections");
        Terms sectionsFacet = countFacet.terms("sections");

        if (sectionsFacet.buckets().size()>1 || !Strings.isNullOrEmpty(query.getSection())) {
          params.put("sectionFacet", resultsService.buildSectionFacet(countFacet, Option.apply(Strings.emptyToNull(query.getSection()))));

          if (!Strings.isNullOrEmpty(query.getSection())) {
            Option<TermBucket> selectedSection = sectionsFacet.bucketOpt(query.getSection());

            if (!Strings.isNullOrEmpty(query.getGroup())) {
              params.put("groupFacet", resultsService.buildGroupFacet(
                      selectedSection, Option.apply(Tuple2.apply(query.getSection(), query.getGroup()))
              ));
            } else {
              params.put("groupFacet", resultsService.buildGroupFacet(selectedSection, None$.empty()));
            }

          }
        } else if (Strings.isNullOrEmpty(query.getSection()) && sectionsFacet.buckets().size()==1) {
          TermBucket onlySection = sectionsFacet.buckets().head();
          query.setSection(onlySection.key());

          params.put("groupFacet", resultsService.buildGroupFacet(Option.apply(onlySection), None$.empty()));
        }

        params.put("tags", resultsService.foundTags(response.aggregations()));
      }

      long time = System.currentTimeMillis() - current;

      params.put("result", res);
      params.put("searchTime", response.took());
      params.put("numFound", response.totalHits());

      if (response.totalHits() > query.getOffset() + SearchViewer.SearchRows()) {
        params.put("nextLink", "/search.jsp?" + query.getQuery(query.getOffset() + SearchViewer.SearchRows()));
      }

      if (query.getOffset() - SearchViewer.SearchRows() >= 0) {
        params.put("prevLink", "/search.jsp?" + query.getQuery(query.getOffset() - SearchViewer.SearchRows()));
      }

      params.put("time", time);
    }

    return "search";
  }

  private void sanitizeQuery(SearchRequest query) {
    if (!Strings.isNullOrEmpty(query.getSection())) {
      Option<Section> section = sectionService.fuzzyNameToSection().get(query.getSection());

      if (section.isDefined()) {
        query.setSection(section.get().getUrlName());

        if (!Strings.isNullOrEmpty(query.getGroup())) {
          Optional<Group> group = groupDao.getGroupOpt(section.get(), query.getGroup(), true);

          if (group.isEmpty()) {
            query.setGroup("");
          } else {
            query.setGroup(group.get().getUrlName());
          }
        } else {
          query.setGroup("");
        }
      } else {
        query.setSection("");
        query.setGroup("");
      }
    } else {
      query.setGroup("");
      query.setSection("");
    }
  }

  @InitBinder
  public void initBinder(WebDataBinder binder) {
    binder.registerCustomEditor(SearchOrder.class, new PropertyEditorSupport() {
      @Override
      public void setAsText(String s) throws IllegalArgumentException {
        switch (s) {
          case "1" ->  // for old links
                  setValue(SearchOrder.Relevance$.MODULE$);
          case "2" ->
                  setValue(SearchOrder.Date$.MODULE$);
          default ->
                  setValue(SearchOrder$.MODULE$.valueOf(s));
        }
      }
    });

    binder.registerCustomEditor(SearchInterval.class, new PropertyEditorSupport() {
      @Override
      public void setAsText(String s) throws IllegalArgumentException {
        setValue(SearchInterval.valueOf(s.toUpperCase()));
      }
    });

    binder.registerCustomEditor(SearchRange.class, new PropertyEditorSupport() {
      @Override
      public void setAsText(String s) throws IllegalArgumentException {
        setValue(SearchRange.valueOf(s.toUpperCase()));
      }
    });

    binder.registerCustomEditor(User.class, new UserPropertyEditor(userService));

    binder.setBindingErrorProcessor(new ExceptionBindingErrorProcessor());
  }
}
