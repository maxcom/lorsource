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
import java.util.ArrayList;
import java.util.List;

import org.apache.solr.client.solrj.SolrServer;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.common.params.ModifiableSolrParams;

public class SearchViewer {
  public static final int SEARCH_TOPICS = 1;
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

  public static final int SORT_R = 1;
  public static final int SORT_DATE = 2;

  private final String query;
  private int include = SEARCH_ALL;
  private SearchInterval interval = DEFAULT_INTERVAL;
  private int section = 0;
  private int sort = SORT_R;

  private String username = "";
  private boolean userTopic = false;

  public SearchViewer(String query) {
    this.query = query;
  }

  public QueryResponse performSearch(SolrServer search, Connection db) throws SQLException, UserErrorException, SolrServerException {
    ModifiableSolrParams params = new ModifiableSolrParams();
    List<SearchItem> items = new ArrayList<SearchItem>();
    // set search query params
    params.set("q", query);
    params.set("rows", 100);
    if(include != SEARCH_ALL){
      params.add("fq","is_comment:false");      
    }

    if (interval.getRange()!=null) {
      params.add("fq", interval.getRange());
    }

    if (section != 0 ){
      params.add("fq","section_id:"+section);
    }

    if(username.length() > 0) {
      try {
        User user = User.getUser(db, username);
        if (userTopic) {
          params.add("fq","topic_user_id:"+user.getId());
        } else {
          params.add("fq","user_id:"+user.getId());
        }
      } catch (UserNotFoundException ex) {
        throw new UserErrorException("User not found: "+username);
      }
    }
    if(sort == SORT_DATE){
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

  public void setSection(int section) {
    this.section = section;
  }

  public void setSort(int sort) {
    this.sort = sort;
  }

  public void setUser(String username) {
    this.username = username;
  }

  public void setUserTopic(boolean userTopic) {
    this.userTopic = userTopic;
  }
}
