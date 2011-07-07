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

import ru.org.linux.site.SearchViewer;

public class SearchRequest {
  private String q = "";
  private String oldQ = "";
  private boolean usertopic = false;
  private String username="";
  private int section = 0;
  private SearchViewer.SearchOrder sort = SearchViewer.SearchOrder.RELEVANCE;
  private int group = 0;
  private SearchViewer.SearchInterval interval = SearchViewer.SearchInterval.ALL;

  public String getQ() {
    return q;
  }

  public void setQ(String q) {
    this.q = q;
  }

  public boolean isInitial() {
    return q.isEmpty();
  }

  public boolean isUsertopic() {
    return usertopic;
  }

  public void setUsertopic(boolean usertopic) {
    this.usertopic = usertopic;
  }

  public String getUsername() {
    return username;
  }

  public void setUsername(String username) {
    this.username = username;
  }

  public int getSection() {
    return section;
  }

  public void setSection(int section) {
    this.section = section;
  }

  public SearchViewer.SearchOrder getSort() {
    return sort;
  }

  public void setSort(SearchViewer.SearchOrder sort) {
    this.sort = sort;
  }

  public String getOldQ() {
    return oldQ;
  }

  public void setOldQ(String oldQ) {
    this.oldQ = oldQ;
  }

  public int getGroup() {
    return group;
  }

  public void setGroup(int group) {
    this.group = group;
  }

  public SearchViewer.SearchInterval getInterval() {
    return interval;
  }

  public void setInterval(SearchViewer.SearchInterval interval) {
    this.interval = interval;
  }
}
