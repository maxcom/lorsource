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

package ru.org.linux.search

import com.fasterxml.jackson.annotation.JsonProperty

case class MessageIndexDocument(
  @JsonProperty("section") section: String,
  @JsonProperty("topic_author") topicAuthor: String,
  @JsonProperty("topic_id") topicId: Int,
  @JsonProperty("author") author: String,
  @JsonProperty("group") group: String,
  @JsonProperty("title") title: Option[String],
  @JsonProperty("topic_title") topicTitle: String,
  @JsonProperty("message") message: String,
  @JsonProperty("postdate") postdate: String,
  @JsonProperty("tag") tags: Seq[String],
  @JsonProperty("is_comment") isComment: Boolean,
  @JsonProperty("topic_awaits_commit") topicAwaitsCommit: Boolean
)
