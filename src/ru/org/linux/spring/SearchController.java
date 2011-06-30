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

import java.sql.Connection;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import ru.org.linux.site.LorDataSource;
import ru.org.linux.site.SearchItem;
import ru.org.linux.site.SearchViewer;

import org.apache.solr.client.solrj.SolrServer;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.common.SolrDocument;
import org.apache.solr.common.SolrDocumentList;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Required;
import org.springframework.jdbc.support.JdbcUtils;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
public class SearchController {
  private SolrServer solrServer;

  @Autowired
  @Required
  public void setSolrServer(SolrServer solrServer) {
    this.solrServer = solrServer;
  }

  @SuppressWarnings({"SameReturnValue"})
  @RequestMapping(value="/search.jsp", method={RequestMethod.GET, RequestMethod.HEAD})
  public String search(
    Model model,
    @ModelAttribute("query") SearchRequest query,
    @RequestParam(value="include", required=false) String includeString,
    @RequestParam(value="date", required=false) String dateString,
    @RequestParam(value="section", required=false) Integer section,
    @RequestParam(value="sort", required=false) Integer sort
  ) throws Exception {
    Map<String, Object> params = model.asMap();

    boolean initial = query.isInitial();

    int include = parseInclude(includeString);

    params.put("include", include);

    SearchViewer.SearchInterval date = parseInterval(dateString);

    params.put("date", date);

    if (section==null) {
      section = 0;
    }

    params.put("section", section);

    if (sort==null) {
      sort = SearchViewer.SORT_R;
    }

    params.put("sort", sort);

    if (!initial) {
      SearchViewer sv = new SearchViewer(query);

      sv.setInterval(date);
      sv.setInclude(include);
      sv.setSection(section);
      sv.setSort(sort);

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

  public static int parseInclude(String include) {
    if (include==null) {
      return SearchViewer.SEARCH_ALL;
    }

    if ("topics".equals(include)) {
      return SearchViewer.SEARCH_TOPICS;
    }

    return SearchViewer.SEARCH_ALL;
  }

  public static SearchViewer.SearchInterval parseInterval(String date) {
    if (date==null) {
      return SearchViewer.DEFAULT_INTERVAL;
    }

    if ("3month".equalsIgnoreCase(date)) {
      return SearchViewer.SearchInterval.THREE_MONTH; // support for old url's
    }

    return SearchViewer.SearchInterval.valueOf(date.toUpperCase());
  }
}
