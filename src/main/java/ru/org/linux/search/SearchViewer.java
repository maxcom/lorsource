/*
 * Copyright 1998-2013 Linux.org.ru
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

import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.SolrServer;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.response.QueryResponse;
import ru.org.linux.user.User;

public class SearchViewer {
  public enum SearchRange {
    ALL(null, "темы и комментарии"),
    TOPICS("is_comment:false", "только темы"),
    COMMENTS("is_comment:true", "только комментарии");

    private final String param;
    private final String title;

    SearchRange(String param, String title) {
      this.param = param;
      this.title = title;
    }

    private String getParam() {
      return param;
    }

    public String getTitle() {
      return title;
    }
  }

  public enum SearchInterval {
    MONTH("postdate:[NOW-1MONTH TO NOW]", "месяц"),
    THREE_MONTH("postdate:[NOW-3MONTH TO NOW]", "три месяца"),
    YEAR("postdate:[NOW-1YEAR TO NOW]", "год"),
    THREE_YEAR("postdate:[NOW-3YEAR TO NOW]", "три года"),
    ALL(null, "весь период");

    private final String range;
    private final String title;

    SearchInterval(String range, String title) {
      this.range = range;
      this.title = title;
    }

    private String getRange() {
      return range;
    }

    public String getTitle() {
      return title;
    }
  }

  public enum SearchOrder {
    RELEVANCE("по релевантности", "score desc"),
    DATE("по дате: от новых к старым", "postdate desc"),
    DATE_OLD_TO_NEW("по дате: от старых к новым", "postdate asc");

    private final String name;
    private final String param;

    SearchOrder(String name, String param) {
      this.name = name;
      this.param = param;
    }

    public String getName() {
      return name;
    }

    private String getParam() {
      return param;
    }
  }

  public static final int SEARCH_ROWS = 50;

  private final SearchRequest query;

  public SearchViewer(SearchRequest query) {
    this.query = query;
  }

  public QueryResponse performSearch(SolrServer search) throws SolrServerException {
    SolrQuery params = new SolrQuery();
    // set search query params
    params.set("q", query.getQ());
    params.set("rows", SEARCH_ROWS);
    params.set("start", query.getOffset());

    params.set("qt", "edismax");

    if (query.getRange().getParam()!=null) {
      params.add("fq", query.getRange().getParam());
    }

    if (query.getInterval().getRange()!=null) {
      params.add("fq", query.getInterval().getRange());
    }

    params.setFacetMinCount(1);
    params.setFacet(true);
    
    String section = query.getSection();

    if (section != null && !section.isEmpty() && !"0".equals(section)){
      params.add("fq", "{!tag=dt}section:"+query.getSection());
      params.addFacetField("{!ex=dt}section");

      params.addFacetField("{!ex=dt}group_id");
    } else {
      params.addFacetField("section");
      params.addFacetField("group_id");
    }

    if (query.getUser() != null) {
      User user = query.getUser();

      if (query.isUsertopic()) {
        params.add("fq", "topic_user_id:" + user.getId());
      } else {
        params.add("fq", "user_id:" + user.getId());
      }
    }

    if (query.getGroup()!=0) {
      params.add("fq", "{!tag=dt}group_id:" + query.getGroup());
    }

    params.set("sort", query.getSort().getParam());

    return search.query(params);
  }
}
