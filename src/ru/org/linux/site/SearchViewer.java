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

package ru.org.linux.site;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.Collection;
import java.util.Set;

import ru.org.linux.spring.SearchRequest;

import com.google.common.collect.ImmutableSet;
import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.SolrServer;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.common.params.ModifiableSolrParams;

public class SearchViewer {
  public static final int SEARCH_TOPICS = 1;
  public static final int SEARCH_COMMENTS = 2;
  public static final int SEARCH_ALL = 0;

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

    String getRange() {
      return range;
    }

    public String getTitle() {
      return title;
    }
  }

  public static final SearchInterval DEFAULT_INTERVAL = SearchInterval.THREE_YEAR;
  public static final int SEARCH_ROWS = 100;

  public static final int SORT_R = 1;
  public static final int SORT_DATE = 2;

  private int include = SEARCH_ALL;
  private SearchInterval interval = DEFAULT_INTERVAL;
  private int sort = SORT_R;
  private int offset = 0;
  private Set<Integer> groups = ImmutableSet.of();

  private final SearchRequest query;

  public SearchViewer(SearchRequest query) {
    this.query = query;
  }

  public QueryResponse performSearch(SolrServer search, Connection db) throws SQLException, UserErrorException, SolrServerException {
    SolrQuery params = new SolrQuery();
    // set search query params
    params.set("q", query.getQ());
    params.set("rows", SEARCH_ROWS);
    params.set("start", offset);

    if (query.isIgnoreTitle()){
      params.set("qt", "dismax-message");
    }else{
      params.set("qt", "dismax");
    }

    if(include == SEARCH_TOPICS){
      params.add("fq","is_comment:false");      
    }else if(include == SEARCH_COMMENTS){
      params.add("fq","is_comment:true");
    }

    if (interval.getRange()!=null) {
      params.add("fq", interval.getRange());
    }

    if (query.getSection() != 0 ){
      params.add("fq", "section_id:"+query.getSection());
    } else {
      params.addFacetField("section_id");
      params.setFacet(true);
    }

    String username = query.getUsername();

    if(username != null && username.length() > 0) {
      try {
        User user = User.getUser(db, username);
        if (query.isUsertopic()) {
          params.add("fq","topic_user_id:"+user.getId());
        } else {
          params.add("fq","user_id:"+user.getId());
        }
      } catch (UserNotFoundException ex) {
        throw new UserErrorException("User not found: "+username);
      }
    }

    if (!groups.isEmpty()) {
      if (groups.size() == 1) {
        params.add("fq", "group_id:" + groups.iterator().next());
      } else {
        StringBuilder buffer = new StringBuilder("group_id:(");

        for (int gId : groups) {
          if (buffer.length() > 0) {
            buffer.append(" OR ");
          }

          buffer.append(Integer.toString(gId));
        }

        buffer.append(')');

        params.add("fq", buffer.toString());
      }
    }

    if(sort == SORT_DATE) {
      params.set("sort","postdate desc");
    }

    return search.query(params);
  }

  public void setInclude(int include) {
    this.include = include;
  }

  public void setInterval(SearchInterval interval) {
    this.interval = interval;
  }

  public void setSort(int sort) {
    this.sort = sort;
  }

  public void setOffset(int offset) {
    this.offset = offset;
  }

  public void setGroups(Collection<Integer> groups){
    this.groups = ImmutableSet.copyOf(groups);
  }
}
