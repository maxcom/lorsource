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

package ru.org.linux.topic;

import org.springframework.web.util.UriComponentsBuilder;
import ru.org.linux.comment.CommentFilter;

public class TopicLinkBuilder {
  private final Topic topic;
  private final int page;
  private final boolean showDeleted;
  private final boolean lastmod;
  private final Integer comment;
  private final String filter;

  private TopicLinkBuilder(
          Topic topic,
          int page,
          boolean showDeleted,
          boolean lastmod,
          Integer comment,
          String filter
  ) {
    this.topic = topic;
    this.page = page;
    this.showDeleted = showDeleted;
    this.lastmod = lastmod;
    this.comment = comment;
    this.filter = filter;
  }

  public static TopicLinkBuilder baseLink(Topic topic) {
    return new TopicLinkBuilder(topic, 0, false, false, null, null);
  }

  public static TopicLinkBuilder pageLink(Topic topic, int page) {
    return new TopicLinkBuilder(topic, page, false, false, null, null);
  }

  public TopicLinkBuilder showDeleted() {
    if (!showDeleted) {
      return new TopicLinkBuilder(topic, page, true, lastmod, comment, filter);
    } else {
      return this;
    }
  }

  public TopicLinkBuilder lastmod(int messagesPerPage) {
    if (!lastmod && !topic.isExpired() && topic.getPageCount(messagesPerPage) - 1 == page) {
      return forceLastmod();
    } else {
      return this;
    }
  }

  public TopicLinkBuilder forceLastmod() {
    if (!lastmod) {
      return new TopicLinkBuilder(topic, page, showDeleted, true, comment, filter);
    } else {
      return this;
    }
  }

  public TopicLinkBuilder comment(int cid) {
    if (comment==null || comment!=cid) {
      return new TopicLinkBuilder(topic, page, showDeleted, lastmod, cid, filter);
    } else {
      return this;
    }
  }

  public TopicLinkBuilder filter(int filter) { // TODO: use Enum for filter
    String value = CommentFilter.toString(filter);

    if (!value.equals(this.filter)) {
      return new TopicLinkBuilder(topic, page, showDeleted, lastmod, comment, value);
    } else {
      return this;
    }
  }

  public String build() {
    UriComponentsBuilder builder = UriComponentsBuilder.fromUriString(topic.getLinkPage(page));

    if (showDeleted) {
      builder.queryParam("deleted", "true");
    }

    if (lastmod) {
      builder.queryParam("lastmod", topic.getLastModified().getTime());
    }

    if (comment!=null) {
      builder.fragment("comment-"+comment);
    }

    if (filter!=null) {
      builder.queryParam("filter", filter);
    }

    return builder.build().toUriString();
  }

  public TopicLinkBuilder page(int page) {
    if (page!=this.page) {
      return new TopicLinkBuilder(topic, page, showDeleted, lastmod, comment, filter);
    } else {
      return this;
    }
  }
}
