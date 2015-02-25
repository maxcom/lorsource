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

import com.google.common.base.Strings;
import org.elasticsearch.action.search.SearchRequestBuilder;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.client.Client;
import org.elasticsearch.index.query.*;
import org.elasticsearch.index.query.functionscore.FunctionScoreQueryBuilder;
import org.elasticsearch.index.query.functionscore.ScoreFunctionBuilders;
import org.elasticsearch.search.highlight.HighlightBuilder;
import org.elasticsearch.search.sort.SortOrder;
import ru.org.linux.user.User;

import java.util.ArrayList;
import java.util.List;

import static org.elasticsearch.index.query.FilterBuilders.*;
import static org.elasticsearch.index.query.QueryBuilders.*;
import static org.elasticsearch.search.aggregations.AggregationBuilders.*;

public class SearchViewer {
  public static final int MESSAGE_FRAGMENT = 500;

  private static final int TOPIC_BOOST = 3;
  private static final int RECENT_BOOST = 2;

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
    MONTH("now/h-1M", "месяц"),
    THREE_MONTH("now/d-3M", "три месяца"),
    YEAR("now/d-1y", "год"),
    THREE_YEAR("now/w-3y", "три года"),
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

  public static final int SEARCH_ROWS = 25;

  private final SearchRequest query;

  public SearchViewer(SearchRequest query) {
    this.query = query;
  }

  private QueryBuilder processQueryString(String queryText) {
    if (queryText.isEmpty()) {
      return matchAllQuery();
    } else {
      CommonTermsQueryBuilder esQuery = commonTerms("_all", queryText);
      esQuery.lowFreqMinimumShouldMatch("2");

      MatchQueryBuilder phraseQuery = matchPhraseQuery("_all", queryText);
      phraseQuery.setLenient(true);

      BoolQueryBuilder boolQuery = boolQuery();
      boolQuery.should(esQuery);
      boolQuery.should(phraseQuery);

      return boolQuery;
    }
  }

  private QueryBuilder boost(QueryBuilder query) {
    FunctionScoreQueryBuilder booster = functionScoreQuery(query);

    booster.add(termFilter("is_comment", "false"), ScoreFunctionBuilders.weightFactorFunction(TOPIC_BOOST));
    booster.add(rangeFilter("postdate").gte("now/d-3y"), ScoreFunctionBuilders.weightFactorFunction(RECENT_BOOST));

    return booster;
  }

  public SearchResponse performSearch(Client client) {
    SearchRequestBuilder request = client.prepareSearch(SearchQueueListener.MESSAGES_INDEX);

    request.setTypes(SearchQueueListener.MESSAGES_TYPE);

    request.addFields(
            "title",
            "topic_title",
            "author",
            "postdate",
            "topic_id",
            "section",
            "message",
            "group",
            "is_comment",
            "tag"
    );

    QueryBuilder esQuery = processQueryString(this.query.getQ());

    request.setSize(SEARCH_ROWS);
    request.setFrom(this.query.getOffset());

    BoolFilterBuilder queryFilter = boolFilter();

    if (this.query.getRange().getValue()!=null) {
      queryFilter.must(termFilter(query.getRange().getColumn(), query.getRange().getValue()));
    }

    if (this.query.getInterval().getRange()!=null) {
      RangeFilterBuilder dateFilter = rangeFilter(query.getInterval().getColumn());

      dateFilter.from(this.query.getInterval().getRange());

      queryFilter.must(dateFilter);
    }

    if (this.query.getUser() != null) {
      User user = this.query.getUser();

      if (this.query.isUsertopic()) {
        queryFilter.must(termFilter("topic_author", user.getNick()));
      } else {
        queryFilter.must(termFilter("author", user.getNick()));
      }
    }

    if (!queryFilter.hasClauses()) {
      request.setQuery(boost(esQuery));
    } else {
      QueryBuilder rootQuery = filteredQuery(esQuery, queryFilter);

      request.setQuery(boost(rootQuery));
    }

    String section = this.query.getSection();
    List<FilterBuilder> postFilters = new ArrayList<>();

    if (!Strings.isNullOrEmpty(section)) {
      postFilters.add(termFilter("section", this.query.getSection()));
    }

    request.addAggregation(
            filter("sections").subAggregation(
                    terms("sections").field("section").size(0).subAggregation(
                            terms("groups").field("group").size(0)
                    )
            ).filter(matchAllFilter())
    );

    request.addAggregation(significantTerms("tags").field("tag").minDocCount(30));

    if (!Strings.isNullOrEmpty(this.query.getGroup())) {
      postFilters.add(termFilter("group", this.query.getGroup()));
    }

    if (!postFilters.isEmpty()) {
      request.setPostFilter(andFilters(postFilters));
    }

    request.addSort(query.getSort().getColumn(), query.getSort().order);

    setupHighlight(request);

    // TODO use Async
    return request.execute().actionGet();
  }

  private FilterBuilder andFilters(List<FilterBuilder> filters) {
    if (filters.size()==1) {
      return filters.get(0);
    } else {
      BoolFilterBuilder postRoot = boolFilter();

      for (FilterBuilder filter : filters) {
        postRoot.must(filter);
      }

      return postRoot;
    }
  }

  private void setupHighlight(SearchRequestBuilder request) {
    HighlightBuilder.Field title = new HighlightBuilder.Field("title");
    title.numOfFragments(0);
    request.addHighlightedField(title);

    HighlightBuilder.Field topicTitle = new HighlightBuilder.Field("topic_title");
    topicTitle.numOfFragments(0);
    request.addHighlightedField(topicTitle);

    HighlightBuilder.Field message = new HighlightBuilder.Field("message");
    message.numOfFragments(1);
    message.fragmentSize(MESSAGE_FRAGMENT);
    request.addHighlightedField(message);

    request.setHighlighterEncoder("html");
    request.setHighlighterPreTags("<em class=search-hl>");
    request.setHighlighterPostTags("</em>");
  }
}
