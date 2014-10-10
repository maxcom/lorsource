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
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableMap.Builder;
import com.google.common.collect.ImmutableSortedMap;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.client.Client;
import org.elasticsearch.search.facet.terms.TermsFacet;
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

    boolean initial = query.isInitial();

    if (!initial && !bindingResult.hasErrors()) {
      if (query.getQ().trim().isEmpty()) {
        return "redirect:/search.jsp";
      }

      sanitizeQuery(query);

      SearchViewer sv = new SearchViewer(query);

      SearchResponse response = sv.performSearch(client);

      long current = System.currentTimeMillis();

      Collection<SearchItem> res = resultsService.prepareAll(response.getHits());

      if (response.getFacets() != null) {
        TermsFacet sectionFacet = (TermsFacet) response.getFacets().facetsAsMap().get("sections");

        if (sectionFacet != null && sectionFacet.getEntries().size() > 1) {
          params.put("sectionFacet", buildSectionFacet(sectionFacet));
          if (query.getSection()==null) {
            query.setSection("");
          }
        } else if (sectionFacet!=null && sectionFacet.getEntries().size() == 1) {
          query.setSection(sectionFacet.getEntries().get(0).getTerm().toString());
        }

        TermsFacet groupFacet = (TermsFacet) response.getFacets().facetsAsMap().get("groups");

        if (groupFacet != null && groupFacet.getEntries().size() > 1) {
          params.put("groupFacet", buildGroupFacet(query.getSection(), groupFacet));
          if (query.getGroup()==null) {
            query.setGroup("");
          }
        }
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
            query.setGroup(null);
          } else {
            query.setGroup(group.get().getUrlName());
          }
        } else {
          query.setGroup(null);
        }
      } else {
        query.setSection(null);
        query.setGroup(null);
      }
    } else {
      query.setGroup(null);
      query.setSection(null);
    }
  }

  private Map<String, String> buildSectionFacet(TermsFacet sectionFacet) {
    Builder<String, String> builder = ImmutableSortedMap.naturalOrder();

    for (TermsFacet.Entry entry : sectionFacet) {
      if("wiki".equals(entry.getTerm())) {
        builder.put(entry.getTerm().string(), entry.getTerm() + " (" + entry.getCount() + ')');
      } else {
        String urlName = entry.getTerm().string();
        String name = sectionService.getSectionByName(urlName).getName().toLowerCase();
        builder.put(entry.getTerm().string(), name + " (" + entry.getCount() + ')');
      }
    }

    builder.put("", "все (" + Long.toString(sectionFacet.getTotalCount()) + ')');

    return builder.build();
  }

  private Map<String, String> buildGroupFacet(String sectionName, TermsFacet groupFacet) {
    Builder<String, String> builder = ImmutableSortedMap.naturalOrder();
    if (sectionName == null || sectionName.isEmpty() || "wiki".equals(sectionName)) {
      return null;
    }

    Section section = sectionService.getSectionByName(sectionName);

    for (TermsFacet.Entry entry : groupFacet) {
      if("0".equals(entry.getTerm().toString())) {
        continue;
      }

      String groupUrlName = entry.getTerm().toString();

      Group group = groupDao.getGroup(section, groupUrlName);

      String name = group.getTitle().toLowerCase();

      builder.put(groupUrlName, name + " (" + entry.getCount() + ')');
    }

    builder.put("", "все (" + Long.toString(groupFacet.getTotalCount()) + ')');

    ImmutableMap<String, String> r = builder.build();

    if (r.size() <= 2) {
      return null;
    } else {
      return r;
    }
  }

  @InitBinder
  public void initBinder(WebDataBinder binder) {
    binder.registerCustomEditor(SearchOrder.class, new PropertyEditorSupport() {
      @Override
      public void setAsText(String s) throws IllegalArgumentException {
        if ("1".equals(s)) { // for old links
          setValue(SearchOrder.RELEVANCE);
        } else if ("2".equals(s)) {
          setValue(SearchOrder.DATE);
        } else {
          setValue(SearchOrder.valueOf(s));
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
