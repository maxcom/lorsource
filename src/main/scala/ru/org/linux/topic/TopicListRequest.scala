/*
 * Copyright 1998-2026 Linux.org.ru
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

package ru.org.linux.topic

import java.util.Date

object TopicListRequest:
  enum CommitMode:
    case CommittedOnly
    case UncommittedOnly
    case PostmoderatedOnly
    case CommittedAndPostmoderated
    case All

  enum DateLimit:
    case NoLimit
    case FromDate(from: Date)
    case Between(from: Date, to: Date)

case class TopicListRequest(
    commitMode: TopicListRequest.CommitMode = TopicListRequest.CommitMode.CommittedAndPostmoderated,
    sections: Set[Int] = Set.empty,
    userId: Int = 0,
    userFavs: Boolean = false,
    userWatches: Boolean = false,
    group: Int = 0,
    tag: Int = 0,
    limit: Option[Int] = None,
    offset: Option[Int] = None,
    dateLimit: TopicListRequest.DateLimit = TopicListRequest.DateLimit.NoLimit,
    notalks: Boolean = false,
    tech: Boolean = false,
    showDraft: Boolean = false)
