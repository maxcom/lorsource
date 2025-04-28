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

package ru.org.linux.group;

import java.sql.Timestamp;
import java.util.Optional;

public record TopicsListItem(int topicAuthor, int topicId, int commentCount, String groupTitle, String title,
                             Optional<Integer> lastCommentId, Optional<Integer> lastCommentBy, boolean resolved,
                             int section, String groupUrlName, Timestamp postdate, boolean uncommited, boolean deleted,
                             boolean sticky, int topicPostscore) {}
