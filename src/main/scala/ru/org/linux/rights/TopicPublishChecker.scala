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
package ru.org.linux.rights

import org.springframework.stereotype.Service
import ru.org.linux.auth.AnySession
import ru.org.linux.group.Group
import ru.org.linux.group.GroupPermissionService.TopicLimitInfo

@Service
class TopicPublishChecker(addTopicChecker: AddTopicChecker):
  def checkPublish(group: Group, topicLimitInfo: TopicLimitInfo)(using AnySession): Permission =
    Unrestricted
      .restrict(addTopicChecker.checkTopicPostingChain(group))
      .restrict(!topicLimitInfo.exempt && topicLimitInfo.reached, "превышен лимит числа топиков в сутки")
      .seal

