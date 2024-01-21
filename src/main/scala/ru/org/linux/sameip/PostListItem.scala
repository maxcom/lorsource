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

package ru.org.linux.sameip

import ru.org.linux.user.User

import java.sql.Timestamp
import javax.annotation.Nullable
import scala.beans.{BeanProperty, BooleanBeanProperty}

case class PostListItem(authorId: Int, commentId: Option[Int], topicId: Int, groupTitle: String, title: String,
                        deleted: Boolean, @Nullable reason: String, postdate: Timestamp)
case class PreparedPostListItem(@BeanProperty link: String, @BeanProperty groupTitle: String,
                                @BeanProperty author: User, @BeanProperty title: String,
                                @BooleanBeanProperty deleted: Boolean, @BeanProperty textPreview: String,
                                @BeanProperty @Nullable reason: String, @BeanProperty postdate: Timestamp,
                                @BooleanBeanProperty comment: Boolean)