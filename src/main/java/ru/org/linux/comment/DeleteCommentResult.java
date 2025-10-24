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

public class DeleteCommentResult {
  private final List<Integer> deletedTopicIds;
  private final List<Integer> deletedCommentIds;
  private final List<Integer> skippedComments;

  DeleteCommentResult(List<Integer> deletedTopicIds, List<Integer> deletedCommentIds, List<Integer> skippedComments) {
    this.deletedCommentIds = deletedCommentIds;
    this.deletedTopicIds = deletedTopicIds;
    this.skippedComments = skippedComments;
  }

  public List<Integer> getDeletedTopicIds() {
    return deletedTopicIds;
  }

  public List<Integer> getDeletedCommentIds() {
    return deletedCommentIds;
  }

  public List<Integer> getSkippedComments() {
    return skippedComments;
  }
}
