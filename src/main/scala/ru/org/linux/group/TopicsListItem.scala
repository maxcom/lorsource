/*
 * Copyright 1998-2025 Linux.org.ru
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
package ru.org.linux.group

import java.sql.Timestamp

case class TopicsListItem(topicAuthor: Int, topicId: Int, commentCount: Int, groupTitle: String, title: String,
                          lastCommentId: Option[Int], lastCommentBy: Option[Int], resolved: Boolean,
                          section: Int, groupUrlName: String, postdate: Timestamp, uncommited: Boolean,
                          deleted: Boolean, sticky: Boolean, topicPostscore: Int)