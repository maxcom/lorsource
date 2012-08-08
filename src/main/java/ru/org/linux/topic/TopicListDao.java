/*
 * Copyright 1998-2012 Linux.org.ru
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

import java.util.List;

public interface TopicListDao {
  public static enum CommitMode {
    COMMITED_ONLY(" AND sections.moderate AND commitdate is not null "),
    UNCOMMITED_ONLY(" AND (NOT topics.moderate) AND sections.moderate "),
    POSTMODERATED_ONLY(" AND NOT sections.moderate"),
    COMMITED_AND_POSTMODERATED(" AND (topics.moderate OR NOT sections.moderate) "),
    ALL(" ");

    final String queryPiece;

    CommitMode(String queryPiece) {
      this.queryPiece = queryPiece;
    }

    public String getQueryPiece() {
      return queryPiece;
    }
  }

  /**
   * Получение список топиков.
   *
   * @param topicListDto объект, содержащий условия выборки
   * @return список топиков
   */
  List<Topic> getTopics(TopicListDto topicListDto);

  /**
   *
   * @param sectionId
   * @return
   */
  List<TopicListDto.DeletedTopic> getDeletedTopics(Integer sectionId);
}
