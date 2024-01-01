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

package ru.org.linux.comment;

import java.util.List;
import java.util.Map;

/**
 * Результат работы deleteCommentsByIPAddress
 */
public class DeleteCommentResult {
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
