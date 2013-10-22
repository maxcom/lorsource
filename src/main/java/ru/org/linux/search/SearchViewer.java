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

import org.elasticsearch.action.search.SearchRequestBuilder;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.client.Client;
import org.elasticsearch.index.query.*;
import org.elasticsearch.search.facet.FacetBuilders;
import org.elasticsearch.search.sort.SortOrder;
import ru.org.linux.user.User;

import static org.elasticsearch.index.query.QueryBuilders.*;

public class SearchViewer {
  public enum SearchRange {
    ALL(null, "темы и комментарии"),
    TOPICS("false", "только темы"),
    COMMENTS("true", "только комментарии");

    private final String param;
    private final String title;

    SearchRange(String param, String title) {
      this.param = param;
      this.title = title;
    }

    private String getValue() {
      return param;
    }

    private String getColumn() {
      return "is_comment";
    }

    public String getTitle() {
      return title;
    }
  }

  public enum SearchInterval {
    MONTH("[NOW-1MONTH TO NOW]", "месяц"),
    THREE_MONTH("[NOW-3MONTH TO NOW]", "три месяца"),
    YEAR("[NOW-1YEAR TO NOW]", "год"),
    THREE_YEAR("[NOW-3YEAR TO NOW]", "три года"),
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

    private String getColumn() {
      return "postdate";
    }
  }

  public enum SearchOrder {
    RELEVANCE("по релевантности", "_score", SortOrder.DESC),
    DATE("по дате: от новых к старым", "postdate", SortOrder.DESC),
    DATE_OLD_TO_NEW("по дате: от старых к новым", "postdate", SortOrder.ASC);

    private final String name;
    private final String param;
    private final SortOrder order;

    SearchOrder(String name, String param, SortOrder order) {
      this.name = name;
      this.param = param;
      this.order = order;
    }

    public String getName() {
      return name;
    }

    private String getColumn() {
      return param;
    }
  }

  public static final int SEARCH_ROWS = 50;

  private final SearchRequest query;

  public SearchViewer(SearchRequest query) {
    this.query = query;
  }

  public SearchResponse performSearch(Client client) {
    SearchRequestBuilder request = client.prepareSearch(SearchQueueListener.MESSAGES_INDEX);

    request.setTypes(SearchQueueListener.MESSAGES_TYPE);

    request.addFields("title", "topic_title", "user_id", "postdate", "topic_id", "section");

    QueryStringQueryBuilder esQuery = queryString(this.query.getQ());
    esQuery.lenient(true);

    request.setSize(SEARCH_ROWS);
    request.setFrom(this.query.getOffset());

    BoolQueryBuilder rootQuery = boolQuery();

    rootQuery.must(esQuery);

    if (this.query.getRange().getValue()!=null) {
      rootQuery.must(termQuery(query.getRange().getColumn(), query.getRange().getValue()));
    }

    if (this.query.getInterval().getRange()!=null) {
      RangeQueryBuilder rangeQuery = QueryBuilders.rangeQuery(query.getInterval().getColumn());

      rangeQuery.from(this.query.getInterval().getRange());
    }

    if (this.query.getUser() != null) {
      User user = this.query.getUser();

      if (this.query.isUsertopic()) {
        rootQuery.must(termQuery("topic_user_id", user.getId()));
      } else {
        rootQuery.must(termQuery("user_id", user.getId()));
      }
    }

    request.setQuery(rootQuery);

    String section = this.query.getSection();

    if (section != null && !section.isEmpty() && !"0".equals(section)){
      request.setFilter(FilterBuilders.termFilter("section", this.query.getSection()));
    }

    request.addFacet(FacetBuilders.termsFacet("sections").field("section"));
    request.addFacet(FacetBuilders.termsFacet("groups").field("group_id"));

    if (this.query.getGroup()!=0) {
      // overrides section filter!
      request.setFilter(FilterBuilders.termFilter("group_id", this.query.getGroup()));
    }

    request.addSort(query.getSort().getColumn(), query.getSort().order);

    // TODO use Async
    return request.execute().actionGet();
  }
}
