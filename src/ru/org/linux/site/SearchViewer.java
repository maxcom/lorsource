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

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import org.apache.solr.client.solrj.SolrServer;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.impl.CommonsHttpSolrServer;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.common.params.ModifiableSolrParams;
import org.apache.solr.common.SolrDocument;
import org.apache.solr.common.SolrDocumentList;
import org.apache.solr.common.util.SimpleOrderedMap;
import java.net.MalformedURLException;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.commons.logging.Log;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import org.apache.commons.logging.LogFactory; 

import com.google.common.collect.ImmutableList;

public class SearchViewer {
  private static final Log logger = LogFactory.getLog(SearchViewer.class);
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

  public List<SearchItem> show(Connection db) throws SQLException, UserErrorException{
    QueryResponse response;
    SolrServer search = LorSearchSource.getConnection();
    ModifiableSolrParams params = new ModifiableSolrParams();
    List<SearchItem> items = new ArrayList<SearchItem>();
    // set search query params
    params.set("q", query);
    params.set("q.op", "AND");
    params.set("rows", 100);
    params.set("defType", "dismax");
    params.set("qf", "message title");
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
    try{
      response = search.query(params);
      SolrDocumentList list = response.getResults();
      for (SolrDocument doc : list) {
          items.add(new SearchItem(db, doc));
      }
    }catch(SolrServerException ex){
      logger.error("Error search:"+ex.toString());
    }

    return ImmutableList.copyOf(items);
  }

  public String getVariantID() {
    try {
      return "search?q="+ URLEncoder.encode(query, "koi8-r")+"&include="+include+"&date="+date+"&section="+section+"&sort="+sort+"&username="+URLEncoder.encode(username);
    } catch (UnsupportedEncodingException e) {
      throw new RuntimeException(e);
    }
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
