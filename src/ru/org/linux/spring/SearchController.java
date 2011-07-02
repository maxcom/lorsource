/*
 * Copyright 1998-2010 Linux.org.ru
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

package ru.org.linux.spring;

import java.beans.PropertyEditorSupport;
import java.sql.Connection;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import ru.org.linux.site.LorDataSource;
import ru.org.linux.site.SearchItem;
import ru.org.linux.site.SearchViewer;
import ru.org.linux.site.Section;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSortedMap;
import org.apache.solr.client.solrj.SolrServer;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.common.SolrDocument;
import org.apache.solr.common.SolrDocumentList;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Required;
import org.springframework.jdbc.support.JdbcUtils;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.*;

@Controller
public class SearchController {
  private SolrServer solrServer;
  private Map<Integer, String> sections;

  @Autowired
  @Required
  public void setSolrServer(SolrServer solrServer) {
    this.solrServer = solrServer;
  }

  @ModelAttribute("sections")
  public Map<Integer, String> getSections() {
    return sections;
  }

  @Autowired
  public void setSectionStore(SectionStore sectionStore) {
    ImmutableMap.Builder<Integer, String> builder = ImmutableSortedMap.naturalOrder();

    builder.put(0, "все");
    for (Section section : sectionStore.getSectionsList()) {
      builder.put(section.getId(), section.getTitle().toLowerCase());
    }

    sections = builder.build();
  }

  @ModelAttribute("sorts")
  public static Map<SearchViewer.SearchOrder, String> getSorts() {
    ImmutableMap.Builder<SearchViewer.SearchOrder, String> builder = ImmutableSortedMap.naturalOrder();

    for (SearchViewer.SearchOrder value : SearchViewer.SearchOrder.values()) {
      builder.put(value, value.getName());
    }

    return builder.build();
  }

  @SuppressWarnings({"SameReturnValue"})
  @RequestMapping(value="/search.jsp", method={RequestMethod.GET, RequestMethod.HEAD})
  public String search(
    Model model,
    @ModelAttribute("query") SearchRequest query,
    @RequestParam(value="include", required=false) String includeString,
    @RequestParam(value="date", required=false) String dateString
  ) throws Exception {
    Map<String, Object> params = model.asMap();

    boolean initial = query.isInitial();

    int include = parseInclude(includeString);

    params.put("include", include);

    SearchViewer.SearchInterval date = parseInterval(dateString);

    params.put("date", date);

    if (!initial) {
      SearchViewer sv = new SearchViewer(query);

      sv.setInterval(date);
      sv.setInclude(include);

      Connection db = null;
      try {
        long current = System.currentTimeMillis();
        db = LorDataSource.getConnection();
        QueryResponse response = sv.performSearch(solrServer, db);

        SolrDocumentList list = response.getResults();
        List<SearchItem> res = new ArrayList<SearchItem>(list.size());
        for (SolrDocument doc : list) {
          res.add(new SearchItem(db, doc));
        }

        long time = System.currentTimeMillis() - current;

        params.put("result", res);
        params.put("searchTime", response.getElapsedTime());
        params.put("numFound", list.getNumFound());

        params.put("time", time);
      } finally {
        JdbcUtils.closeConnection(db);
      }
    }

    return "search";
  }

  private static int parseInclude(String include) {
    if (include==null) {
      return SearchViewer.SEARCH_ALL;
    }

    if ("topics".equals(include)) {
      return SearchViewer.SEARCH_TOPICS;
    }

    return SearchViewer.SEARCH_ALL;
  }

  private static SearchViewer.SearchInterval parseInterval(String date) {
    if (date==null) {
      return SearchViewer.DEFAULT_INTERVAL;
    }

    if ("3month".equalsIgnoreCase(date)) {
      return SearchViewer.SearchInterval.THREE_MONTH; // support for old url's
    }

    return SearchViewer.SearchInterval.valueOf(date.toUpperCase());
  }

  @InitBinder
  public static void initBinder(WebDataBinder binder) {
    binder.registerCustomEditor(SearchViewer.SearchOrder.class, new PropertyEditorSupport() {
      @Override
      public void setAsText(String s) throws IllegalArgumentException {
        if ("1".equals(s)) { // for old links
          setValue(SearchViewer.SearchOrder.RELEVANCE);
        } else if ("2".equals(s)) {
          setValue(SearchViewer.SearchOrder.DATE);
        } else {
          setValue(SearchViewer.SearchOrder.valueOf(s));
        }
      }
    });
  }

}
