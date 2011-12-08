package ru.org.linux.dto;

import java.util.List;
import java.util.Map;

/**
 * Результат работы deleteCommentsByIPAddress
 */
public class DeleteCommentDto {
  /**
   * список id удаленных топиков
   */
  private final List<Integer> deletedTopicIds;
  /**
   * список id удаленных комментариев
   */
  private final List<Integer> deletedCommentIds;
  /**
   * хэш id удаляемого топика -> строка с результатом удален или пропущен
   */
  private final Map<Integer, String> deleteInfo;

  public DeleteCommentDto(List<Integer> deletedTopicIds, List<Integer> deletedCommentIds, Map<Integer, String> deleteInfo) {
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
