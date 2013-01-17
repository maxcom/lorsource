package ru.org.linux.topic;

import org.springframework.web.util.UriComponentsBuilder;
import ru.org.linux.comment.CommentFilter;

public class TopicLinkBuilder {
  private final UriComponentsBuilder builder;
  private final Topic topic;
  private final int page;

  private TopicLinkBuilder(UriComponentsBuilder builder, Topic topic, int page) {
    this.builder = builder;
    this.topic = topic;
    this.page = page;
  }

  public static TopicLinkBuilder baseLink(Topic topic) {
    return new TopicLinkBuilder(UriComponentsBuilder.fromUriString(topic.getLink()), topic, 0);
  }

  public static TopicLinkBuilder pageLink(Topic topic, int page) {
    return new TopicLinkBuilder(UriComponentsBuilder.fromUriString(topic.getLinkPage(page)), topic, page);
  }

  public TopicLinkBuilder showDeleted() {
    builder.queryParam("deleted", "true");

    return this;
  }

  public TopicLinkBuilder lastmod(int messagesPerPage) {
    if (!topic.isExpired() && topic.getPageCount(messagesPerPage) - 1 == page) {
      builder.queryParam("lastmod", topic.getLastModified().getTime());
    }

    return this;
  }

  public TopicLinkBuilder forceLastmod() {
    builder.queryParam("lastmod", topic.getLastModified().getTime());

    return this;
  }
  public TopicLinkBuilder comment(int cid) {
    builder.fragment("comment-"+cid);

    return this;
  }

  public TopicLinkBuilder filter(int filter) { // TODO: use Enum for filter
    builder.queryParam(
            "filter",
            CommentFilter.toString(filter)
    );

    return this;
  }

  public String build() {
    return builder.build().toUriString();
  }
}
