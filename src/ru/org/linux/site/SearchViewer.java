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

import com.google.common.collect.ImmutableList;
import org.apache.solr.client.solrj.SolrServer;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.common.SolrDocument;
import org.apache.solr.common.SolrDocumentList;
import org.apache.solr.common.params.ModifiableSolrParams;

public class SearchViewer {
  public static final int SEARCH_TOPICS = 1;
  public static final int SEARCH_ALL = 0;

  public static final int SEARCH_3MONTH = 1;
  public static final int SEARCH_YEAR = 2;

  public static final int SORT_R = 1;
  public static final int SORT_DATE = 2;

  private final String query;
  private int include = SEARCH_ALL;
  private int date = SEARCH_ALL;
  private int section = 0;
  private int sort = SORT_R;

  private String username = "";
  private boolean userTopic = false;

  public SearchViewer(String query) {
    this.query = query;
  }

  public List<SearchItem> show(Connection db) throws SQLException, UserErrorException, SolrServerException {
    SolrServer search = LorSearchSource.getConnection();
    ModifiableSolrParams params = new ModifiableSolrParams();
    List<SearchItem> items = new ArrayList<SearchItem>();
    // set search query params
    params.set("q", query);
    params.set("q.op", "AND");
    params.set("rows", 100);
    params.set("qt", "dismax");
    if(include != SEARCH_ALL){
      params.add("fq","is_comment:false");      
    }
    if(date == SEARCH_3MONTH){
      params.add("fq","postdate:[NOW-3MONTH TO NOW]");
    }else if (date == SEARCH_YEAR){
      params.add("fq","postdate:[NOW-1YEAR TO NOW]");
    }
    if (section != 0 ){
      params.add("fq","section_id:"+section);
    }
    if(username.length() > 0) {
      try {
        User user = User.getUser(db, username);
        if (userTopic) {
          params.add("fq","user_id:"+user.getId());

        }
      } catch (UserNotFoundException ex) {
        throw new UserErrorException("User not found: "+username);
      }
    }
    if(sort == SORT_DATE){
      params.set("sort:postdate desc");
    }
    params.set("rows:100"); // maximum number of documents from the complete result set to return to the client

    // send search query to solr
    QueryResponse response = search.query(params);
    SolrDocumentList list = response.getResults();
    for (SolrDocument doc : list) {
      items.add(new SearchItem(db, doc));
    }

    return ImmutableList.copyOf(items);
  }

  public void setInclude(int include) {
    this.include = include;
  }

  public void setDate(int date) {
    this.date = date;
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
