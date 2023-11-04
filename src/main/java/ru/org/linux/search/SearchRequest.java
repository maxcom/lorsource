/*
 * Copyright 1998-2023 Linux.org.ru
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

import org.joda.time.DateTimeZone;
import ru.org.linux.search.SearchEnums.SearchInterval;
import ru.org.linux.search.SearchEnums.SearchRange;
import ru.org.linux.user.User;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Calendar;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;

public class SearchRequest {
  private String q = "";
  private boolean usertopic = false;
  private User user=null;
  private String section = null;
  private SearchOrder sort = SearchOrder.Relevance$.MODULE$;
  private String group;
  private SearchInterval interval = SearchInterval.ALL;
  private SearchRange range = SearchRange.ALL;
  private int offset = 0;
  private long dt;
  public long getDt() { return dt;   }
  public void setDt(long dt) { this.dt = dt; }
  public String getQ() {
    return q;
  }

  public void setQ(String q) {
    this.q = q;
  }

  public boolean isInitial() {
    return q.isEmpty() && user==null && !isDateSelected();
  }

  public boolean isUsertopic() {
    return usertopic;
  }

  public void setUsertopic(boolean usertopic) {
    this.usertopic = usertopic;
  }

  public User getUser() {
    return user;
  }

  public void setUser(User user) {
    this.user = user;
  }

  public String getSection() {
    return section;
  }

  public void setSection(String section) {
    this.section = section;
  }

  public SearchOrder getSort() {
    return sort;
  }

  public void setSort(SearchOrder sort) {
    this.sort = sort;
  }

  public String getGroup() {
    return group;
  }

  public void setGroup(String group) {
    this.group = group;
  }

  public SearchInterval getInterval() {
    return interval;
  }

  public void setInterval(SearchInterval interval) {
    this.interval = interval;
  }

  public SearchRange getRange() {
    return range;
  }

  public void setRange(SearchRange range) {
    this.range = range;
  }

  public int getOffset() {
    return offset;
  }

  public void setOffset(int offset) {
    this.offset = offset;
  }

  public String getQuery(int newOffset) {
    Map<String, String> params = new LinkedHashMap<>();

    if (q!=null && !q.isEmpty()) {
      params.put("q", q);
      params.put("oldQ", q);
    }

    if (range != SearchRange.ALL) {
      params.put("range", range.toString());
    }

    if (interval != SearchInterval.ALL) {
      params.put("interval", interval.toString());
    }

    if (user!=null) {
      params.put("user", user.getNick());
    }

    if (usertopic) {
      params.put("usertopic", "true");
    }

    if (sort!= SearchOrder.Relevance$.MODULE$) {
      params.put("sort", sort.id());
    }

    if (section!=null && !section.isEmpty()) {
      params.put("section", section);
    }

    if (group!=null) {
      params.put("group", group);
    }

    if (newOffset!=0) {
      params.put("offset", Integer.toString(newOffset));
    }

    return buildParams(params);
  }

  public boolean isDateSelected() {
    return dt >0;
  }

  public long atEndOfDaySelected(DateTimeZone tz) {
    final Calendar calendar = tz!=null? Calendar.getInstance(tz.toTimeZone()) : Calendar.getInstance();
    calendar.set(Calendar.HOUR_OF_DAY, 23);
    calendar.set(Calendar.MINUTE, 59);
    calendar.set(Calendar.SECOND, 59);
    calendar.set(Calendar.MILLISECOND, 999);
    return calendar.getTime().getTime();
  }

  public long atStartOfDaySelected(DateTimeZone tz) {
    final Calendar calendar = tz!=null? Calendar.getInstance(tz.toTimeZone()) : Calendar.getInstance();
    calendar.setTime(new Date(dt));
    calendar.set(Calendar.HOUR_OF_DAY, 0);
    calendar.set(Calendar.MINUTE, 0);
    calendar.set(Calendar.SECOND, 0);
    calendar.set(Calendar.MILLISECOND, 0);
    return calendar.getTime().getTime();
  }

  private static String buildParams(Map<String, String> params) {
    StringBuilder str = new StringBuilder();

    for (Entry<String, String> entry : params.entrySet()) {
      if (!str.isEmpty()) {
        str.append('&');
      }

      str.append(URLEncoder.encode(entry.getKey(), StandardCharsets.UTF_8));
      str.append('=');
      str.append(URLEncoder.encode(entry.getValue(), StandardCharsets.UTF_8));
    }

    return str.toString();
  }
}
