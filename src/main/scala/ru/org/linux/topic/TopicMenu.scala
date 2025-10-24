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
package ru.org.linux.topic

import ru.org.linux.user.Userpic

import javax.annotation.Nullable
import scala.beans.{BeanProperty, BooleanBeanProperty}

case class TopicMenu(@BooleanBeanProperty topicEditable: Boolean, @BooleanBeanProperty tagsEditable: Boolean,
                     @BooleanBeanProperty resolvable: Boolean, @BooleanBeanProperty commentsAllowed: Boolean,
                     @BooleanBeanProperty deletable: Boolean, @BooleanBeanProperty undeletable: Boolean,
                     @BooleanBeanProperty commitable: Boolean, @BeanProperty @Nullable userpic: Userpic,
                     @BooleanBeanProperty showComments: Boolean, @BooleanBeanProperty warningsAllowed: Boolean) {
  @BooleanBeanProperty
  val editable: Boolean = tagsEditable || topicEditable
}