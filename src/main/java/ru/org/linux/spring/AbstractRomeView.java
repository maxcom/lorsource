/*
 * Copyright 1998-2024 Linux.org.ru
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

import com.google.common.base.Strings;
import com.rometools.rome.feed.synd.SyndFeed;
import com.rometools.rome.feed.synd.SyndFeedImpl;
import com.rometools.rome.io.SyndFeedOutput;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.servlet.view.AbstractView;

import java.util.Map;

public abstract class AbstractRomeView extends AbstractView {
  private Map<String,String> contentTypes;
  private Map<String,String> feedTypes;
  private Integer defaultCount;
  private String defaultType;
  private Integer minimalCount;
  private Integer maximalCount;

  public Map<String, String> getContentTypes() {
    return contentTypes;
  }

  public void setContentTypes(Map<String, String> contentTypes) {
    this.contentTypes = contentTypes;
  }

  public Map<String, String> getFeedTypes() {
    return feedTypes;
  }

  public void setFeedTypes(Map<String, String> feedTypes) {
    this.feedTypes = feedTypes;
  }

  public Integer getDefaultCount() {
    return defaultCount;
  }

  public void setDefaultCount(Integer defaultCount) {
    this.defaultCount = defaultCount;
  }

  @Override
  protected void renderMergedOutputModel(Map model, HttpServletRequest request,
                                         HttpServletResponse response) throws Exception {
    SyndFeed feed = new SyndFeedImpl();
    feed.setEncoding("utf-8");
    String feedType = (String) model.get("feed-type");
    if (Strings.isNullOrEmpty(feedType)){
      feedType = "rss";
    }
    feed.setFeedType(feedTypes.get(feedType));
    createFeed(feed, model);
    response.setContentType(contentTypes.get(feedType));
    response.setCharacterEncoding("UTF-8");
    SyndFeedOutput output = new SyndFeedOutput();
    output.output(feed, response.getWriter());
  }

  protected abstract void createFeed(SyndFeed feed, Map model);

  public Integer getMaximalCount() {
    return maximalCount;
  }

  public void setMaximalCount(Integer maximalCount) {
    this.maximalCount = maximalCount;
  }

  public Integer getMinimalCount() {
    return minimalCount;
  }

  public void setMinimalCount(Integer minimalCount) {
    this.minimalCount = minimalCount;
  }

  public String getDefaultType() {
    return defaultType;
  }

  public void setDefaultType(String defaultType) {
    this.defaultType = defaultType;
  }
}
