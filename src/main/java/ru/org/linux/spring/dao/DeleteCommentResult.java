package ru.org.linux.spring.dao;

import java.util.List;
import java.util.Map;

/**
 * Результат работы deleteCommentsByIPAddress
 */
public class DeleteCommentResult {
  private final List<Integer> deletedTopicIds;
  private final List<Integer> deletedCommentIds;
  private final Map<Integer, String> deleteInfo;

  DeleteCommentResult(List<Integer> deletedTopicIds, List<Integer> deletedCommentIds, Map<Integer, String> deleteInfo) {
    this.deletedCommentIds = deletedCommentIds;
    this.deletedTopicIds = deletedTopicIds;
    this.deleteInfo = deleteInfo;
  }

  public List<Integer> getDeletedTopicIds() {
    return deletedTopicIds;
  }

  public List<Integer> getDeletedCommentIds() {
    return deletedCommentIds;
  }

  public Map<Integer, String> getDeleteInfo() {
    return deleteInfo;
  }
}
