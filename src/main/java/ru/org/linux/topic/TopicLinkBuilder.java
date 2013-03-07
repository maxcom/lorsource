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

    if (!value.equals(value)) {
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
}
