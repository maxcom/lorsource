/*
 * Copyright 1998-2014 Linux.org.ru
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

import com.google.common.base.Optional;
import com.google.common.base.Strings;
import com.google.common.collect.ImmutableMap.Builder;
import com.google.common.collect.ImmutableSortedMap;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.client.Client;
import org.elasticsearch.search.aggregations.bucket.filter.Filter;
import org.elasticsearch.search.aggregations.bucket.terms.Terms;
import org.springframework.beans.factory.annotation.Autowired;
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
import ru.org.linux.search.SearchViewer.SearchInterval;
import ru.org.linux.search.SearchViewer.SearchOrder;
import ru.org.linux.search.SearchViewer.SearchRange;
import ru.org.linux.section.Section;
import ru.org.linux.section.SectionService;
import ru.org.linux.user.User;
import ru.org.linux.user.UserDao;
import ru.org.linux.user.UserPropertyEditor;
import ru.org.linux.util.ExceptionBindingErrorProcessor;
import scala.Option;

import java.beans.PropertyEditorSupport;
import java.util.Collection;
import java.util.Map;

@Controller
public class SearchController {
  @Autowired
  private SectionService sectionService;

  @Autowired
  private UserDao userDao;

  @Autowired
  private GroupDao groupDao;

  @Autowired
  private Client client;

  @Autowired
  private SearchResultsService resultsService;

  @ModelAttribute("sorts")
  public static Map<SearchOrder, String> getSorts() {
    Builder<SearchOrder, String> builder = ImmutableSortedMap.naturalOrder();

    for (SearchOrder value : SearchOrder.values()) {
      builder.put(value, value.getName());
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
          BindingResult bindingResult
  ) throws Exception {
    Map<String, Object> params = model.asMap();

    if (!query.isInitial() && !bindingResult.hasErrors()) {
      sanitizeQuery(query);

      SearchViewer sv = new SearchViewer(query);

      SearchResponse response = sv.performSearch(client);

      long current = System.currentTimeMillis();

      Collection<SearchItem> res = resultsService.prepareAll(response.getHits());

      if (response.getAggregations() != null) {
        Filter countFacet = response.getAggregations().get("sections");
        Terms sectionsFacet = countFacet.getAggregations().get("sections");

        if (sectionsFacet.getBuckets().size()>1) {
          params.put("sectionFacet", resultsService.buildSectionFacet(countFacet));

          if (!Strings.isNullOrEmpty(query.getSection()) && sectionsFacet.getBucketByKey(query.getSection())!=null) {
            Terms.Bucket selectedSection = sectionsFacet.getBucketByKey(query.getSection());

            params.put("groupFacet", resultsService.buildGroupFacet(selectedSection));
          }
        } else if (Strings.isNullOrEmpty(query.getSection()) && sectionsFacet.getBuckets().size()==1) {
          Terms.Bucket onlySection = sectionsFacet.getBuckets().iterator().next();
          query.setSection(onlySection.getKey());

          params.put("groupFacet", resultsService.buildGroupFacet(onlySection));
        }

        params.put("tags", resultsService.foundTags(response.getAggregations()));
      }

      long time = System.currentTimeMillis() - current;

      params.put("result", res);
      params.put("searchTime", response.getTookInMillis());
      params.put("numFound", response.getHits().getTotalHits());

      if (response.getHits().getTotalHits() > query.getOffset() + SearchViewer.SEARCH_ROWS) {
        params.put("nextLink", "/search.jsp?" + query.getQuery(query.getOffset() + SearchViewer.SEARCH_ROWS));
      }

      if (query.getOffset() - SearchViewer.SEARCH_ROWS >= 0) {
        params.put("prevLink", "/search.jsp?" + query.getQuery(query.getOffset() - SearchViewer.SEARCH_ROWS));
      }

      params.put("time", time);
    }

    return "search";
  }

  private void sanitizeQuery(SearchRequest query) {
    if (!query.getQ().equals(query.getOldQ())) {
      query.setSection(null);
      query.setGroup(null);
    }

    query.setOldQ(query.getQ());

    if (!Strings.isNullOrEmpty(query.getSection())) {
      Option<Section> section = sectionService.fuzzyNameToSection().get(query.getSection());

      if (section.isDefined()) {
        query.setSection(section.get().getUrlName());

        if (!Strings.isNullOrEmpty(query.getGroup())) {
          Optional<Group> group = groupDao.getGroupOpt(section.get(), query.getGroup(), true);

          if (!group.isPresent()) {
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
          case "1":  // for old links
            setValue(SearchOrder.RELEVANCE);
            break;
          case "2":
            setValue(SearchOrder.DATE);
            break;
          default:
            setValue(SearchOrder.valueOf(s));
            break;
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

    binder.registerCustomEditor(User.class, new UserPropertyEditor(userDao));

    binder.setBindingErrorProcessor(new ExceptionBindingErrorProcessor());
  }
}
